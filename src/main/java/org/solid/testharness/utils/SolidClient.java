package org.solid.testharness.utils;

import jakarta.ws.rs.core.Link;
import org.apache.commons.text.RandomStringGenerator;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
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
import java.io.StringReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.UUID.randomUUID;
import static org.apache.commons.text.CharacterPredicates.DIGITS;
import static org.apache.commons.text.CharacterPredicates.LETTERS;
import static org.jose4j.jwx.HeaderParameterNames.TYPE;

public class SolidClient {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.utils.SolidClient");

    private HttpClient client = null;
    private String authHeader = null;
    private RsaJsonWebKey clientKey = null;

    public static class Builder {
        private HttpClient.Builder clientBuilder = null;
        private String authHeader = null;
        private RsaJsonWebKey clientKey = null;

        private static final RandomStringGenerator GENERATOR = new RandomStringGenerator.Builder()
                .withinRange('0', 'z').filteredBy(LETTERS, DIGITS).build();

        public Builder() {
            clientBuilder = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(5));
        }

        public Builder withSessionSupport(){
            CookieHandler.setDefault(new CookieManager());
            clientBuilder.cookieHandler(CookieHandler.getDefault());
            return this;
        }

        public Builder withLocalhostSupport(){
            // Allow self-signed certificates for testing
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };
            SSLContext sslContext = null;
            try {
                sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                e.printStackTrace();
            }
            System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "");

            clientBuilder.sslContext(sslContext);
            return this;
        }

        public Builder withAuthorizationHeader(String authHeader){
            this.authHeader = authHeader;
            return this;
        }

        public Builder withDpopSupport() throws Exception {
            String identifier = GENERATOR.generate(12);
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

        public SolidClient build() {
            SolidClient solidClient = new SolidClient();
            solidClient.client = clientBuilder.build();
            solidClient.authHeader = authHeader;
            solidClient.clientKey = clientKey;
            return solidClient;
        }
    }

    private SolidClient() {}

    public HttpClient getHttpClient() {
        return client;
    }

    public static SolidClient create(String authHeader) {
        return new SolidClient.Builder().withAuthorizationHeader(authHeader).build();
    }

    public final HttpRequest.Builder signRequest(HttpRequest.Builder builder) throws Exception {
        HttpRequest provisionalRequest = builder.copy().build();
        String dpopToken = generateDpopToken(provisionalRequest.method(), provisionalRequest.uri().toString());
        return builder.header("DPoP", dpopToken);
    }

    public final HttpHeaders createResource(URI url, String data, String type) throws IOException, InterruptedException {
        HttpRequest.Builder builder = requestBuilder(url)
                .PUT(HttpRequest.BodyPublishers.ofString(data))
                .header("Content-Type", type);
        HttpRequest request = builder.build();
        HttpResponse<Void> response = client.send(request, BodyHandlers.discarding());
        HttpUtils.logResponse(logger, response);
        return response.headers();
    }

    public final URI getResourceAclLink(String url) throws IOException, InterruptedException {
        HttpRequest.Builder builder = requestBuilder(URI.create(url))
                .header("Accept", "*/*")    // TODO: This is required due to CSS bug: https://github.com/solid/community-server/issues/593
                .method("HEAD", HttpRequest.BodyPublishers.noBody());
        HttpRequest request = builder.build();
        HttpUtils.logRequest(logger, request);
        HttpResponse<Void> response = client.send(request, BodyHandlers.discarding());
        HttpUtils.logResponse(logger, response);
        return getAclLink(response.headers());
    }

    public URI getAclLink(HttpHeaders headers) {
        List<String> optLinks = headers.allValues("Link");
        if (optLinks.size() == 1 && optLinks.get(0).split(", ").length > 1) {
            // TODO: Investigate why NSS link header is treated as single link when there are more than one - this is the workaround
            optLinks = Arrays.asList(optLinks.get(0).split(", "));
        }
        Optional<Link> aclLink = optLinks.stream().map(link -> Link.valueOf(link)).filter(link -> link.getRels().contains("acl")).findFirst();
        return aclLink.isPresent() ? aclLink.get().getUri() : null;
    }

    public boolean createAcl(URI url, String acl) throws IOException, InterruptedException {
        HttpRequest.Builder builder = requestBuilder(url)
                .PUT(HttpRequest.BodyPublishers.ofString(acl))
                .header("Content-Type", "text/turtle");
        HttpRequest request = builder.build();
        HttpResponse<Void> response = client.send(request, BodyHandlers.discarding());
        HttpUtils.logResponse(logger, response);
        return HttpUtils.isSuccessful(response.statusCode());
    }

    public static Map<String, List<String>> parseWacAllowHeader(Map<String, List<String>> headers) {
        logger.debug("WAC-Allow: {}", headers.toString());
        Map<String, Set<String>> permissions = Map.of(
                "user", new HashSet<String>(),
                "public", new HashSet<String>()
        );
        Optional<Map.Entry<String, List<String>>> header = headers.entrySet()
                .stream()
                .filter(entry -> entry.getKey().toLowerCase().equals("wac-allow"))
                .findFirst();
        if (header.isPresent()) {
            String wacAllowHeader = header.get().getValue().get(0);
            // note this does not support imbalanced quotes
            Pattern p = Pattern.compile("(\\w+)\\s*=\\s*\"?\\s*((?:\\s*[^\",\\s]+)*)\\s*\"?");
            Matcher m = p.matcher(wacAllowHeader);
            while (m.find()) {
                if (!permissions.containsKey(m.group(1))) {
                    permissions.put(m.group(1), new HashSet<String>());
                }
                if (!m.group(2).isEmpty()) {
                    permissions.get(m.group(1)).addAll(Arrays.asList(m.group(2).toLowerCase().split("\\s+")));
                }
            }
        } else {
            logger.error("WAC-Allow header missing");
        }
        return permissions.entrySet().stream().collect(Collectors.toMap(
                entry -> entry.getKey(),
                entry -> List.copyOf(entry.getValue())));
    }

    public static String deleteResourceRecursively(String url, String authHeader) {
        return deleteResourceRecursively(URI.create(url), authHeader);
    }

    public static String deleteResourceRecursively(URI url, String authHeader) {
        logger.debug("Delete resource recursively {}", url);
        SolidClient solidClient = new SolidClient.Builder().withAuthorizationHeader(authHeader).build();
        try {
            solidClient.deleteResourceRecursively(url);
            return "OK";
        } catch (IOException | InterruptedException | ExecutionException e) {
            logger.error(e.getMessage(), e);
        }
        return "FAIL";
    }

    public void deleteResourceRecursively(String url) throws IOException, InterruptedException, ExecutionException {
        deleteRecursive(URI.create(url), null).get();
    }

    public void deleteContentsRecursively(String url) throws IOException, InterruptedException, ExecutionException {
        deleteRecursive(URI.create(url), new AtomicInteger(0)).get();
    }

    public void deleteResourceRecursively(URI url) throws IOException, InterruptedException, ExecutionException {
        deleteRecursive(url, null).get();
    }

    public void deleteContentsRecursively(URI url) throws IOException, InterruptedException, ExecutionException {
        deleteRecursive(url, new AtomicInteger(0)).get();
    }

    private CompletableFuture<HttpResponse<Void>> deleteRecursive(URI url, AtomicInteger depth) {
        List<URI> failed = null;
        if (isContainer(url)) {
            if (depth != null) {
                depth.incrementAndGet();
            }
            // get all members
            List<URI> members = null;
            try {
                members = getContainerMembers(url).stream().filter(m -> !m.getPath().endsWith("test/")).collect(Collectors.toList());
            } catch (IOException | InterruptedException e) {
                logger.error("Failed to get container members", e);
                return CompletableFuture.completedFuture(null);
            }

            // delete members via this method
            logger.debug("DELETING MEMBERS {}", members.toString());
            List<CompletableFuture<HttpResponse<Void>>> completableFutures = members.stream().map(u -> deleteRecursive(u, depth)).collect(Collectors.toList());
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new));
            CompletableFuture<List<HttpResponse<Void>>> allCompletableFuture = allFutures.thenApply(future -> {
                return completableFutures.stream()
                        .map(completableFuture -> completableFuture.join())
                        .collect(Collectors.toList());
            });
            try {
                failed = allCompletableFuture.thenApply(responses ->
                        responses.stream().filter(response -> response.statusCode() != 204 && response.statusCode() != 205).map(response -> {
                            logger.debug("BAD RESPONSE {} {} {}", response.statusCode(), response.uri(), response.body());
                            return response.uri();
                        }).collect(Collectors.toList())
                ).exceptionally(ex -> {
                    // TODO: We don't know which one failed
                    logger.error("Failed to delete resources", ex);
                    return null;
                }).get();
                if (!failed.isEmpty()) {
                    logger.debug("FAILED {}", failed);
                }
            } catch (ExecutionException | InterruptedException e) {
                logger.error("Failed to execute requests", e);
            }
            if (depth != null) {
                depth.decrementAndGet();
            }
        }
        // delete the container unless depth counting to avoid this
        if (depth == null || depth.get() > 0) {
            logger.debug("DELETE RESOURCE {}", url);
            return deleteResourceAsync(url);
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    private CompletableFuture<HttpResponse<Void>> deleteResourceAsync(URI url) {
        logger.debug("Deleting Resource {}", url);
        HttpRequest request = requestBuilder(url).DELETE().build();
        CompletableFuture<HttpResponse<Void>> response = client.sendAsync(request, BodyHandlers.discarding());
        return response;
    }

    private List<URI> getContainerMembers(URI url) throws IOException, InterruptedException {
        HttpRequest.Builder builder = requestBuilder(url)
                .GET()
                .header("Accept", "text/turtle");
        HttpRequest request = builder.build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        Model model = Rio.parse(new StringReader(response.body()), url.toString(), RDFFormat.TURTLE);
        // create list of resources to delete
        Set<Value> resources = model.filter(null, LDP.CONTAINS, null).objects();
        return resources.stream().map(resource -> URI.create(resource.toString())).collect(Collectors.toList());
    }

    private boolean isContainer(URI url) {
        return (url != null && url.getPath().endsWith("/"));
    }

    private HttpRequest.Builder requestBuilder(URI url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(url);
        if (authHeader != null && !authHeader.isEmpty()) {
            builder.setHeader("Authorization", authHeader);
        }
        return builder;
    }

    // TODO: Switch to elliptical curve as it is faster
    public String generateDpopToken(final String htm, final String htu) throws Exception {
        if (clientKey == null) {
            throw new Exception("This instance does not have DPoP support added");
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
}
