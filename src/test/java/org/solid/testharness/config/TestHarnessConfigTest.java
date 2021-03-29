package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class TestHarnessConfigTest {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.config.TestHarnessConfigTest");

    @Inject
    TestHarnessConfig testHarnessConfig;

    @Test
    void injection() {
        assertNotNull(testHarnessConfig);
        testHarnessConfig.logSettings();
    }

    @Test
    public void loadFromTurtle() {
        // TODO: Create test config file for tests
        String targetServer = "https://github.com/solid/conformance-test-harness/ess-compat";
        ClassLoader classLoader = TestHarnessConfigTest.class.getClassLoader();
        String configFile = classLoader.getResource("config-sample.ttl").getFile();
        System.setProperty("config", configFile);
//        System.setProperty("credentials", configFile);

        assertNotNull(testHarnessConfig);
        TargetServer server = testHarnessConfig.getServers().get(targetServer);
        assertEquals(targetServer, server.getTestSubject().stringValue());
        assertEquals("https://pod-compat.inrupt.com", server.getServerRoot());
    }
}
