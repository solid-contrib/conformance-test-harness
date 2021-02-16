package org.solid.testharness.utils;

import jakarta.ws.rs.core.Link;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.StringReader;
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

public class HttpUtils {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.utils.HttpUtils");

    private HttpClient client = null;
    private String authHeader = null;

    public HttpUtils() {
        this(null);
    }

    public HttpUtils(String authHeader) {
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

        client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .sslContext(sslContext)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.authHeader = authHeader;
    }

    public HttpClient getClient() {
        return client;
    }

    public static HttpUtils create(String authHeader) {
        return new HttpUtils(authHeader);
    }

    public final HttpHeaders createResource(URI url, String data, String type) throws IOException, InterruptedException {
        HttpRequest.Builder builder = requestBuilder(url)
                .PUT(HttpRequest.BodyPublishers.ofString(data))
                .header("Content-Type", type);
        HttpRequest request = builder.build();
        HttpResponse<Void> response = client.send(request, BodyHandlers.discarding());
        logResponse(response);
        return response.headers();
    }

    public final URI getResourceAclLink(String url) throws IOException, InterruptedException {
        HttpRequest.Builder builder = requestBuilder(URI.create(url))
                .header("Accept", "*/*")    // TODO: This is required due to CSS bug: https://github.com/solid/community-server/issues/593
                .method("HEAD", HttpRequest.BodyPublishers.noBody());
        HttpRequest request = builder.build();
        logRequest(request);
        HttpResponse<Void> response = client.send(request, BodyHandlers.discarding());
        logResponse(response);
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
        logResponse(response);
        return isSuccessful(response.statusCode());
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
        HttpUtils utils = new HttpUtils(authHeader);
        try {
            utils.deleteResourceRecursively(url);
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

    private boolean isSuccessful(int code) {
        return code >= 200 && code < 300;
    }

    private void logRequest(HttpRequest request) {
        logger.debug("REQUEST {} {}", request.method(), request.uri());
        HttpHeaders headers = request.headers();
        headers.map().forEach((k, v) -> logger.debug("REQ HEADER {}: {}", k, v));
    }

    private <T> void logResponse(HttpResponse<T> response) {
        logger.debug("REQUEST {} {}", response.request().method(), response.uri());
        logger.debug("STATUS  {}", response.statusCode());
        HttpHeaders headers = response.headers();
        headers.map().forEach((k, v) -> logger.debug("HEADER  {}: {}", k, v));
        T body = response.body();
        if (body != null) {
            logger.debug("BODY    {}", response.body());
        }
    }

    // for testing only
    public String getAuthHeader() {
        return authHeader;
    }

    // for testing only
    public String throwException() throws Exception {
        throw new Exception("Test exception");
    }
}
