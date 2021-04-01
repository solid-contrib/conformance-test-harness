package org.solid.testharness.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
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
                () -> assertNull(userCredentials.getCredentials()),
                () -> assertTrue(userCredentials.isUsingRefreshToken()),
                () -> assertTrue(userCredentials.isUsingUsernamePassword())
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
                () -> assertNull(userCredentials.getCredentials()),
                () -> assertFalse(userCredentials.isUsingRefreshToken()),
                () -> assertFalse(userCredentials.isUsingUsernamePassword())
        );
    }

    @Test
    public void parseCredentialsWithExternalFile() throws Exception {
        String json = "{" +
                "\"webID\": \"ALICE_WEB_ID\", " +
                "\"credentials\": \"inrupt-alice.json\"" +
                "}";
        UserCredentials userCredentials = objectMapper.readValue(json, UserCredentials.class);
        assertAll("userCredentials",
                () -> assertEquals("ALICE_WEB_ID", userCredentials.getWebID()),
                () -> assertEquals("EXTERNAL_TOKEN", userCredentials.getRefreshToken()),
                () -> assertEquals("EXTERNAL_CLIENT_ID", userCredentials.getClientId()),
                () -> assertEquals("EXTERNAL_CLIENT_SECRET", userCredentials.getClientSecret()),
                () -> assertEquals("EXTERNAL_USERNAME", userCredentials.getUsername()),
                () -> assertEquals("EXTERNAL_PASSWORD", userCredentials.getPassword()),
                () -> assertEquals("inrupt-alice.json", userCredentials.getCredentials())
        );
    }

    @Test
    public void parseCredentialsWithMissingFile() {
        String json = "{" +
                "\"webID\": \"ALICE_WEB_ID\", " +
                "\"credentials\": \"inrupt-missing.json\"" +
                "}";
        assertThrows(JsonMappingException.class, () -> objectMapper.readValue(json, UserCredentials.class));
    }

    @Test
    public void usernamePasswordOptions() throws JsonProcessingException {
        String json = "{\"username\": \"USERNAME\"}";
        UserCredentials userCredentials = objectMapper.readValue(json, UserCredentials.class);
        assertFalse(userCredentials.isUsingUsernamePassword());
    }

    @Test
    public void refreshTokenOptions() throws JsonProcessingException {
        String json = "{\"refreshToken\": \"TOKEN\"}";
        UserCredentials userCredentials = objectMapper.readValue(json, UserCredentials.class);
        assertFalse(userCredentials.isUsingRefreshToken());
    }

    @Test
    public void refreshTokenOptions2() throws JsonProcessingException {
        String json = "{\"refreshToken\": \"TOKEN\", \"clientId\": \"CLIENT_ID\"}";
        UserCredentials userCredentials = objectMapper.readValue(json, UserCredentials.class);
        assertFalse(userCredentials.isUsingRefreshToken());
    }
}
