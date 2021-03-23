package org.solid.testharness.config;

import jakarta.inject.Inject;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnableAutoWeld
public class TestHarnessConfigTest {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.config.TestHarnessConfigTest");

    @Inject
    TestHarnessConfig testHarnessConfig;

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
