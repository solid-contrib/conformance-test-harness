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

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.testharness.config.Config;
import org.solid.testharness.config.TestCredentials;
import org.solid.testharness.utils.TestHarnessInitializationException;

import jakarta.inject.Inject;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(AuthenticationResource.class)
class AuthManagerTest {
    public static final Map<String, String> ALICE_WEBID_MAP = Map.of(HttpConstants.ALICE,
            "https://alice.target.example.org/profile/card#me");

    private URI baseUri;

    @Inject
    AuthManager authManager;

    @InjectMock
    Config config;

    @BeforeEach
    void setup() {
        System.clearProperty("jdk.internal.httpclient.disableHostnameVerification");
        when(config.getConnectTimeout()).thenReturn(2000);
        when(config.getReadTimeout()).thenReturn(2000);
        when(config.getAgent()).thenReturn("AGENT");
    }

    @Test
    void registerUserFails() {
        setupLogin(baseUri, HttpConstants.ALICE, null, "/400/idp/register");
        assertThrows(TestHarnessInitializationException.class, () -> authManager.registerUser(HttpConstants.ALICE));
    }

    @Test
    void registerUser() {
        setupLogin(baseUri, HttpConstants.ALICE, null, "/idp/register");
        assertDoesNotThrow(() -> authManager.registerUser(HttpConstants.ALICE));
    }

    @Test
    void authenticateNoWebIdInCredentials() {
        when(config.getWebIds()).thenReturn(ALICE_WEBID_MAP);
        when(config.getSolidIdentityProvider()).thenReturn(baseUri);
        when(config.getCredentials("nowebid")).thenReturn(new TestCredentials());

        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.authenticate("nowebid"));

        assertEquals("Failed to read WebID Document for [null] " +
                        "Caused by: java.lang.NullPointerException: webId is required",
                exception.getMessage());
    }

    @Test
    void authenticateInvalidWebID() {
        when(config.getWebIds()).thenReturn(ALICE_WEBID_MAP);
        final var credentials = new TestCredentials();
        credentials.webId = "not a web id";
        when(config.getCredentials("notwebid")).thenReturn(credentials);
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.authenticate("notwebid"));
        assertEquals("Failed to read WebID Document for [not a web id] Caused by: " +
                        "java.lang.IllegalArgumentException: Illegal character in path at index 3: not a web id",
                exception.getMessage());
    }

    @Test
    void authenticateNotFoundWebID() {
        when(config.getWebIds()).thenReturn(ALICE_WEBID_MAP);
        final var credentials = new TestCredentials();
        credentials.webId = baseUri.resolve("404webID").toString();
        when(config.getCredentials("notfoundwebid")).thenReturn(credentials);
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.authenticate("notfoundwebid"));
        assertEquals("Failed to read WebID Document for [" + credentials.webId + "] Caused by: " +
                        "org.solid.testharness.utils.TestHarnessException: " +
                        "Error response=404 trying to get content for " + credentials.webId,
                exception.getMessage());
    }


    @Test
    void authenticateNoCredentials() {
        when(config.getWebIds()).thenReturn(ALICE_WEBID_MAP);
        when(config.getSolidIdentityProvider()).thenReturn(baseUri);
        final var credentials = new TestCredentials();
        credentials.webId = baseUri.resolve("webID").toString();
        when(config.getCredentials("nocredentials")).thenReturn(credentials);

        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.authenticate("nocredentials"));
        assertEquals("Neither login credentials nor refresh token details provided for nocredentials: ["
                        + credentials.webId + "]",
                exception.getMessage());
    }

    @Test
    void authenticateLoginSession() {
        when(config.getWebIds()).thenReturn(ALICE_WEBID_MAP);
        when(config.getOrigin()).thenReturn("https://origin/goodcode");
        setupLogin(baseUri, "login", "/login/password", null);

        final SolidClientProvider solidClientProvider = authManager.authenticate("login");
        assertNotNull(solidClientProvider.getClient().getAccessToken());
        assertEquals(solidClientProvider.getClient().getAccessToken(),
                solidClientProvider.getClient().requestAccessToken());
    }

    @Test
    void authenticateLoginUserRegistration() {
        when(config.getWebIds()).thenReturn(ALICE_WEBID_MAP);
        when(config.getOrigin()).thenReturn("https://origin/form");
        setupLogin(baseUri, "login", "/login/password",
                "/idp/register");

        final SolidClientProvider solidClientProvider = authManager.authenticate("login");
        assertNotNull(solidClientProvider.getClient().getAccessToken());
        assertEquals(solidClientProvider.getClient().getAccessToken(),
                solidClientProvider.getClient().requestAccessToken());
    }

    @Test
    void authenticateRefreshCredentials() {
        when(config.getWebIds()).thenReturn(ALICE_WEBID_MAP);
        when(config.getSolidIdentityProvider()).thenReturn(baseUri);
        final TestCredentials credentials = new TestCredentials();
        credentials.webId = baseUri.resolve("webID").toString();
        credentials.refreshToken = Optional.of("REFRESH");
        credentials.clientId = Optional.of("CLIENTID");
        credentials.clientSecret = Optional.of("CLIENTSECRET");
        when(config.getCredentials("refresh_token")).thenReturn(credentials);

        final SolidClientProvider solidClientProvider = authManager.authenticate("refresh_token");
        assertNotNull(solidClientProvider.getClient().getAccessToken());
        assertEquals(solidClientProvider.getClient().getAccessToken(),
                solidClientProvider.getClient().requestAccessToken());
    }

    @Test
    void authenticateClientCredentials() {
        when(config.getWebIds()).thenReturn(ALICE_WEBID_MAP);
        when(config.getSolidIdentityProvider()).thenReturn(baseUri);
        final TestCredentials credentials = new TestCredentials();
        credentials.webId = baseUri.resolve("webID").toString();
        credentials.clientId = Optional.of("CLIENTID");
        credentials.clientSecret = Optional.of("CLIENTSECRET");
        when(config.getCredentials("client_credentials")).thenReturn(credentials);

        final SolidClientProvider solidClientProvider = authManager.authenticate("client_credentials");
        assertNotNull(solidClientProvider.getClient().getAccessToken());
        assertEquals(solidClientProvider.getClient().getAccessToken(),
                solidClientProvider.getClient().requestAccessToken());
    }

    public void setBaseUri(final URI baseUri) {
        this.baseUri = baseUri;
    }

    private void setupLogin(final URI idpBaseUri, final String testId,
                            final String loginEndpoint, final String userRegistrationEndpoint) {
        when(config.getSolidIdentityProvider()).thenReturn(idpBaseUri);
        if (loginEndpoint != null) {
            when(config.getLoginEndpoint()).thenReturn(idpBaseUri.resolve(loginEndpoint));
        }
        if (userRegistrationEndpoint != null) {
            when(config.getUserRegistrationEndpoint()).thenReturn(idpBaseUri.resolve(userRegistrationEndpoint));
        }
        final TestCredentials credentials = new TestCredentials();
        credentials.webId = baseUri.resolve("webID").toString();
        credentials.username = Optional.of("USERNAME");
        credentials.password = Optional.of("PASSWORD");
        when(config.getCredentials(testId)).thenReturn(credentials);
    }
}
