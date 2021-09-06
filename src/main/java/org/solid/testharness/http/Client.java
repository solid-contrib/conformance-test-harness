/*
 * MIT License
 *
 * Copyright (c) 2021 Solid
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.solid.testharness.http;

import org.apache.commons.text.RandomStringGenerator;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;
import static org.apache.commons.text.CharacterPredicates.DIGITS;
import static org.apache.commons.text.CharacterPredicates.LETTERS;

@SuppressWarnings({"checkstyle:FinalClass", "PMD.AvoidDuplicateLiterals"})
// Class is not final because it needs mocking for tests
// Duplicate string are null check error messages for same parameter
public class Client {
    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    private HttpClient httpClient;
    private String accessToken;
    private RsaJsonWebKey clientKey;
    private boolean dpopSupported;
    private String agent;
    private String user;

    public static class Builder {
        private final HttpClient.Builder clientBuilder;
        private final String user;
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
                    .connectTimeout(HttpUtils.getConnectTimeout());
        }

        public Builder withSessionSupport() {
            CookieHandler.setDefault(new CookieManager());
            clientBuilder.cookieHandler(CookieHandler.getDefault());
            return this;
        }

        public Builder withLocalhostSupport() {
            System.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
            clientBuilder.sslContext(LocalHostSupport.createSSLContext());
            return this;
        }

        public Builder withDpopSupport() throws JoseException {
            final String identifier = GENERATOR.generate(12);
            clientKey = RsaJwkGenerator.generateJwk(2048);
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

    public <T> HttpResponse<T> send(@NotNull final HttpRequest request,
                                    @NotNull final HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException, InterruptedException {
        requireNonNull(request, "request is required");
        requireNonNull(responseBodyHandler, "responseBodyHandler is required");
        return httpClient.send(request, responseBodyHandler);
    }

    public <T> HttpResponse<T> sendAuthorized(@NotNull final HttpRequest.Builder requestBuilder,
                                              @NotNull final HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException, InterruptedException {
        requireNonNull(requestBuilder, "requestBuilder is required");
        requireNonNull(responseBodyHandler, "responseBodyHandler is required");
        final HttpRequest request = authorize(requestBuilder).build();
        return httpClient.send(request, responseBodyHandler);
    }

    @SuppressWarnings("checkstyle:MultipleStringLiterals")
    public HttpResponse<String> getAsTurtle(@NotNull final URI url) throws IOException, InterruptedException {
        requireNonNull(url, "url is required");
        final HttpRequest.Builder builder = HttpUtils.newRequestBuilder(url)
                .header(HttpConstants.HEADER_ACCEPT, HttpConstants.MEDIA_TYPE_TEXT_TURTLE);
        final HttpRequest request = authorize(builder).build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<Void> put(@NotNull final URI url, final String data, final String type)
            throws IOException, InterruptedException {
        requireNonNull(url, "url is required");
        requireNonNull(data, "data is required");
        requireNonNull(type, "type is required");
        final HttpRequest.Builder builder = HttpUtils.newRequestBuilder(url)
                .PUT(HttpRequest.BodyPublishers.ofString(data))
                .header(HttpConstants.HEADER_CONTENT_TYPE, type);
        final HttpRequest request = authorize(builder).build();
        HttpUtils.logRequest(logger, request);
        final HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        HttpUtils.logResponse(logger, response);
        return response;
    }

    public HttpResponse<String> patch(@NotNull final URI url, final String data, final String type)
            throws IOException, InterruptedException {
        requireNonNull(url, "url is required");
        requireNonNull(data, "data is required");
        requireNonNull(type, "type is required");
        final HttpRequest.Builder builder = HttpUtils.newRequestBuilder(url)
                .method(HttpConstants.METHOD_PATCH, HttpRequest.BodyPublishers.ofString(data))
                .header(HttpConstants.HEADER_CONTENT_TYPE, type);
        final HttpRequest request = authorize(builder).build();
        HttpUtils.logRequest(logger, request);
        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        HttpUtils.logResponse(logger, response);
        return response;
    }

    public HttpResponse<Void> head(@NotNull final URI url) throws IOException, InterruptedException {
        requireNonNull(url, "url is required");
        final HttpRequest.Builder builder = HttpUtils.newRequestBuilder(url)
                .method(HttpConstants.METHOD_HEAD, HttpRequest.BodyPublishers.noBody());
        final HttpRequest request = authorize(builder).build();
        HttpUtils.logRequest(logger, request);
        final HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        HttpUtils.logResponse(logger, response);
        return response;
    }

    public CompletableFuture<HttpResponse<Void>> deleteAsync(@NotNull final URI url) {
        requireNonNull(url, "url is required");
        logger.debug("Deleting {}", url);
        final HttpRequest.Builder builder = HttpUtils.newRequestBuilder(url).DELETE();
        final HttpRequest request = authorize(builder).build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }

    /**
     * Sign a request using a DPoP token.
     * @param builder Request builder containing request to be signed
     * @return The builder with the DPoP header added
     */
    public HttpRequest.Builder signRequest(@NotNull final HttpRequest.Builder builder) {
        requireNonNull(builder, "builder is required");
        if (!dpopSupported) return builder;
        final HttpRequest provisionalRequest = builder.copy().build();
        final String dpopToken = generateDpopToken(provisionalRequest.method(), provisionalRequest.uri().toString());
        return builder.header(HttpConstants.HEADER_DPOP, dpopToken);
    }

    /**
     * Return a map of authentication headers depending on the client's authentication options. This is normally used
     * within Karate test features to add headers to a request.
     * @param method HTTP method of request
     * @param uri URI of request
     * @return Map of authentication and agent headers
     */
    public Map<String, String> getAuthHeaders(@NotNull final String method, @NotNull final String uri) {
        requireNonNull(method, "method is required");
        requireNonNull(uri, "uri is required");
        final Map<String, String> headers = new HashMap<>();
        if (accessToken == null) return headers;
        if (dpopSupported) {
            headers.put(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.PREFIX_DPOP + accessToken);
            final String dpopToken = generateDpopToken(method, uri);
            headers.put(HttpConstants.HEADER_DPOP, dpopToken);
        } else {
            headers.put(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.PREFIX_BEARER + accessToken);
        }
        headers.put(HttpConstants.USER_AGENT, agent);
        return headers;
    }

    /**
     * Sign and add authentication headers to a request.
     * @param builder Request builder containing request to be signed
     * @return The builder with the Authorization and DPoP headers added
     */
    private HttpRequest.Builder authorize(@NotNull final HttpRequest.Builder builder) {
        requireNonNull(builder, "builder is required");
        if (accessToken == null) return builder;
        if (dpopSupported) {
            builder.setHeader(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.PREFIX_DPOP + accessToken);
            return signRequest(builder);
        } else {
            return builder.setHeader(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.PREFIX_BEARER + accessToken);
        }
    }

    private String generateDpopToken(final String htm, final String htu) {
        requireNonNull(clientKey, "This instance does not have DPoP support added");
        final JwtClaims claims = new JwtClaims();
        claims.setJwtId(randomUUID().toString());
        claims.setStringClaim("htm", htm);
        claims.setStringClaim("htu", htu);
        claims.setIssuedAtToNow();
        return JwsUtils.generateDpopToken(clientKey, claims);
    }

    @Override
    public String toString() {
        return String.format("Client: user=%s, dPoP=%s, session=%s, local=%s",
                user, dpopSupported, httpClient.cookieHandler().isPresent(),
                System.getProperty("jdk.internal.httpclient.disableHostnameVerification", "false"));
    }

    private Client() { }
}
