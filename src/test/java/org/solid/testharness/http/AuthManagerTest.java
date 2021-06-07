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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.solid.testharness.config.Config;
import org.solid.testharness.config.TargetServer;
import org.solid.testharness.config.UserCredentials;
import org.solid.testharness.utils.TestData;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
@QuarkusTestResource(AuthenticationResource.class)
class AuthManagerTest {
    private URI baseUri;
    private Client client;

    @Inject
    AuthManager authManager;

    @InjectMock
    ClientRegistry clientRegistry;

    @InjectMock
    Config config;

    @BeforeEach
    void setup() {
        System.clearProperty("jdk.internal.httpclient.disableHostnameVerification");
        when(config.getConnectTimeout()).thenReturn(2000);
        when(config.getReadTimeout()).thenReturn(2000);
        when(config.getAgent()).thenReturn("AGENT");
        client = new Client.Builder().build();
    }

    @Test
    void authenticateNullUser() {
        final TargetServer targetServer = mock(TargetServer.class);
        assertThrows(ConstraintViolationException.class, () -> authManager.authenticate(null, targetServer));
    }

    @Test
    void authenticateNullTarget() {
        assertThrows(ConstraintViolationException.class, () -> authManager.authenticate("alice", null));
    }

    @Test
    void authenticateNonAuthenticatingTarget() throws Exception {
        when(clientRegistry.getClient(ClientRegistry.DEFAULT)).thenReturn(client);
        final TargetServer targetServer = mock(TargetServer.class);
        when(targetServer.getFeatures()).thenReturn(Collections.emptyMap());
        final SolidClient solidClient = authManager.authenticate("ignored", targetServer);
        assertTrue(solidClient.getClient().getUser().isEmpty());
    }

    @Test
    void authenticateAlreadyExists() throws Exception {
        final Client client = new Client.Builder("existing").build();
        final TargetServer targetServer = getMockTargetServer(true);
        when(clientRegistry.hasClient("existing")).thenReturn(true);
        when(clientRegistry.getClient("existing")).thenReturn(client);
        final SolidClient solidClient = authManager.authenticate("existing", targetServer);
        assertEquals("existing", solidClient.getClient().getUser());
        verify(config, never()).getCredentials("existing");
    }

    @Test
    void authenticateNoDpopNoOidc() {
        final TargetServer targetServer = getMockTargetServer(true);
        when(config.getSolidIdentityProvider()).thenReturn(baseUri.resolve("/404/"));
        when(config.getServerRoot()).thenReturn(URI.create(TestData.SAMPLE_BASE));

        assertThrows(TestHarnessInitializationException.class,
                () -> authManager.authenticate("test1", targetServer));

        final ArgumentCaptor<Client> argumentCaptor = ArgumentCaptor.forClass(Client.class);
        verify(clientRegistry).register(eq("test1"), argumentCaptor.capture());
        assertEquals("Client: user=test1, dPoP=false, session=false, local=false",
                argumentCaptor.getValue().toString());
    }

    @Test
    void authenticateDpopLocalhostNoOidc() {
        final TargetServer targetServer = getMockTargetServer(false);
        when(config.getSolidIdentityProvider()).thenReturn(baseUri.resolve("/404/"));
        when(config.getServerRoot()).thenReturn(URI.create("https://localhost"));
        assertThrows(TestHarnessInitializationException.class,
                () -> authManager.authenticate("test2", targetServer));

        final ArgumentCaptor<Client> argumentCaptor = ArgumentCaptor.forClass(Client.class);
        verify(clientRegistry).register(eq("test2"), argumentCaptor.capture());
        assertEquals("Client: user=test2, dPoP=true, session=false, local=true",
                argumentCaptor.getValue().toString());
    }

    @Test
    void authenticateDpopLocalhostBadIssuer() {
        final TargetServer targetServer = getMockTargetServer(false);
        when(config.getSolidIdentityProvider()).thenReturn(baseUri.resolve("/badissuer/"));
        when(config.getServerRoot()).thenReturn(URI.create("http://localhost"));

        assertThrows(TestHarnessInitializationException.class,
                () -> authManager.authenticate("test3", targetServer));

        final ArgumentCaptor<Client> argumentCaptor = ArgumentCaptor.forClass(Client.class);
        verify(clientRegistry).register(eq("test3"), argumentCaptor.capture());
        assertEquals("Client: user=test3, dPoP=true, session=false, local=true",
                argumentCaptor.getValue().toString());
    }

    @Test
    void authenticateNullCredentials() {
        final TargetServer targetServer = getMockTargetServer(false);
        when(config.getSolidIdentityProvider()).thenReturn(baseUri);
        when(config.getServerRoot()).thenReturn(URI.create(TestData.SAMPLE_BASE));
        when(config.getCredentials("test4")).thenReturn(null);

        assertThrows(TestHarnessInitializationException.class,
                () -> authManager.authenticate("test4", targetServer));
    }

    @Test
    void authenticateMissingCredentials() {
        final TargetServer targetServer = getMockTargetServer(false);
        when(config.getSolidIdentityProvider()).thenReturn(baseUri);
        when(config.getServerRoot()).thenReturn(URI.create(TestData.SAMPLE_BASE));
        when(config.getCredentials("test5")).thenReturn(new UserCredentials());

        assertThrows(TestHarnessInitializationException.class,
                () -> authManager.authenticate("test5", targetServer));
    }

    @Test
    void authenticateRefreshCredentialsFails() {
        final TargetServer targetServer = getMockTargetServer(false);
        when(config.getSolidIdentityProvider()).thenReturn(baseUri);
        when(config.getServerRoot()).thenReturn(URI.create(TestData.SAMPLE_BASE));
        final UserCredentials credentials = new UserCredentials();
        credentials.refreshToken = Optional.of("REFRESH");
        credentials.clientId = Optional.of("CLIENTID");
        credentials.clientSecret = Optional.of("BADSECRET");
        when(config.getCredentials("test7")).thenReturn(credentials);

        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.authenticate("test7", targetServer));
        assertEquals("Token exchange failed for grant type: " + HttpConstants.REFRESH_TOKEN,
                exception.getMessage());
    }

    @Test
    void authenticateRefreshCredentials() throws Exception {
        final TargetServer targetServer = getMockTargetServer(false);
        when(config.getSolidIdentityProvider()).thenReturn(baseUri);
        when(config.getServerRoot()).thenReturn(URI.create(TestData.SAMPLE_BASE));
        final UserCredentials credentials = new UserCredentials();
        credentials.refreshToken = Optional.of("REFRESH");
        credentials.clientId = Optional.of("CLIENTID");
        credentials.clientSecret = Optional.of("CLIENTSECRET");
        when(config.getCredentials("test6")).thenReturn(credentials);

        final SolidClient solidClient = authManager.authenticate("test6", targetServer);
        assertEquals("ACCESS_TOKEN", solidClient.getClient().getAccessToken());
    }

    @Test
    void authenticateLoginSessionFails() {
        final TargetServer targetServer = getMockTargetServer(false);
        when(clientRegistry.getClient(ClientRegistry.SESSION_BASED)).thenReturn(client);
        setupLogin("test8", "BADPASSWORD");

        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.authenticate("test8", targetServer));
        assertEquals("Login failed with status code 403", exception.getMessage());
    }

    @Test
    void authenticateLoginRegisterFails() {
        final TargetServer targetServer = getMockTargetServer(false);
        when(clientRegistry.getClient(ClientRegistry.SESSION_BASED)).thenReturn(client);
        when(targetServer.getOrigin()).thenReturn("BADORIGIN");
        setupLogin("test9", "PASSWORD");

        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.authenticate("test9", targetServer));
        assertEquals("Registration failed with status code 400", exception.getMessage());
    }

    @Test
    void authenticateLoginAuthorizationFails() {
        final TargetServer targetServer = getMockTargetServer(false);
        when(clientRegistry.getClient(ClientRegistry.SESSION_BASED)).thenReturn(client);
        when(targetServer.getOrigin()).thenReturn("AUTHFAIL");
        setupLogin("test10", "PASSWORD");

        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.authenticate("test10", targetServer));
        assertEquals("Authorization failed with status code 400", exception.getMessage());
    }

    @Test
    void authenticateLoginAuthorizationFailsWithoutRedirect() {
        final TargetServer targetServer = getMockTargetServer(false);
        when(clientRegistry.getClient(ClientRegistry.SESSION_BASED)).thenReturn(client);
        when(targetServer.getOrigin()).thenReturn("https://origin/noredirect");
        setupLogin("test11", "PASSWORD");

        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.authenticate("test11", targetServer));
        assertEquals("Failed to follow authentication redirects", exception.getMessage());
    }

    @Test
    void authenticateLoginNoRedirectFailsNoCode() {
        final TargetServer targetServer = getMockTargetServer(false);
        when(clientRegistry.getClient(ClientRegistry.SESSION_BASED)).thenReturn(client);
        when(targetServer.getOrigin()).thenReturn("https://origin/immediate");
        setupLogin("test12", "PASSWORD");

        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.authenticate("test12", targetServer));
        assertEquals("Failed to get authorization code", exception.getMessage());
    }

    @Test
    void authenticateLoginOneRedirectFailsNoCode() {
        final TargetServer targetServer = getMockTargetServer(false);
        when(clientRegistry.getClient(ClientRegistry.SESSION_BASED)).thenReturn(client);
        when(targetServer.getOrigin()).thenReturn("https://origin/redirect");
        setupLogin("test13", "PASSWORD");

        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.authenticate("test13", targetServer));
        assertEquals("Failed to get authorization code", exception.getMessage());
    }

    @Test
    void authenticateLoginTokenFails() throws Exception {
        final TargetServer targetServer = getMockTargetServer(false);
        when(clientRegistry.getClient(ClientRegistry.SESSION_BASED)).thenReturn(client);
        when(targetServer.getOrigin()).thenReturn("https://origin/badcode");
        setupLogin("test14", "PASSWORD");

        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.authenticate("test14", targetServer));
        assertEquals("Token exchange failed for grant type: " + HttpConstants.AUTHORIZATION_CODE_TYPE,
                exception.getMessage());
    }

    @Test
    void authenticateLoginTokenSucceeds() throws Exception {
        final TargetServer targetServer = getMockTargetServer(false);
        when(clientRegistry.getClient(ClientRegistry.SESSION_BASED)).thenReturn(client);
        when(targetServer.getOrigin()).thenReturn("https://origin/goodcode");
        setupLogin("test15", "PASSWORD");

        final SolidClient solidClient = authManager.authenticate("test15", targetServer);
        assertEquals("ACCESS_TOKEN", solidClient.getClient().getAccessToken());
    }

    public void setBaseUri(final URI baseUri) {
        this.baseUri = baseUri;
    }

    private TargetServer getMockTargetServer(final boolean disableDpop) {
        final TargetServer targetServer = mock(TargetServer.class);
        when(targetServer.getFeatures()).thenReturn(Map.of("authentication", true));
        when(targetServer.isDisableDPoP()).thenReturn(disableDpop);
        return targetServer;
    }

    private void setupLogin(final String testId, final String password) {
        when(config.getSolidIdentityProvider()).thenReturn(baseUri);
        when(config.getLoginEndpoint()).thenReturn(baseUri.resolve("/login/password"));
        when(config.getServerRoot()).thenReturn(URI.create(TestData.SAMPLE_BASE));
        final UserCredentials credentials = new UserCredentials();
        credentials.username = Optional.of("USERNAME");
        credentials.password = Optional.of(password);
        when(config.getCredentials(testId)).thenReturn(credentials);
    }
}
