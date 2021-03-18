package org.solid.testharness.config;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestHarnessConfigTest {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.config.TestHarnessConfigTest");

    @Test
    public void loadFromTurtle() {
        // TODO: Create test config file for tests
        String targetServer = "https://github.com/solid/conformance-test-harness#ess-compat";
        ClassLoader classLoader = TestHarnessConfigTest.class.getClassLoader();
        String configFile = classLoader.getResource("config-sample.ttl").getFile();
        System.setProperty("config", configFile);
//        System.setProperty("credentials", configFile);

        TestHarnessConfig config = TestHarnessConfig.getInstance();
        assertNotNull(config);
        TargetServer server = config.getServers().get(targetServer);
        assertEquals(targetServer, server.getTestSubject().stringValue());
        assertEquals("https://pod-compat.inrupt.com", server.getServerRoot());

    }
}
