package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.TestUtils;

import javax.inject.Inject;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(ConfigTestNormalProfile.class)
public class ConfigTest {
    @Inject
    Config config;

    @Test
    void getTestSubjectDefault() {
        assertEquals(iri("https://github.com/solid/conformance-test-harness/testserver"), config.getTestSubject());
    }

    @Test
    void getConfigUrlDefault() throws MalformedURLException {
        assertEquals(TestUtils.getFileUrl("src/test/resources/config-sample.ttl"), config.getConfigUrl());
    }

    @Test
    void getTestSuiteDescriptionDefault() throws MalformedURLException {
        assertEquals(TestUtils.getFileUrl("src/test/resources/testsuite-sample.ttl"), config.getTestSuiteDescription());
    }

    @Test
    void getOutputDirectory() {
        assertNull(config.getOutputDirectory());
        final File file = new File("target");
        config.setOutputDirectory(file);
        assertEquals(file, config.getOutputDirectory());
    }

    @Test
    void getSolidIdentityProvider() {
        assertEquals(URI.create("https://idp.example.org"), config.getSolidIdentityProvider());
    }

    @Test
    void getLoginEndpoint() {
        assertEquals(URI.create("https://example.org/login/password"), config.getLoginEndpoint());
    }

    @Test
    void getServerRoot() {
        assertEquals(URI.create("https://target.example.org/"), config.getServerRoot());
    }

    @Test
    void getTestContainer() {
        assertEquals("https://target.example.org/test/", config.getTestContainer());
    }

    @Test
    void getAliceWebId() {
        assertEquals("https://alice.target.example.org/profile/card#me", config.getAliceWebId());
    }

    @Test
    void getBobWebId() {
        assertEquals("https://bob.target.example.org/profile/card#me", config.getBobWebId());
    }

    @Test
    void logConfigSettings() {
        assertDoesNotThrow(() -> config.logConfigSettings());
    }
}
