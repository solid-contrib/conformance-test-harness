package org.solid.testharness.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;
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

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TestHarnessConfig {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.config.TestHarnessConfig");

    private static final String TEST_HARNESS_URI = "https://github.com/solid/conformance-test-harness/";

    private TargetServer targetServer;
    private Map<String, TargetServer> servers;
    private Map<String, SolidClient> clients;
    private File credentialsDirectory;
    private File configFile;
    private File testSuiteDescriptionFile;

    // the settings are taken in the following order of preference:
    //   system property
    //   env variable
    //   .env file in cwd
    //   config/application.yaml (local)
    //   src/main/resources/application.yaml (project)
    // @seeAlso: https://quarkus.io/guides/config-reference#configuration_sources

    @ConfigProperty(name = "configFile")
    String configPath;
    @ConfigProperty(name = "credentialsDir")
    String credentialsPath;
    @ConfigProperty(name = "target")
    String target;
    @ConfigProperty(name = "testSuiteDescription")
    String testSuiteDescriptionPath;
    @Inject
    PathMappings pathMappings;

    @Inject
    DataRepository dataRepository;
    @Inject
    AuthManager authManager;

    @PostConstruct
    public void initialize() throws IOException {
        configFile = new File(configPath).getCanonicalFile();
        testSuiteDescriptionFile = new File(testSuiteDescriptionPath).getCanonicalFile();
        credentialsDirectory = new File(credentialsPath).getCanonicalFile();
        logSettings();

        dataRepository.loadTurtle(configFile);

        String targetIri = TEST_HARNESS_URI + target;
        dataRepository.setupNamespaces(TEST_HARNESS_URI);

        try (RepositoryConnection conn = dataRepository.getConnection()) {
            RepositoryResult<Statement> statements = conn.getStatements(null, RDF.type, EARL.TestSubject);
            servers = new HashMap<>();
            statements.forEach(s -> {
                // TODO: Change to IRI not string once this is complete
                TargetServer server = new TargetServer(dataRepository, (IRI) s.getSubject());
                servers.put(s.getSubject().stringValue(), server);
                if (s.getSubject().stringValue().equals(targetIri)) {
                    targetServer = server;
                    dataRepository.setTestSubject(targetServer.getTestSubject());
                }
            });
        }

        if (targetServer == null) {
            logger.error("No config found for server: {}", targetIri);
        } else {
            logger.debug("TestSubject {}", targetServer.getTestSubject());
            logger.debug("Max threads: {}", targetServer.getMaxThreads());
        }
    }

    public void registerClients() {
        logger.info("===================== REGISTER CLIENTS ========================");
        clients = new HashMap<>();
        targetServer.getUsers().keySet().forEach(user -> {
            try {
                clients.put(user, authManager.authenticate(user, targetServer));
            } catch (Exception e) {
                logger.error("Failed to register clients", e);
            }
        });
    }

    public TargetServer getTargetServer() {
        return targetServer;
    }

    public Map<String, TargetServer> getServers() {
        return servers;
    }

    public Map<String, SolidClient> getClients() {
        return clients;
    }

    public File getTestSuiteDescription() {
        return testSuiteDescriptionFile;
    }

    public File getCredentialsDirectory() {
        return credentialsDirectory;
    }

    public List<PathMappings.Mapping> getPathMappings() {
        return pathMappings.getMappings();
    }

    private void logSettings() throws IOException {
        logger.info("Config filename:  {}", configFile.getCanonicalPath());
        logger.info("Credentials path: {}", credentialsDirectory.getCanonicalPath());
        logger.info("Test suite:       {}", testSuiteDescriptionFile.getCanonicalPath());
        logger.info("Path mappings:    {}", pathMappings.getMappings());
        logger.info("Target server:    {}", target);
    }
}
