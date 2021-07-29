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

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.EARL;
import org.solid.common.vocab.RDF;
import org.solid.testharness.http.AuthManager;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.http.SolidClient;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.SolidContainer;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class TestSubject {
    private static final Logger logger = LoggerFactory.getLogger(TestSubject.class);

    private TargetServer targetServer;
    private Map<String, SolidClient> clients;
    private SolidContainer rootTestContainer;

    @Inject
    Config config;
    @Inject
    DataRepository dataRepository;
    @Inject
    AuthManager authManager;

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
            logger.debug("TestSubject {}", targetServer.getSubject());
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
                    if (value.isBNode()) {
                        conn.add(model.filter((BNode) value, null, null));
                    }
                }
            }
        }
    }

    public void prepareServer() {
        if (targetServer == null) {
            throw new TestHarnessInitializationException("No target server has been configured");
        }
        final SolidClient solidClient = new SolidClient(HttpConstants.ALICE);
        if (config.isSetupRootAcl()) {
            logger.debug("Setup root acl");
            try {
                final URI rootAclUrl = solidClient.getAclUri(config.getServerRoot());
                if (rootAclUrl == null) {
                    throw new TestHarnessInitializationException("Failed getting the root ACL link");
                }
                final String acl = String.format("@prefix acl: <http://www.w3.org/ns/auth/acl#>. " +
                        "<#alice> a acl:Authorization ; " +
                        "  acl:agent <%s> ;" +
                        "  acl:accessTo <./>;" +
                        "  acl:default <./>;" +
                        "  acl:mode acl:Read, acl:Write, acl:Control .", config.getWebIds().get(HttpConstants.ALICE));
                if (!solidClient.createAcl(rootAclUrl, acl)) {
                    throw new TestHarnessInitializationException("Failed to create root ACL");
                }
            } catch (IOException | InterruptedException e) {
                throw (TestHarnessInitializationException) new TestHarnessInitializationException(
                        "Failed to create root ACL: %s", e.toString()
                ).initCause(e);
            }
        }
        logger.debug("\n========== CHECK TEST SUBJECT ROOT");
        try {
            final SolidContainer rootContainer = SolidContainer.create(solidClient, config.getTestContainer());
            logger.debug("Root container content: {}", rootContainer.getContentAsTurtle());
            logger.debug("Root container access controls: {}", rootContainer.getAccessDataset());

            // create a root container for all the test cases
            rootTestContainer = SolidContainer.create(solidClient, config.getTestContainer())
                    .generateChildContainer().instantiate();
            logger.debug("Test container content: {}", rootTestContainer.getContentAsTurtle());
            logger.debug("Test container access controls: {}", rootTestContainer.getAccessDataset());
        } catch (Exception e) {
            throw (TestHarnessInitializationException) new TestHarnessInitializationException(
                    "Failed to prepare server: %s", e.toString()
            ).initCause(e);
        }
    }

    public void registerUsers() {
        try {
            authManager.registerUser(HttpConstants.ALICE);
            authManager.registerUser(HttpConstants.BOB);
        } catch (Exception e) {
            throw (TestHarnessInitializationException) new TestHarnessInitializationException(
                    "Failed to register users: %s", e.toString()
            ).initCause(e);
        }
    }

    public void registerClients() {
        clients = new HashMap<>();
        if (targetServer == null) {
            throw new TestHarnessInitializationException("No target server has been configured");
        }
        config.getWebIds().keySet().forEach(user -> {
            try {
                clients.put(user, authManager.authenticate(user, targetServer));
            } catch (Exception e) {
                throw (TestHarnessInitializationException) new TestHarnessInitializationException(
                        "Failed to register clients: %s", e.toString()
                ).initCause(e);
            }
        });
    }

    public void tearDownServer() {
        try {
            rootTestContainer.delete();
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

    public Map<String, SolidClient> getClients() {
        return clients;
    }

    public SolidContainer getRootTestContainer() {
        return rootTestContainer;
    }

    public void setRootTestContainer(final SolidContainer solidContainer) {
        rootTestContainer = solidContainer;
    }
}
