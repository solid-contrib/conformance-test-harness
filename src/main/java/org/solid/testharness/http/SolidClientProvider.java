/*
 * MIT License
 *
 * Copyright (c) 2019 - 2022 W3C Solid Community Group
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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.ACP;
import org.solid.common.vocab.PIM;
import org.solid.testharness.accesscontrol.AccessControlFactory;
import org.solid.testharness.accesscontrol.AccessDataset;
import org.solid.testharness.accesscontrol.AccessDatasetBuilder;
import org.solid.testharness.config.TestSubject;
import org.solid.testharness.utils.TestHarnessException;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.Link;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.eclipse.rdf4j.model.util.Values.iri;

public class SolidClientProvider {
    private static final Logger logger = LoggerFactory.getLogger(SolidClientProvider.class);

    private final Client client;
    private final AccessControlFactory accessControlFactory;

    public SolidClientProvider() {
        final ClientRegistry clientRegistry = CDI.current().select(ClientRegistry.class).get();
        client = clientRegistry.getClient(ClientRegistry.DEFAULT);
        accessControlFactory = CDI.current().select(AccessControlFactory.class).get();
    }
    public SolidClientProvider(final String user) throws TestHarnessException {
        final ClientRegistry clientRegistry = CDI.current().select(ClientRegistry.class).get();
        client = clientRegistry.getClient(user);
        if (client == null) {
            throw new TestHarnessException("Client has not been registered yet: " + user);
        }
        accessControlFactory = CDI.current().select(AccessControlFactory.class).get();
    }
    public SolidClientProvider(final Client client) {
        this.client = client;
        accessControlFactory = CDI.current().select(AccessControlFactory.class).get();
    }

    public static SolidClientProvider create(final String user) throws TestHarnessException {
        return new SolidClientProvider(user);
    }

    public Client getClient() {
        return client;
    }

    public HttpHeaders createResource(final URI url, final String data, final String type) throws TestHarnessException {
        final HttpResponse<Void> response = client.put(url, data, type);
        if (!HttpUtils.isSuccessful(response.statusCode())) {
            throw new TestHarnessException("Failed to create " + url.toString() +
                    ", response=" + response.statusCode());
        }
        return response.headers();
    }

    public HttpHeaders createContainer(final URI url) throws TestHarnessException {
        final HttpRequest.Builder builder = HttpUtils.newRequestBuilder(url)
                .header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.MEDIA_TYPE_TEXT_TURTLE)
                .header(HttpConstants.HEADER_LINK, HttpConstants.CONTAINER_LINK)
                .PUT(HttpRequest.BodyPublishers.noBody());
        final HttpResponse<String> response = client.sendAuthorized(null, builder, BodyHandlers.ofString());
        if (!HttpUtils.isSuccessful(response.statusCode())) {
            throw new TestHarnessException("Failed to create " + url.toString() +
                    ", response=" + response.statusCode());
        }
        return response.headers();
    }

    public URI getAclUri(final URI uri) {
        final HttpResponse<Void> response = client.head(uri);
        return getAclUri(response.headers());
    }

    public URI getAclUri(final HttpHeaders headers) {
        final List<Link> links = HttpUtils.parseLinkHeaders(headers);
        final Optional<Link> aclLink = links.stream()
                .filter(link -> link.getRels().contains("acl") ||
                        link.getRels().contains(ACP.accessControl.toString()))
                .findFirst();
        return aclLink.map(Link::getUri).orElse(null);
    }

    public TestSubject.AccessControlMode getAclType(final URI aclUri) {
        final URI acpLink = getLinkByType(aclUri, ACP.AccessControlResource);
        return acpLink != null ? TestSubject.AccessControlMode.ACP : TestSubject.AccessControlMode.WAC;
    }

    public void createAcl(final URI url, final AccessDataset accessDataset) {
        logger.debug("ACL: {} for {}", accessDataset, url);
        accessDataset.apply(client, url);
    }

    public AccessDataset getAcl(final URI url) {
        final HttpResponse<String> response = client.getAsTurtle(url);
        if (!HttpUtils.isSuccessful(response.statusCode())) {
            return null;
        }
        return accessControlFactory.createAccessDataset(response.body(), url);
    }

    public <T extends AccessDataset> AccessDatasetBuilder<T> getAccessDatasetBuilder(final URI aclUrl) {
        return accessControlFactory.getAccessDatasetBuilder(aclUrl.toString());
    }

    public boolean hasStorageType(final URI uri) {
        return getLinkByType(uri, PIM.Storage) != null;
    }

    private URI getLinkByType(final URI uri, final IRI type)  {
        final HttpResponse<Void> response = client.head(uri);
        return HttpUtils.getHeaderLinkByType(response.headers(), type.toString());
    }

    public String getContentAsTurtle(final URI url) throws TestHarnessException {
        final HttpResponse<String> response = client.getAsTurtle(url);
        if (!HttpUtils.isSuccessful(response.statusCode())) {
            throw new TestHarnessException("Error response=" + response.statusCode() +
                    " trying to get content for " + url);
        }
        return response.body();
    }

    public Model getContentAsModel(final URI url) throws TestHarnessException, IOException {
        return Rio.parse(new StringReader(getContentAsTurtle(url)), url.toString(), RDFFormat.TURTLE);
    }

    public void deleteResourceRecursively(final URI url) {
        deleteRecursive(url, null).join();
    }

    public void deleteContentsRecursively(final URI url) {
        deleteRecursive(url, new AtomicInteger(0)).join();
    }

    private CompletableFuture<HttpResponse<Void>> deleteRecursive(final URI url, final AtomicInteger depth) {
        if (url == null) {
            throw new IllegalArgumentException("url is required for deleteRecursive");
        }
        if (isContainer(url)) {
            if (depth != null) {
                depth.incrementAndGet();
            }
            // get all members
            final List<URI> members;
            try {
                members = parseContainerContents(getContentAsTurtle(url), url);
            } catch (Exception e) {
                logger.error("Failed to get container members: {}", e.toString());
                // server may have overwritten a container as a resource so attempt to delete it in the resource form
                return client.deleteAsync(URI.create(HttpUtils.ensureNoSlashEnd(url.toString())));
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
            final List<URI> failed = allCompletableFuture.thenApply(responses ->
                    responses.stream()
                            .filter(Objects::nonNull)
                            .filter(response -> !HttpUtils.isSuccessful(response.statusCode()))
                            .map(response -> {
                                logger.debug("BAD RESPONSE {} {} {}", response.statusCode(),
                                        response.uri(), response.body()
                                );
                                return response.uri();
                            })
                            .collect(Collectors.toList())
            ).exceptionally(ex -> {
                // We don't know which one failed
                logger.error("Failed to delete resources", ex);
                return null;
            }).join();
            if (failed != null && !failed.isEmpty()) {
                logger.debug("FAILED {}", failed);
            }
            if (depth != null) {
                depth.decrementAndGet();
            }
        }
        return deleteResource(url, depth);
    }

    private CompletableFuture<HttpResponse<Void>> deleteResource(final URI url, final AtomicInteger depth) {
        // delete the resource unless depth counting to avoid this
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

    private static List<URI> parseContainerContents(final String data, final URI url) throws TestHarnessException {
        final Model model;
        try {
            model = Rio.parse(new StringReader(data), url.toString(), RDFFormat.TURTLE);
        } catch (IOException | RuntimeException e) {
            throw new TestHarnessException("Bad container listing in " + data, e);
        }
        return model.filter(iri(url.toString()), LDP.CONTAINS, null).objects().stream()
                .map(Object::toString)
                .map(URI::create)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "SolidClientProvider: user=" + client.getUser() + ", accessToken=" + client.getAccessToken();
    }
}
