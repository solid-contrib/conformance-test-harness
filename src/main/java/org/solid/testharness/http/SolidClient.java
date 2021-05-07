package org.solid.testharness.http;

import jakarta.ws.rs.core.Link;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SolidClient {
    private static final Logger logger = LoggerFactory.getLogger(SolidClient.class);

    private Client client ;

    public SolidClient() {
        client = ClientRegistry.getClient(ClientRegistry.DEFAULT);
    }
    public SolidClient(String user) {
        client = ClientRegistry.getClient(user);
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

    public boolean setupRootAcl(String serverRoot, String webID) throws IOException, InterruptedException {
        URI rootAclUrl = getResourceAclLink(serverRoot + (serverRoot.endsWith("/") ? "" : "/"));
        String acl = String.format("@prefix acl: <http://www.w3.org/ns/auth/acl#>. " +
                "<#alice> a acl:Authorization ; " +
                "  acl:agent <%s> ;" +
                "  acl:accessTo </>;" +
                "  acl:default </>;" +
                "  acl:mode acl:Read, acl:Write, acl:Control .", webID);
        return createAcl(rootAclUrl, acl);
    }

    public Map<String, String> getAuthHeaders(String method, String uri) {
        return client.getAuthHeaders(method, uri);
    }

    public HttpHeaders createResource(URI url, String data, String type) throws Exception {
        HttpResponse<Void> response = client.put(url, data, type);
        if (!HttpUtils.isSuccessful(response.statusCode())) {
            throw new Exception("Failed to create resource - status=" + response.statusCode());
        }
        return response.headers();
    }

    public URI getResourceAclLink(String url) throws IOException, InterruptedException {
        HttpResponse<Void> response = client.head(URI.create(url));
        return getAclLink(response.headers());
    }

    public URI getAclLink(HttpHeaders headers) {
        List<Link> links = HttpUtils.parseLinkHeaders(headers);
        Optional<Link> aclLink = links.stream().filter(link -> link.getRels().contains("acl")).findFirst();
        return aclLink.map(Link::getUri).orElse(null);
    }

    public boolean createAcl(URI url, String acl) throws IOException, InterruptedException {
        logger.debug("ACL: {}", acl);
        HttpResponse<Void> response = client.put(url, acl, "text/turtle");
        return HttpUtils.isSuccessful(response.statusCode());
    }

    public String getContainmentData(URI url) throws Exception {
        HttpResponse<String> response = client.getAsString(url);
        if (!HttpUtils.isSuccessful(response.statusCode())) {
            throw new Exception("Error response="+response.statusCode()+" trying to get container members for "+url);
        }
        return response.body();
    }

    public List<String> parseMembers(String data, URI url) throws Exception {
        Model model;
        try {
            model = Rio.parse(new StringReader(data), url.toString(), RDFFormat.TURTLE);
        } catch (Exception e) {
            logger.error("RDF Parse Error: {} in {}", e, data);
            throw new Exception("Bad container listing");
        }
        Set<Value> resources = model.filter(null, LDP.CONTAINS, null).objects();
        return resources.stream().map(Object::toString).collect(Collectors.toList());
    }

    public void deleteResourceRecursively(URI url) throws Exception {
        deleteRecursive(url, null).get();
    }

    public void deleteContentsRecursively(URI url) throws Exception {
        deleteRecursive(url, new AtomicInteger(0)).get();
    }

    private CompletableFuture<HttpResponse<Void>> deleteRecursive(URI url, AtomicInteger depth) {
        List<URI> failed;
        if (url == null) {
            throw new IllegalArgumentException("url is required");
        }
        if (isContainer(url)) {
            if (depth != null) {
                depth.incrementAndGet();
            }
            // get all members
            List<URI> members;
            try {
                String data = getContainmentData(url);
                members = parseMembers(data, url).stream().map(URI::create).collect(Collectors.toList());
            } catch (Exception e) {
                logger.error("Failed to get container members: {}", e.toString());
                return CompletableFuture.completedFuture(null);
            }

            // delete members via this method
            logger.debug("DELETING MEMBERS {}", members);
            List<CompletableFuture<HttpResponse<Void>>> completableFutures = members.stream().map(u -> deleteRecursive(u, depth)).collect(Collectors.toList());
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(completableFutures.toArray(CompletableFuture[]::new));
            CompletableFuture<List<HttpResponse<Void>>> allCompletableFuture = allFutures.thenApply(future -> completableFutures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList()));
            try {
                failed = allCompletableFuture.thenApply(responses ->
                        responses.stream().filter(response -> !HttpUtils.isSuccessful(response.statusCode())).map(response -> {
                            logger.debug("BAD RESPONSE {} {} {}", response.statusCode(), response.uri(), response.body());
                            return response.uri();
                        }).collect(Collectors.toList())
                ).exceptionally(ex -> {
                    // TODO: We don't know which one failed
                    logger.error("Failed to delete resources", ex);
                    return null;
                }).get();
                if (failed != null && !failed.isEmpty()) {
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
            return client.deleteAsync(url);
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    private boolean isContainer(@NotNull URI url) {
        return url.getPath().endsWith("/");
    }

    @Override
    public String toString() {
        return "SolidClient: user=" + client.getUser() + ", accessToken=" + client.getAccessToken();
    }
}
