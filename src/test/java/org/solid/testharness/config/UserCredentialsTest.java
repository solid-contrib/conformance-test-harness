package org.solid.testharness.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserCredentialsTest {
    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void parseCredentials() throws Exception {
        String json = "{" +
                "\"webID\": \"ALICE_WEB_ID\", " +
                "\"refreshToken\": \"TOKEN\", " +
                "\"clientId\": \"CLIENT_ID\", " +
                "\"clientSecret\": \"CLIENT_SECRET\", " +
                "\"username\": \"USERNAME\", " +
                "\"password\": \"PASSWORD\"" +
                "}";
        UserCredentials userCredentials = objectMapper.readValue(json, UserCredentials.class);
        assertAll("userCredentials",
                () -> assertEquals("ALICE_WEB_ID", userCredentials.getWebID()),
                () -> assertEquals("TOKEN", userCredentials.getRefreshToken()),
                () -> assertEquals("CLIENT_ID", userCredentials.getClientId()),
                () -> assertEquals("CLIENT_SECRET", userCredentials.getClientSecret()),
                () -> assertEquals("USERNAME", userCredentials.getUsername()),
                () -> assertEquals("PASSWORD", userCredentials.getPassword()),
                () -> assertNull(userCredentials.getCredentials())
        );
    }

    @Test
    public void parseEmptyCredentials() throws Exception {
        String json = "{}";
        UserCredentials userCredentials = objectMapper.readValue(json, UserCredentials.class);
        assertAll("userCredentials",
                () -> assertNull(userCredentials.getWebID()),
                () -> assertNull(userCredentials.getRefreshToken()),
                () -> assertNull(userCredentials.getClientId()),
                () -> assertNull(userCredentials.getClientSecret()),
                () -> assertNull(userCredentials.getUsername()),
                () -> assertNull(userCredentials.getPassword()),
                () -> assertNull(userCredentials.getCredentials())
        );
    }

    @Test
    public void parseCredentialsWithExternalFile() throws Exception {
        String json = "{" +
                "\"webID\": \"ALICE_WEB_ID\", " +
                "\"credentials\": \"alice-credentials.json\"" +
                "}";
        String credentialsPath = ReaderHelperTest.getResourceDirectory("alice-credentials.json");
        System.setProperty("credentials", credentialsPath);

        UserCredentials userCredentials = objectMapper.readValue(json, UserCredentials.class);
        assertAll("userCredentials",
                () -> assertEquals("ALICE_WEB_ID", userCredentials.getWebID()),
                () -> assertEquals("EXTERNAL_TOKEN", userCredentials.getRefreshToken()),
                () -> assertEquals("EXTERNAL_CLIENT_ID", userCredentials.getClientId()),
                () -> assertEquals("EXTERNAL_CLIENT_SECRET", userCredentials.getClientSecret()),
                () -> assertEquals("EXTERNAL_USERNAME", userCredentials.getUsername()),
                () -> assertEquals("EXTERNAL_PASSWORD", userCredentials.getPassword()),
                () -> assertEquals("alice-credentials.json", userCredentials.getCredentials())
        );
    }
}
