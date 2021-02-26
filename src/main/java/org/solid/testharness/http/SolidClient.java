package org.solid.testharness.http;

import jakarta.ws.rs.core.Link;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SolidClient {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.http.SolidClient");

    private Client client = null;
    private int aclCachePause = 0;

    public SolidClient() {
        client = ClientRegistry.getClient(ClientRegistry.DEFAULT);
    }
    public SolidClient(String user) {
        client = ClientRegistry.getClient(user);
    }
    public SolidClient(Client client, int aclCachePause) {
        this.client = client;
        this.aclCachePause = aclCachePause;
    }
    public SolidClient(Client client) {
        this.client = client;
    }

    public static SolidClient create(String user) {
        return new SolidClient(user);
    }

    public Client getClient() {
        return client;
    }

    public HttpClient getHttpClient() {
        return client.getHttpClient();
    }

    public boolean setupRootAcl(String serverRoot, String webID) throws Exception {
        URI rootAclUrl = getResourceAclLink(serverRoot + (serverRoot.endsWith("/") ? "" : "/"));
        String acl = String.format("@prefix acl: <http://www.w3.org/ns/auth/acl#>. " +
                "<#alice> a acl:Authorization ; " +
                "  acl:agent <%s> ;" +
                "  acl:accessTo </>;" +
                "  acl:default </>;" +
                "  acl:mode acl:Read, acl:Write, acl:Control .", webID);
        return createAcl(rootAclUrl, acl);
    }

    public Map<String, String> getAuthHeaders(String method, String uri) throws Exception {
        return client.getAuthHeaders(method, uri);
    }

    public final HttpHeaders createResource(URI url, String data, String type) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(url)
                .PUT(HttpRequest.BodyPublishers.ofString(data))
                .header("Content-Type", type);
        HttpRequest request = client.authorize(builder).build();
        HttpResponse<Void> response = client.send(request, BodyHandlers.discarding());
        HttpUtils.logResponse(logger, response);
        return response.headers();
    }

    public final URI getResourceAclLink(String url) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "*/*")    // TODO: This is required due to CSS bug: https://github.com/solid/community-server/issues/593
                .method("HEAD", HttpRequest.BodyPublishers.noBody());
        HttpRequest request = client.authorize(builder).build();
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

    public boolean createAcl(URI url, String acl) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(url)
                .PUT(HttpRequest.BodyPublishers.ofString(acl))
                .header("Content-Type", "text/turtle");
        HttpRequest request = client.authorize(builder).build();
        logger.debug("ACL {}", acl);
        HttpUtils.logRequest(logger, request);
        HttpResponse<Void> response = client.send(request, BodyHandlers.discarding());
        HttpUtils.logResponse(logger, response);
        if (aclCachePause > 0) {
            Thread.sleep(aclCachePause);
        }
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

    public static String deleteResourceRecursively(String url, String user) {
        return deleteResourceRecursively(URI.create(url), user);
    }

    public static String deleteResourceRecursively(URI url, String user) {
        logger.debug("Delete resource recursively {} for {}", url, user);
        SolidClient solidClient = new SolidClient(user);
        try {
            solidClient.deleteResourceRecursively(url);
            return "OK";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return "FAIL";
    }

    public void deleteResourceRecursively(String url) throws Exception, ExecutionException {
        deleteRecursive(URI.create(url), null).get();
    }

    public void deleteContentsRecursively(String url) throws Exception, ExecutionException {
        deleteRecursive(URI.create(url), new AtomicInteger(0)).get();
    }

    public void deleteResourceRecursively(URI url) throws Exception, ExecutionException {
        deleteRecursive(url, null).get();
    }

    public void deleteContentsRecursively(URI url) throws Exception, ExecutionException {
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
            } catch (Exception e) {
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
                        responses.stream().filter(response -> response.statusCode() < 200 || response.statusCode() > 299).map(response -> {
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
        HttpRequest.Builder builder = HttpRequest.newBuilder(url).DELETE();
        HttpRequest request;
        try {
            request = client.authorize(builder).build();
        } catch (Exception e) {
            logger.error("Failed to set up authorization", e);
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<HttpResponse<Void>> response = getHttpClient().sendAsync(request, BodyHandlers.discarding());
        return response;
    }

    private List<URI> getContainerMembers(URI url) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(url)
                .GET()
                .header("Accept", "text/turtle");
        HttpRequest request = client.authorize(builder).build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Error response="+response.statusCode()+" trying to get container members for "+url);
        }
        Model model = null;
        try {
            model = Rio.parse(new StringReader(response.body()), url.toString(), RDFFormat.TURTLE);
        } catch (RDFParseException e) {
            logger.error("RDF Parse Error: {} in {}", e.getMessage(), response.body());
            throw new Exception("Bad container listing");
        }
        // create list of resources to delete
        Set<Value> resources = model.filter(null, LDP.CONTAINS, null).objects();
        return resources.stream().map(resource -> URI.create(resource.toString())).collect(Collectors.toList());
    }

    private boolean isContainer(URI url) {
        return (url != null && url.getPath().endsWith("/"));
    }

    @Override
    public String toString() {
        return "SolidClient user=" + client.getUser() + ", accessToken=" + client.getAccessToken();
    }
}
