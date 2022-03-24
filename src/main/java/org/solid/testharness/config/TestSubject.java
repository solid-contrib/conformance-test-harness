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
package org.solid.testharness.config;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.EARL;
import org.solid.common.vocab.PIM;
import org.solid.common.vocab.RDF;
import org.solid.testharness.http.ClientRegistry;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.http.SolidClientProvider;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.SolidContainerProvider;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.rdf4j.model.util.Values.iri;

@ApplicationScoped
public class TestSubject {
    private static final Logger logger = LoggerFactory.getLogger(TestSubject.class);

    public enum AccessControlMode {
        ACP_LEGACY,
        ACP,
        WAC
    }

    private TargetServer targetServer;
    private URI webId;
    private SolidContainerProvider rootTestContainer;
    private SolidContainerProvider testRunContainer;
    private AccessControlMode accessControlMode;

    @Inject
    Config config;
    @Inject
    DataRepository dataRepository;

    public void loadTestSubjectConfig()  {
        final IRI configuredTestSubject = config.getTestSubject();
        try (final InputStream is = config.getSubjectsUrl().openStream()) {
            final Model model = Rio.parse(is, config.getSubjectsUrl().toString(), RDFFormat.TURTLE);
            final Set<Resource> testSubjects = model.filter(null, RDF.type, EARL.TestSubject).subjects();
            if (testSubjects.isEmpty()) {
                throw new TestHarnessInitializationException("No TestSubjects were found in the config file");
            }
            if (configuredTestSubject == null && testSubjects.size() > 1) {
                throw new TestHarnessInitializationException("No target has been specified but there are more than " +
                        "one available");
            }
            final IRI testSubject;
            if (configuredTestSubject == null) {
                testSubject = (IRI) testSubjects.iterator().next();
                config.setTestSubject(testSubject);
                loadSubjectIntoRepository(model, null);
            } else {
                testSubject = (IRI) testSubjects.stream()
                        .filter(subject -> subject.equals(configuredTestSubject))
                        .findFirst()
                        .orElseThrow(() -> new TestHarnessInitializationException("No config found for server: " +
                                configuredTestSubject.stringValue()));
                loadSubjectIntoRepository(model, testSubject);
            }
            targetServer = new TargetServer(testSubject);
            dataRepository.setTestSubject(testSubject);
            logger.info("TestSubject {}", targetServer.getSubject());
        } catch (IOException e) {
            throw new TestHarnessInitializationException("Failed to read config file " + config.getSubjectsUrl(), e);
        }
    }

    private void loadSubjectIntoRepository(@NotNull final Model model, final IRI subject) {
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            if (subject == null) {
                conn.add(model);
            } else {
                final Model subjectModel = model.filter(subject, null, null);
                conn.add(subjectModel);
                for (Value value : subjectModel.objects()) {
                    if (value.isResource()) {
                        conn.add(model.filter((Resource) value, null, null));
                    }
                }
            }
        }
    }

    /**
     * Find the storage to be used for tests
     * Confirm access to the root test container and ACL.
     * Create a sub-container for this run and confirm access to it and its ACL.
     */
    public void prepareServer() {
        if (targetServer == null) {
            throw new TestHarnessInitializationException("No target server has been configured");
        }
        final URI testContainerUri = findTestContainer();
        logger.info("Test subject test container: {}", testContainerUri);

        final SolidClientProvider ownerClient = new SolidClientProvider(HttpConstants.ALICE);
        rootTestContainer = new SolidContainerProvider(ownerClient, testContainerUri);
        determineAccessControlImplementation(ownerClient);

        logger.debug("\n========== CHECK TEST SUBJECT ROOT");
        try {
            logger.debug("Root test container content: {}", rootTestContainer.getContentAsTurtle());
            logger.debug("Root test container access controls: {}", rootTestContainer.getAccessDataset());

            // create a root container for all the test cases in this run
            testRunContainer = rootTestContainer.reserveContainer(rootTestContainer.generateId()).instantiate();
            logger.debug("Test run container content: {}", testRunContainer.getContentAsTurtle());
            logger.debug("Test run container access controls: {}", testRunContainer.getAccessDataset());
        } catch (Exception e) {
            throw new TestHarnessInitializationException("Failed to prepare server", e);
        }
    }

    // Determine the ACL mode the server has implemented
    private void determineAccessControlImplementation(final SolidClientProvider ownerClient) {
        try {
            final URI aclUrl = rootTestContainer.getAclUrl();
            if (aclUrl == null) {
                throw new Exception("Cannot get ACL url for root test container: " + rootTestContainer.getUrl());
            }
            accessControlMode = ownerClient.getAclType(aclUrl);
            if (accessControlMode == AccessControlMode.ACP && targetServer.getFeatures().contains("acp-legacy")) {
                accessControlMode = AccessControlMode.ACP_LEGACY;
            }
        } catch (Exception e) {
            throw new TestHarnessInitializationException("Failed to determine access control mode", e);
        }
    }

    URI findTestContainer() {
        final String testContainer = config.getTestContainer();
        if (StringUtils.isEmpty(testContainer)) {
            // find storage from profile
            return findStorage();
        }
        final URI uri = URI.create(testContainer);
        if (uri.isAbsolute()) {
            // testContainer was absolute
            return uri;
        } else {
            final String serverRoot = config.getServerRoot();
            if (!StringUtils.isEmpty(serverRoot)) {
                return URI.create(serverRoot).resolve(testContainer).normalize();
            } else {
                // find storage and resolve with testContainer
                return findStorage().resolve(testContainer).normalize();
            }
        }
    }

    URI findStorage() {
        final SolidClientProvider publicClient = new SolidClientProvider(ClientRegistry.ALICE_WEBID);
        final SolidClientProvider ownerClient = new SolidClientProvider(HttpConstants.ALICE);
        webId = URI.create(config.getWebIds().get(HttpConstants.ALICE));
        final Model profile;
        try {
            profile = publicClient.getContentAsModel(webId);
        } catch (Exception e) {
            throw new TestHarnessInitializationException("Failed to read WebId profile for " + webId, e);
        }
        final List<URI> pods = profile.filter(iri(webId.toString()), PIM.storage, null)
                .objects()
                .stream()
                .filter(Value::isIRI)
                .map(Value::stringValue)
                .map(URI::create)
                .filter(p -> isPodAccessible(p, ownerClient))
                .collect(Collectors.toList());
        if (pods.isEmpty()) {
            throw new TestHarnessInitializationException("Pod provisioning is not yet implemented. " +
                    "Please ensure the storage already exists for the test user.");
//            return provisionPod();
        } else {
            return pods.get(0);
        }
    }

    boolean isPodAccessible(final URI pod, final SolidClientProvider ownerClient) {
        try {
            return ownerClient.hasStorageType(pod);
        } catch (Exception e) {
            logger.warn("Failed to check pod accessibility: " + pod);
            return false;
        }
    }

    /*
    URI provisionPod() {
        try {
            final URI provisionEndpoint = URI.create("https://start.dev-next.inrupt.com");
            HttpRequest.Builder builder = HttpUtils.newRequestBuilder(provisionEndpoint)
                    .header(HttpConstants.HEADER_ACCEPT, HttpConstants.MEDIA_TYPE_APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.noBody());
            final HttpResponse<String> response = ownerClient.getClient()
                    .sendAuthorized(builder, HttpResponse.BodyHandlers.ofString());
            if (!HttpUtils.isSuccessful(response.statusCode())) {
                throw new TestHarnessInitializationException("Failed to provision pod at " + provisionEndpoint +
                        ", response=" + response.statusCode());
            }
            // returns: id, profile, storage (as a uri)
            // If you provision a new pod you should also add it to the WebID profile but this is only possible for
            // registered clients
            builder = HttpUtils.newRequestBuilder(webId)
                    .header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.MEDIA_TYPE_APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(response.body()));
            final HttpResponse<Void> response2 = ownerClient.getClient()
                    .sendAuthorized(builder, HttpResponse.BodyHandlers.discarding());
            if (!HttpUtils.isSuccessful(response2.statusCode())) {
                throw new TestHarnessInitializationException("Failed to update profile at " + webId +
                        ", response=" + response2.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            throw new TestHarnessInitializationException("Failed to provision pod for " + webId, e);
        }
    }
    */

    public void tearDownServer() {
        try {
            testRunContainer.delete();
        } catch (Exception e) {
            // log failure but continue to report results
            logger.error("Failed to delete the test containers", e);
        }
    }

    public TargetServer getTargetServer() {
        return targetServer;
    }

    public void setTargetServer(final TargetServer targetServer) {
        this.targetServer = targetServer;
    }

    public SolidContainerProvider getTestRunContainer() {
        return testRunContainer;
    }

    protected void setTestRunContainer(final SolidContainerProvider solidContainerProvider) {
        testRunContainer = solidContainerProvider;
    }

    public AccessControlMode getAccessControlMode() {
        return accessControlMode;
    }
}
