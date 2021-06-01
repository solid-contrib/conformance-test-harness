package org.solid.testharness.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class OidcConfigurationTest {
    private static final String DATA = "{\"authorization_endpoint\":\"https://example.org/authorization\"," +
            "\"issuer\":\"https://example.org\"," +
            "\"registration_endpoint\":\"https://example.org/registration\"," +
            "\"token_endpoint\":\"https://example.org/token\"" +
            "}";

    @Inject
    ObjectMapper objectMapper;

    private OidcConfiguration oidcConfiguration;

    @BeforeEach
    void setup() throws JsonProcessingException {
        oidcConfiguration = objectMapper.readValue(DATA, OidcConfiguration.class);
    }

    @Test
    void getIssuer() {
        assertEquals("https://example.org/", oidcConfiguration.getIssuer());
    }

    @Test
    void getIssuerWithSlash() {
        oidcConfiguration.setIssuer("https://example.org/");
        assertEquals("https://example.org/", oidcConfiguration.getIssuer());
    }

    @Test
    void getIssuerNull() {
        oidcConfiguration.setIssuer(null);
        assertEquals(null, oidcConfiguration.getIssuer());
    }

    @Test
    void getAuthorizeEndpoint() {
        assertEquals("https://example.org/authorization", oidcConfiguration.getAuthorizeEndpoint());
    }

    @Test
    void getTokenEndpoint() {
        assertEquals("https://example.org/token", oidcConfiguration.getTokenEndpoint());
    }

    @Test
    void getRegistrationEndpoint() {
        assertEquals("https://example.org/registration", oidcConfiguration.getRegistrationEndpoint());
    }
}
