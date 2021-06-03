/*
 * MIT License
 *
 * Copyright (c) 2021 Solid
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.solid.testharness.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class OidcConfigurationTest {
    private static final String DATA = "{\"authorization_endpoint\":\"https://example.org/authorization\"," +
            "\"issuer\":\"https://example.org\"," +
            "\"registration_endpoint\":\"https://example.org/registration\"," +
            "\"token_endpoint\":\"https://example.org/token\"," +
            "\"grant_types_supported\":[\"authorization_code\",\"refresh_token\",\"client_credentials\"]" +
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

    @Test
    void getGrantTypesSupported() {
        final List<String> grantTypes = oidcConfiguration.getGrantTypesSupported();
        assertEquals(3, grantTypes.size());
        assertTrue(grantTypes.contains(HttpConstants.AUTHORIZATION_CODE_TYPE));
        assertTrue(grantTypes.contains(HttpConstants.REFRESH_TOKEN));
        assertTrue(grantTypes.contains(HttpConstants.CLIENT_CREDENTIALS));
    }
}
