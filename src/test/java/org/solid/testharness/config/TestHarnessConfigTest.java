package org.solid.testharness.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

public class TestHarnessConfigTest {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.config.TestHarnessConfigTest");
    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @Disabled
    public void structureTest() throws Exception {
        String json = "{ \"target\": \"ess\", " +
                "\"servers\": {" +
                "  \"ess\": {" +
                "    \"features\": { \"feature1\": true, \"feature2\": false }," +
                "      \"solidIdentityProvider\": \"IdP\"," +
                "      \"serverRoot\": \"http://localhost:3000\"," +
                "      \"setupRootAcl\": true," +
                "      \"aclCachePause\": 100," +
                "      \"maxThreads\": 4," +
                "      \"rootContainer\": \"/\"," +
                "      \"testContainer\": \"/test/\"," +
                "      \"users\": {" +
                "        \"alice\": {" +
                "          \"webID\": \"aliceWebID\"" +
                "        }," +
                "        \"bob\": {" +
                "          \"webID\": \"bobWebID\"," +
                "          \"username\": \"USERNAME\" " +
                "        }" +
                "      }}" +
                "}}";
        TestHarnessConfig config = objectMapper.readValue(json, TestHarnessConfig.class);

        assertAll("targetServer",
                () -> assertNotNull(config.getTarget())
        );
    }

    @Test
    public void loadFromTurtle() {
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
