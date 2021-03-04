package org.solid.testharness.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TargetHarnessConfigTest {
    ObjectMapper objectMapper = new ObjectMapper();

    @Test
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
                "          \"username\": \"USERNAME\", " +
                "        }" +
                "      }}" +
                "}}";
        TestHarnessConfig config = objectMapper.readValue(json, TestHarnessConfig.class);

        assertAll("targetServer",
                () -> assertNotNull(config.getTarget())
        );
    }
}
