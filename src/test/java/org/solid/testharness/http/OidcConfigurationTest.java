/*
 * MIT License
 *
 * Copyright (c) 2019 - 2022 W3C Solid Community Group
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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.TestUtils;

import jakarta.inject.Inject;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class OidcConfigurationTest {
    @Inject
    ObjectMapper objectMapper;

    private OidcConfiguration oidcConfiguration;

    @BeforeEach
    void setup() throws IOException {
        final var config = TestUtils.loadStringFromFile("src/test/resources/openid-configuration.json");
        oidcConfiguration = objectMapper.readValue(config, OidcConfiguration.class);
    }

    @Test
    void getIssuer() {
        assertEquals("https://example.org/", oidcConfiguration.getIssuer().toString());
    }

    @Test
    void setIssuerEmpty() {
        assertEquals("https://example.org/", oidcConfiguration.getIssuer().toString());
        oidcConfiguration.setIssuer("");
        assertEquals("https://example.org/", oidcConfiguration.getIssuer().toString());
    }

    @Test
    void getIssuerWithSlash() {
        oidcConfiguration.setIssuer("https://example.org/");
        assertEquals("https://example.org/", oidcConfiguration.getIssuer().toString());
    }

    @Test
    void getIssuerNull() {
        final OidcConfiguration oidcConfiguration = new OidcConfiguration();
        assertNull(oidcConfiguration.getIssuer());
    }

    @Test
    void getJwksEndpoint() {
        assertEquals("https://example.org/jwks", oidcConfiguration.getJwksEndpoint().toString());
    }

    @Test
    void getAuthorizeEndpoint() {
        assertEquals("https://example.org/authorization", oidcConfiguration.getAuthorizeEndpoint().toString());
    }

    @Test
    void getTokenEndpoint() {
        assertEquals("https://example.org/token", oidcConfiguration.getTokenEndpoint().toString());
    }

    @Test
    void getRegistrationEndpoint() {
        assertEquals("https://example.org/register", oidcConfiguration.getRegistrationEndpoint().toString());
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
