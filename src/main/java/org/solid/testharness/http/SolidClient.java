package org.solid.testharness.http;

import jakarta.ws.rs.core.Link;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.enterprise.inject.spi.CDI;
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

    private Client client;

    public SolidClient() {
        final ClientRegistry clientRegistry = CDI.current().select(ClientRegistry.class).get();
        client = clientRegistry.getClient(ClientRegistry.DEFAULT);
    }
    public SolidClient(final String user) {
        final ClientRegistry clientRegistry = CDI.current().select(ClientRegistry.class).get();
        client = clientRegistry.getClient(user);
        if (client == null) {
            throw new TestHarnessInitializationException("Client '%s' has not been registered yet", user);
        }
    }
    public SolidClient(final Client client) {
        this.client = client;
    }

    public static SolidClient create(final String user) {
        return new SolidClient(user);
    }

    public Client getClient() {
        return client;
    }

    public Map<String, String> getAuthHeaders(final String method, final String uri) {
        return client.getAuthHeaders(method, uri);
    }

    public HttpHeaders createResource(final URI url, final String data, final String type) throws Exception {
        final HttpResponse<Void> response = client.put(url, data, type);
        if (!HttpUtils.isSuccessful(response.statusCode())) {
            throw new Exception("Failed to create resource - status=" + response.statusCode());
        }
        return response.headers();
    }

    public URI getResourceAclLink(final String url) throws IOException, InterruptedException {
        final HttpResponse<Void> response = client.head(URI.create(url));
        return getAclLink(response.headers());
    }

    public URI getAclLink(final HttpHeaders headers) {
        final List<Link> links = HttpUtils.parseLinkHeaders(headers);
        final Optional<Link> aclLink = links.stream().filter(link -> link.getRels().contains("acl")).findFirst();
        return aclLink.map(Link::getUri).orElse(null);
    }

    public boolean createAcl(final URI url, final String acl) throws IOException, InterruptedException {
        logger.debug("ACL: {} for {}", acl, url);
        final HttpResponse<Void> response = client.put(url, acl, HttpConstants.MEDIA_TYPE_TEXT_TURTLE);
        return HttpUtils.isSuccessful(response.statusCode());
    }

    public String getContainmentData(final URI url) throws Exception {
        final HttpResponse<String> response = client.getAsString(url);
        if (!HttpUtils.isSuccessful(response.statusCode())) {
            throw new Exception("Error response=" + response.statusCode() +
                    " trying to get container members for " + url);
        }
        return response.body();
    }

    public List<String> parseMembers(final String data, final URI url) throws Exception {
        final Model model;
        try {
            model = Rio.parse(new StringReader(data), url.toString(), RDFFormat.TURTLE);
        } catch (Exception e) {
            logger.error("RDF Parse Error: {} in {}", e, data);
            throw (Exception) new Exception("Bad container listing").initCause(e);
        }
        final Set<Value> resources = model.filter(null, LDP.CONTAINS, null).objects();
        return resources.stream().map(Object::toString).collect(Collectors.toList());
    }

    public void deleteResourceRecursively(final URI url) throws Exception {
        deleteRecursive(url, null).get();
    }

    public void deleteContentsRecursively(final URI url) throws Exception {
        deleteRecursive(url, new AtomicInteger(0)).get();
    }

    private CompletableFuture<HttpResponse<Void>> deleteRecursive(final URI url, final AtomicInteger depth) {
        final List<URI> failed;
        if (url == null) {
            throw new IllegalArgumentException("url is required");
        }
        if (isContainer(url)) {
            if (depth != null) {
                depth.incrementAndGet();
            }
            // get all members
            final List<URI> members;
            try {
                members = parseMembers(getContainmentData(url), url).stream()
                        .map(URI::create)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Failed to get container members: {}", e.toString());
                }
                return CompletableFuture.completedFuture(null);
            }

            // delete members via this method
            logger.debug("DELETING MEMBERS {}", members);
            final List<CompletableFuture<HttpResponse<Void>>> completableFutures = members.stream()
                    .map(u -> deleteRecursive(u, depth))
                    .collect(Collectors.toList());
            final CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    completableFutures.toArray(CompletableFuture[]::new)
            );
            final CompletableFuture<List<HttpResponse<Void>>> allCompletableFuture = allFutures
                    .thenApply(future -> completableFutures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList())
                    );
            try {
                failed = allCompletableFuture.thenApply(responses ->
                        responses.stream()
                                .filter(response -> !HttpUtils.isSuccessful(response.statusCode()))
                                .map(response -> {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("BAD RESPONSE {} {} {}", response.statusCode(),
                                                response.uri(), response.body()
                                        );
                                    }
                                    return response.uri();
                                })
                                .collect(Collectors.toList())
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

    private boolean isContainer(@NotNull final URI url) {
        return url.getPath().endsWith("/");
    }

    @Override
    public String toString() {
        return "SolidClient: user=" + client.getUser() + ", accessToken=" + client.getAccessToken();
    }
}
