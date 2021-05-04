package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.TestUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(ConfigTestNormalProfile.class)
public class ConfigTest {
    @Inject
    Config config;

    @Test
    void getTestSubjectDefault() {
        assertEquals("https://github.com/solid/conformance-test-harness/testserver", config.getTestSubject().stringValue());
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
    void getCredentialsDirectory() throws IOException {
        File file = config.getCredentialsDirectory();
        assertTrue(file.exists() && file.isDirectory());
        assertEquals(Path.of("src/test/resources").toFile().getCanonicalFile(), file);
    }

    @Test
    void getOutputDirectory() {
        assertNull(config.getOutputDirectory());
        File file = new File("target");
        config.setOutputDirectory(file);
        assertEquals(file, config.getOutputDirectory());
    }

    @Test
    void logConfigSettings() {
        assertDoesNotThrow(() -> config.logConfigSettings());
    }
}
