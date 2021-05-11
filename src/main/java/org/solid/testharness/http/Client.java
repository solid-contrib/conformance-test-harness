package org.solid.testharness.http;

import org.apache.commons.text.RandomStringGenerator;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;
import org.jose4j.lang.UncheckedJoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.UUID.randomUUID;
import static org.apache.commons.text.CharacterPredicates.DIGITS;
import static org.apache.commons.text.CharacterPredicates.LETTERS;
import static org.jose4j.jwx.HeaderParameterNames.TYPE;

@SuppressWarnings("checkstyle:FinalClass") // Not final because it needs mocking for tests
public class Client {
    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    private HttpClient httpClient;
    private String accessToken;
    private RsaJsonWebKey clientKey;
    private boolean dpopSupported;
    private String agent;
    private String user;

    public static class Builder {
        private HttpClient.Builder clientBuilder;
        private String user;
        private RsaJsonWebKey clientKey;

        private static final RandomStringGenerator GENERATOR = new RandomStringGenerator.Builder()
                .withinRange('0', 'z').filteredBy(LETTERS, DIGITS).build();

        public Builder() {
            this("");
        }
        public Builder(final String user) {
            this.user = user;
            clientBuilder = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(5));
        }

        public Builder withSessionSupport() {
            CookieHandler.setDefault(new CookieManager());
            clientBuilder.cookieHandler(CookieHandler.getDefault());
            return this;
        }

        public Builder withLocalhostSupport() {
            // Allow self-signed certificates for testing
            final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    @Override
                    public void checkClientTrusted(final X509Certificate[] certs, final String authType) { }
                    @Override
                    public void checkServerTrusted(final X509Certificate[] certs, final String authType) { }
                }
            };
            SSLContext sslContext = null;
            try {
                sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                logger.error("Failed to setup sslContext", e);
            }
            System.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());

            clientBuilder.sslContext(sslContext);
            return this;
        }

        public Builder withDpopSupport() throws Exception {
            final String identifier = GENERATOR.generate(12);
            try {
                clientKey = RsaJwkGenerator.generateJwk(2048);
            } catch (JoseException e) {
                throw new Exception("Failed to generate client key", e);
            }
            clientKey.setKeyId(identifier);
            clientKey.setUse("sig");
            clientKey.setAlgorithm("RS256");
            return this;
        }

        public Client build() {
            final Client client = new Client();
            client.agent = HttpUtils.getAgent();
            client.user = user;
            client.httpClient = clientBuilder.build();
            client.clientKey = clientKey;
            client.dpopSupported = clientKey != null;
            return client;
        }
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setAccessToken(final String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getUser() {
        return user;
    }

    public <T> HttpResponse<T> send(final HttpRequest request, final HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException, InterruptedException {
        return httpClient.send(request, responseBodyHandler);
    }

    public HttpResponse<String> getAsString(final URI url) throws IOException, InterruptedException {
        final HttpRequest.Builder builder = HttpUtils.newRequestBuilder(url)
                .header(HttpConstants.HEADER_ACCEPT, HttpConstants.MEDIA_TYPE_TEXT_TURTLE);
        final HttpRequest request = authorize(builder).build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<Void> put(final URI url, final String data, final String type)
            throws IOException, InterruptedException {
        final HttpRequest.Builder builder = HttpUtils.newRequestBuilder(url)
                .PUT(HttpRequest.BodyPublishers.ofString(data))
                .header(HttpConstants.HEADER_CONTENT_TYPE, type);
        final HttpRequest request = authorize(builder).build();
        HttpUtils.logRequest(logger, request);
        final HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        HttpUtils.logResponse(logger, response);
        return response;
    }

    public HttpResponse<Void> head(final URI url) throws IOException, InterruptedException {
        final HttpRequest.Builder builder = HttpUtils.newRequestBuilder(url)
                .method(HttpConstants.METHOD_HEAD, HttpRequest.BodyPublishers.noBody());
        final HttpRequest request = authorize(builder).build();
        HttpUtils.logRequest(logger, request);
        final HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        HttpUtils.logResponse(logger, response);
        return response;
    }

    public CompletableFuture<HttpResponse<Void>> deleteAsync(final URI url) {
        logger.debug("Deleting {}", url);
        final HttpRequest.Builder builder = HttpUtils.newRequestBuilder(url).DELETE();
        final HttpRequest request;
        try {
            request = authorize(builder).build();
        } catch (Exception e) {
            logger.error("Failed to set up authorization", e);
            return CompletableFuture.completedFuture(null);
        }
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }

    public HttpRequest.Builder signRequest(final HttpRequest.Builder builder) {
        if (!dpopSupported) return builder;
        final HttpRequest provisionalRequest = builder.copy().build();
        final String dpopToken = generateDpopToken(provisionalRequest.method(), provisionalRequest.uri().toString());
        return builder.header(HttpConstants.HEADER_DPOP, dpopToken);
    }

    public  HttpRequest.Builder authorize(final HttpRequest.Builder builder) {
        if (accessToken == null) return builder;
        if (dpopSupported) {
            builder.setHeader(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.PREFIX_DPOP + accessToken);
            return signRequest(builder);
        } else {
            return builder.setHeader(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.PREFIX_BEARER + accessToken);
        }
    }

    public Map<String, String> getAuthHeaders(final String method, final String uri) {
        final Map<String, String> headers = new HashMap<>();
        if (accessToken == null) return headers;
        if (dpopSupported) {
            headers.put(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.PREFIX_DPOP + accessToken);
            final String dpopToken = generateDpopToken(method, uri);
            headers.put(HttpConstants.HEADER_DPOP, dpopToken);
        } else {
            headers.put(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.PREFIX_BEARER + accessToken);
        }
        if (agent != null) {
            headers.put(HttpConstants.USER_AGENT, agent);
        }
        return headers;
    }

    // TODO: Switch to elliptical curve as it is faster
    public String generateDpopToken(final String htm, final String htu) {
        if (clientKey == null) {
            throw new RuntimeException("This instance does not have DPoP support added");
        }
        try {
            final JsonWebSignature jws = new JsonWebSignature();
//            jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);
            jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
            jws.setHeader(TYPE, "dpop+jwt");
            jws.setJwkHeader(clientKey);
            jws.setKey(clientKey.getPrivateKey());

            final JwtClaims claims = new JwtClaims();
            claims.setJwtId(randomUUID().toString());
            claims.setStringClaim("htm", htm);
            claims.setStringClaim("htu", htu);
            claims.setIssuedAtToNow();

            jws.setPayload(claims.toJson());

            return jws.getCompactSerialization();
        } catch (final JoseException ex) {
            throw new UncheckedJoseException("Unable to generate DPoP token", ex);
        }
    }

    private Client() { }
}
