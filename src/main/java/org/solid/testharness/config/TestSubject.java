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
import org.solid.testharness.http.SolidClient;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class TestSubject {
    private static final Logger logger = LoggerFactory.getLogger(TestSubject.class);

    private TargetServer targetServer;
    private Map<String, SolidClient> clients;

    @Inject
    Config config;
    @Inject
    DataRepository dataRepository;
    @Inject
    AuthManager authManager;

    public void loadTestSubjectConfig()  {
        final IRI configuredTestSubject = config.getTestSubject();
        try (final InputStream is = config.getConfigUrl().openStream()) {
            final Model model = Rio.parse(is, config.getConfigUrl().toString(), RDFFormat.TURTLE);
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
            if (logger.isDebugEnabled()) {
                logger.debug("TestSubject {}", targetServer.getSubject());
                logger.debug("Max threads: {}", targetServer.getMaxThreads());
            }
        } catch (IOException e) {
            throw (TestHarnessInitializationException) new TestHarnessInitializationException(
                    "Failed to read config file %s: %s",
                    config.getConfigUrl().toString(), e.toString()
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

    public void registerClients() {
        logger.info("===================== REGISTER CLIENTS ========================");
        clients = new HashMap<>();
        if (targetServer == null) {
            throw new TestHarnessInitializationException("No target server has been configured");
        }
        targetServer.getUsers().keySet().forEach(user -> {
            try {
                clients.put(user, authManager.authenticate(user, targetServer));
            } catch (Exception e) {
                throw (TestHarnessInitializationException) new TestHarnessInitializationException(
                        "Failed to register clients: %s", e.toString()
                ).initCause(e);
            }
        });
    }

    public TargetServer getTargetServer() {
        return targetServer;
    }

    public Map<String, SolidClient> getClients() {
        return clients;
    }
}
