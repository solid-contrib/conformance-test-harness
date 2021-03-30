package org.solid.testharness.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TargetServerTest {
    ObjectMapper objectMapper = new ObjectMapper();

    // TODO: convert to test of Turtle instead of JSON
    @Test
    @Disabled
    public void parseTargetServer() throws Exception {
        String json = "{ \"features\": { \"feature1\": true, \"feature2\": false }," +
                "      \"solidIdentityProvider\": \"IdP\"," +
                "      \"serverRoot\": \"http://localhost:3000\"," +
                "      \"setupRootAcl\": true," +
                "      \"aclCachePause\": 100," +
                "      \"maxThreads\": 4," +
                "      \"disableDPoP\": true," +
                "      \"rootContainer\": \"/\"," +
                "      \"testContainer\": \"/test/\"," +
                "      \"users\": {" +
                "        \"alice\": {" +
                "          \"webID\": \"aliceWebID\"," +
                "          \"credentials\": \"inrupt-alice.json\"" +
                "        }," +
                "        \"bob\": {" +
                "          \"webID\": \"bobWebID\"," +
                "          \"username\": \"USERNAME\", " +
                "          \"password\": \"PASSWORD\"" +
                "        }" +
                "      }}";
        TargetServer targetServer = objectMapper.readValue(json, TargetServer.class);

        assertAll("targetServer",
                () -> assertNotNull(targetServer.getFeatures()),
                () -> assertEquals(true, targetServer.getFeatures().get("feature1")),
                () -> assertEquals(false, targetServer.getFeatures().get("feature2")),
                () -> assertEquals("IdP", targetServer.getSolidIdentityProvider()),
                () -> assertEquals("http://localhost:3000", targetServer.getServerRoot()),
                () -> assertTrue(targetServer.isSetupRootAcl()),
                () -> assertEquals(100, targetServer.getAclCachePause()),
                () -> assertEquals(4, targetServer.getMaxThreads()),
                () -> assertTrue(targetServer.isDisableDPoP()),
                () -> assertEquals("/", targetServer.getRootContainer()),
                () -> assertEquals("/test/", targetServer.getTestContainer()),
                () -> assertNotNull(targetServer.getUsers()),
                () -> assertEquals(2, targetServer.getUsers().size()),
                () -> assertNotNull(targetServer.getUsers().get("alice")),
                () -> assertEquals("EXTERNAL_TOKEN", targetServer.getUsers().get("alice").getRefreshToken()),
                () -> assertNotNull(targetServer.getUsers().get("bob")),
                () -> assertEquals("USERNAME", targetServer.getUsers().get("bob").getUsername())
        );
    }
}
