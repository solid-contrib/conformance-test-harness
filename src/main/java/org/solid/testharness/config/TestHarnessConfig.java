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
 import org.solid.testharness.utils.Namespaces;

 import javax.enterprise.context.ApplicationScoped;
 import javax.inject.Inject;
 import java.io.File;
 import java.io.IOException;
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.nio.file.Paths;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;

 import static org.eclipse.rdf4j.model.util.Values.iri;

@ApplicationScoped
public class TestHarnessConfig {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.config.TestHarnessConfig");

    private IRI testSubject;
    private TargetServer targetServer;
    private Map<String, TargetServer> servers;
    private Map<String, SolidClient> clients;
    private URL configUrl;
    private URL testSuiteDescriptionFile;
    File credentialsDirectory;
    private boolean initialized = false;
    private File outputDir;

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
    String testSuiteDescription;
    @Inject
    PathMappings pathMappings;

    @Inject
    DataRepository dataRepository;
    @Inject
    AuthManager authManager;

    public void loadTestSubjectConfig() throws IOException {
        if (!initialized) {
            initialized = true;
            logger.debug("Initializing Config");
            logSettings();

            dataRepository.loadTurtle(getConfigUrl());

            try (RepositoryConnection conn = dataRepository.getConnection()) {
                RepositoryResult<Statement> statements = conn.getStatements(null, RDF.type, EARL.TestSubject);
                servers = new HashMap<>();
                statements.forEach(s -> {
                    // TODO: Change to IRI not string once this is complete
                    TargetServer server = new TargetServer((IRI) s.getSubject());
                    servers.put(s.getSubject().stringValue(), server);
                    if (s.getSubject().equals(getTestSubject())) {
                        targetServer = server;
                        dataRepository.setTestSubject(targetServer.getSubjectIri());
                    }
                });
            }

            if (targetServer == null) {
                throw new RuntimeException("No config found for server: " + getTestSubject().stringValue());
            } else {
                logger.debug("TestSubject {}", targetServer.getSubject());
                logger.debug("Max threads: {}", targetServer.getMaxThreads());
            }
        }
    }

    public void registerClients() {
        logger.info("===================== REGISTER CLIENTS ========================");
        if (targetServer == null) {
            throw new RuntimeException("No target server has been configured");
        }
        clients = new HashMap<>();
        targetServer.getUsers().keySet().forEach(user -> {
            try {
                clients.put(user, authManager.authenticate(user, targetServer));
            } catch (Exception e) {
                logger.error("Failed to register clients", e);
            }
        });
    }

    public IRI getTestSubject() {
        if (testSubject == null) {
            testSubject = iri(Namespaces.TEST_HARNESS_URI, target);
        }
        return testSubject;
    }

    public void setTestSubject(IRI testSubject) {
        this.testSubject = testSubject;
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

    public URL getConfigUrl() {
        if (configUrl == null) {
            try {
                configUrl = Paths.get(configPath).toAbsolutePath().normalize().toUri().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException("configPath config is not a valid file or URL", e);
            }
        }
        return configUrl;
    }

    public void setConfigUrl(URL configUrl) {
        this.configUrl = configUrl;
    }

    public URL getTestSuiteDescription()  {
        if (testSuiteDescriptionFile == null) {
            try {
                testSuiteDescriptionFile = Paths.get(testSuiteDescription).toAbsolutePath().normalize().toUri().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException("testSuiteDescription config is not a valid file or URL", e);
            }
        }
        return testSuiteDescriptionFile;
    }

    public void setTestSuiteDescription(URL testSuiteDescriptionFile) {
        this.testSuiteDescriptionFile = testSuiteDescriptionFile;
    }

    public File getCredentialsDirectory() {
        if (credentialsDirectory == null) {
            try {
                credentialsDirectory = new File(credentialsPath).getCanonicalFile();
            } catch (IOException e) {
                throw new RuntimeException("credentialsDir config is not a valid file", e);
            }
        }
        return credentialsDirectory;
    }

    public void setCredentialsDirectory(File credentialsDirectory) {
        this.credentialsDirectory = credentialsDirectory;
    }

    public void setOutputDirectory(File outputDir) throws IOException {
        this.outputDir = outputDir.getCanonicalFile();
    }

    public File getOutputDirectory() {
        return outputDir;
    }

    public List<PathMappings.Mapping> getPathMappings() {
        return pathMappings.getMappings();
    }

    private void logSettings() throws IOException {
        logger.info("Config url:       {}", getConfigUrl().toString());
        logger.info("Credentials path: {}", getCredentialsDirectory().getCanonicalPath());
        logger.info("Test suite:       {}", getTestSuiteDescription().toString());
        logger.info("Path mappings:    {}", pathMappings.getMappings());
        logger.info("Target server:    {}", getTestSubject().stringValue());
    }
}
