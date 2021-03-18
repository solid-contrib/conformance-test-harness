package org.solid.testharness.config;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.EARL;
import org.solid.common.vocab.RDF;
import org.solid.testharness.http.AuthManager;
import org.solid.testharness.http.SolidClient;
import org.solid.testharness.utils.DataRepository;

import java.util.HashMap;
import java.util.Map;

public class TestHarnessConfig {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.config.TestHarnessConfig");

    private static TestHarnessConfig INSTANCE;

    // TODO delete this
    private String target;
    private Map<String, TargetServer> servers;
    private TargetServer targetServer;
    private Map<String, SolidClient> clients;

    public synchronized static TestHarnessConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TestHarnessConfig();
        }
        return INSTANCE;
    }

    private TestHarnessConfig() {
        Settings settings = Settings.getInstance();
        settings.loadSystemProperties();
        settings.loadLocalProperties();
        settings.logSettings();

        DataRepository dataRepository = DataRepository.getInstance();
        dataRepository.loadTurtle(settings.getConfigFile());
        // TODO override default server with local-config or something similar

        String target = "https://github.com/solid/conformance-test-harness#" + settings.getTargetServer();


        try (RepositoryConnection conn = dataRepository.getConnection()) {
            RepositoryResult<Statement> statements = conn.getStatements(null, RDF.type, EARL.TestSubject);
            servers = new HashMap<>();
            statements.forEach(s -> {
                // TODO: Change to IRI not string once this is complete
                TargetServer server = new TargetServer(dataRepository, (IRI) s.getSubject());
                servers.put(s.getSubject().stringValue(), server);
                if (s.getSubject().stringValue().equals(target)) {
                    targetServer = server;
                }
            });
        }

        if (targetServer == null) {
            logger.error("No config found for server: {}", target);
        } else {
            logger.debug("TestSubject {}", targetServer.getTestSubject());
            logger.debug("Max threads: {}", targetServer.getMaxThreads());
            logger.info("===================== REGISTER CLIENTS ========================");
            clients = new HashMap<>();
            targetServer.getUsers().keySet().forEach(user -> {
                try {
                    clients.put(user, AuthManager.authenticate(user, targetServer));
                } catch (Exception e) {
                    logger.error("Failed to register clients", e);
                }
            });
        }
    }

    public TargetServer getTargetServer() {
        return targetServer;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Map<String, TargetServer> getServers() {
        return servers;
    }

    public void setServers(Map<String, TargetServer> servers) {
        this.servers = servers;
    }

    public Map<String, SolidClient> getClients() {
        return clients;
    }
}
