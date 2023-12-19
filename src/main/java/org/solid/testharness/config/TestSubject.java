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
package org.solid.testharness.config;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.EARL;
import org.solid.common.vocab.PIM;
import org.solid.common.vocab.RDF;
import org.solid.testharness.api.TestHarnessApiException;
import org.solid.testharness.http.ClientRegistry;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.http.HttpUtils;
import org.solid.testharness.http.SolidClientProvider;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.SolidContainerProvider;
import org.solid.testharness.utils.TestHarnessException;
import org.solid.testharness.utils.TestHarnessInitializationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

import static org.eclipse.rdf4j.model.util.Values.iri;

@ApplicationScoped
public class TestSubject {
    private static final Logger logger = LoggerFactory.getLogger(TestSubject.class);

    public enum AccessControlMode {
        ACP,
        WAC
    }

    private TargetServer targetServer;
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
                throw new TestHarnessInitializationException(MessageFormat.format(
                        "No TestSubjects were found in the config file: [{0}]", config.getTestSubject()));
            }
            if (configuredTestSubject == null && testSubjects.size() > 1) {
                throw new TestHarnessInitializationException(MessageFormat.format(
                        "No target option has been specified and there are more " +
                        "than one available in the config file: [{0}]", config.getTestSubject()));
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
                        .orElseThrow(() -> new TestHarnessInitializationException(MessageFormat.format(
                                "No config found for test subject: [{0}]", configuredTestSubject.stringValue())));
                loadSubjectIntoRepository(model, testSubject);
            }
            targetServer = new TargetServer(testSubject);
            dataRepository.setTestSubject(testSubject);
            logger.info("TestSubject {}", targetServer.getSubject());
        } catch (IOException | RDF4JException | UnsupportedRDFormatException e) {
            throw new TestHarnessInitializationException(MessageFormat.format(
                    "Failed to read test subjects config file [{0}]", config.getSubjectsUrl()), e);
        }
    }

    private void loadSubjectIntoRepository(final Model model, final IRI subject) {
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
        try {
            final URI testContainerUri = findTestContainer();
            logger.info("Test subject test container: {}", testContainerUri);

            final SolidClientProvider ownerClient = new SolidClientProvider(HttpConstants.ALICE);
            rootTestContainer = new SolidContainerProvider(ownerClient, testContainerUri);
            determineAccessControlImplementation(ownerClient);

            logger.debug("\n========== CHECK TEST SUBJECT ROOT");
            logger.debug("Root test container content: {}", rootTestContainer.getContentAsTurtle());
            logger.debug("Root test container access controls: {}", rootTestContainer.getAccessDataset());

            // create a root container for all the test cases in this run
            testRunContainer = rootTestContainer.reserveContainer(rootTestContainer.generateId()).instantiate();
            logger.debug("Test run container content: {}", testRunContainer.getContentAsTurtle());
            logger.debug("Test run container access controls: {}", testRunContainer.getAccessDataset());

            testAccessControlCapability();
        } catch (TestHarnessException | RuntimeException e) {
            throw new TestHarnessInitializationException("Failed to prepare server", e);
        }
    }

    private void testAccessControlCapability() throws TestHarnessException {
        // check that we can change the access control of a resource so we know those tests can run
        final var aclTestContainer = testRunContainer.reserveContainer("acltest");
        try {
            aclTestContainer .instantiate();
            final var builder = aclTestContainer.getAccessDatasetBuilder();
            final var bobReadAcl = builder
                    .setAgentAccess(
                    aclTestContainer.getUrl().toString(),
                    config.getWebIds().get(HttpConstants.BOB),
                    List.of("read")
            ).build();
            aclTestContainer.setAccessDataset(bobReadAcl); // MOCK THIS PASS/FAIL
            logger.info("Confirmed we can create a container [{}] and set ACLs on it", aclTestContainer.getUrl());
        } catch (TestHarnessException | TestHarnessApiException ex) {
            // don't throw an error as we might not be running access control tests
            logger.warn("Failed to create a container [{}] and set ACLs on it: {}",
                    aclTestContainer.getUrl(), ex.getMessage());
            throw new TestHarnessException("Failed to create a container [" + aclTestContainer.getUrl()
                    + "] and set ACLs on it", ex);
        }
    }

    // Determine the ACL mode the server has implemented
    private void determineAccessControlImplementation(final SolidClientProvider ownerClient)
            throws TestHarnessException {
        final URI aclUrl = rootTestContainer.getAclUrl();
        if (aclUrl == null) {
            throw new TestHarnessException("Cannot get ACL url for root test container: " +
                    rootTestContainer.getUrl());
        }
        accessControlMode = ownerClient.getAclType(aclUrl);
        logger.info("The Pod is using [{}] for access control", accessControlMode);
    }

    URI findTestContainer() throws TestHarnessException {
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

    URI findStorage() throws TestHarnessException {
        final SolidClientProvider publicClient = new SolidClientProvider(ClientRegistry.ALICE_WEBID);
        final SolidClientProvider ownerClient = new SolidClientProvider(HttpConstants.ALICE);
        final URI webId = URI.create(config.getWebIds().get(HttpConstants.ALICE));
        final Model profile;
        try {
            profile = publicClient.getContentAsModel(webId);
            logger.info("Loaded WebID Document for [{}]", webId);
        } catch (Exception e) {
            throw new TestHarnessInitializationException(MessageFormat.format(
                    "Failed to read WebID Document for [{0}]", webId), e);
        }

        final var storages = profile.filter(iri(webId.toString()), PIM.storage, null)
                .objects()
                .stream()
                .filter(Value::isIRI)
                .map(Value::stringValue)
                .map(URI::create)
                .toList();
        if (storages.isEmpty()) {
            logger.warn("No Pod references found in the WebID Document for [{}], looking for predicate [{}]",
                    webId, PIM.storage);
            throw new TestHarnessInitializationException(MessageFormat.format(
                    "No Pod references found in the WebID Document for [{0}], looking for predicate [{1}]",
                    webId, PIM.storage));
        }
        logger.info("Found [{}] Pods, checking them...", storages.size());
        final List<URI> pods = storages.stream()
                .filter(p -> isPodAccessible(p, ownerClient))
                .toList();

        if (pods.isEmpty()) {
            throw new TestHarnessInitializationException(MessageFormat.format(
                    "No accessible Pods were found for test user: [{0}]. " +
                    "Please check the logs to see why, then ensure that Pod storage exists " +
                    "and that pim:storage is defined in their WebID Document.", webId));
//            return provisionPod();
        } else {
            logger.info("Pod found: [{}]", pods.get(0));
            return pods.get(0);
        }
    }

    boolean isPodAccessible(final URI pod, final SolidClientProvider ownerClient) {
        logger.info("Checking Pod [{}]", pod);
        try {
            // is it present?
            final HttpResponse<Void> response = ownerClient.getClient().head(pod);
            if (!HttpUtils.isSuccessful(response.statusCode())) {
                logger.warn("HEAD request to Pod [{}] returned [{}]", pod, response.statusCode());
                return false;
            }
            // is it a storage?
            final var storageLink = HttpUtils.getHeaderLinkByType(response.headers(), PIM.Storage.toString());
            if (storageLink == null) {
                logger.warn("Pod [{}] is not a valid storage as it missing a [{}] header", pod, PIM.Storage);
                return false;
            }
        } catch (Exception e) {
            logger.warn("Failed to check pod [{}] accessibility due to: [{}]", pod, e.getMessage());
            return false;
        }
        return true;
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
            if (testRunContainer != null) {
                logger.info("TEAR DOWN {}", testRunContainer.getUrl());
                testRunContainer.delete();
                logger.info("TEAR DOWN COMPLETE");
            }
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
