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
import org.solid.common.vocab.RDF;
import org.solid.testharness.accesscontrol.AccessControlFactory;
import org.solid.testharness.accesscontrol.AccessDataset;
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
import java.util.UUID;

import static org.solid.testharness.config.Config.AccessControlMode.ACP;
import static org.solid.testharness.config.Config.AccessControlMode.ACP_LEGACY;

@ApplicationScoped
public class TestSubject {
    private static final Logger logger = LoggerFactory.getLogger(TestSubject.class);

    private TargetServer targetServer;
    private SolidContainerProvider testRunContainer;

    @Inject
    Config config;
    @Inject
    DataRepository dataRepository;
    @Inject
    AccessControlFactory accessControlFactory;

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
                        .orElseThrow(() -> new TestHarnessInitializationException("No config found for server: %s",
                                configuredTestSubject.stringValue()));
                loadSubjectIntoRepository(model, testSubject);
            }
            targetServer = new TargetServer(testSubject);
            dataRepository.setTestSubject(testSubject);
            logger.info("TestSubject {}", targetServer.getSubject());
        } catch (IOException e) {
            throw (TestHarnessInitializationException) new TestHarnessInitializationException(
                    "Failed to read config file %s: %s",
                    config.getSubjectsUrl().toString(), e.toString()
            ).initCause(e);
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
     * Set up the root ACL for Alice (if required).
     * Confirm access to the root test container and ACL.
     * Create a sub-container for this run and confirm access to it and its ACL.
     */
    public void prepareServer() {
        if (targetServer == null) {
            throw new TestHarnessInitializationException("No target server has been configured");
        }
        final SolidClientProvider solidClientProvider = new SolidClientProvider(HttpConstants.ALICE);

        // Determine the ACL mode the server has implemented
        final SolidContainerProvider rootTestContainer = new SolidContainerProvider(solidClientProvider,
                URI.create(config.getTestContainer()));
        try {
            final URI aclUrl = rootTestContainer.getAclUrl();
            if (aclUrl == null) {
                throw new Exception("Cannot get ACL url for root test container: " + rootTestContainer.getUrl());
            }
            Config.AccessControlMode accessControlMode = solidClientProvider.getAclType(aclUrl);
            if (accessControlMode == ACP && targetServer.getFeatures().containsKey("acp-legacy")) {
                accessControlMode = ACP_LEGACY;
            }
            config.setAccessControlMode(accessControlMode);
        } catch (Exception e) {
            throw (TestHarnessInitializationException) new TestHarnessInitializationException(
                    "Failed to determine access control mode: %s", e.toString()
            ).initCause(e);
        }

        if (config.isSetupRootAcl()) {
            logger.debug("Setup root acl");
            final boolean success;
            try {
                final SolidContainerProvider rootContainer = new SolidContainerProvider(solidClientProvider,
                        config.getServerRoot());
                final String alice = config.getCredentials(HttpConstants.ALICE).webId();
                final List<String> modes = List.of(AccessDataset.READ, AccessDataset.WRITE, AccessDataset.CONTROL);
                final AccessDataset accessDataset = accessControlFactory
                        .getAccessDatasetBuilder(rootContainer.getAclUrl().toString())
                        .setAgentAccess(rootContainer.getUrl().toString(), alice, modes)
                        .setInheritableAgentAccess(rootContainer.getUrl().toString(), alice, modes)
                        .build();
                logger.info("ACL doc: {}", accessDataset.toString());
                success = rootContainer.setAccessDataset(accessDataset);
            } catch (Exception e) {
                throw (TestHarnessInitializationException) new TestHarnessInitializationException(
                        "Failed to create root ACL: %s", e.toString()
                ).initCause(e);
            }
            if (!success) {
                throw new TestHarnessInitializationException("Failed to create root ACL");
            }
        }
        logger.debug("\n========== CHECK TEST SUBJECT ROOT");
        try {
            logger.debug("Root test container content: {}", rootTestContainer.getContentAsTurtle());
            logger.debug("Root test container access controls: {}", rootTestContainer.getAccessDataset());

            // create a root container for all the test cases in this run
            testRunContainer = rootTestContainer.reserveContainer(UUID.randomUUID().toString()).instantiate();
            if (testRunContainer == null) {
                throw new Exception("Cannot create test run container");
            }
            logger.debug("Test run container content: {}", testRunContainer.getContentAsTurtle());
            logger.debug("Test run container access controls: {}", testRunContainer.getAccessDataset());
        } catch (Exception e) {
            throw (TestHarnessInitializationException) new TestHarnessInitializationException(
                    "Failed to prepare server: %s", e.toString()
            ).initCause(e);
        }
    }

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
}
