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

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import org.jose4j.jwk.JsonWebKeySet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.solid.testharness.config.Config;
import org.solid.testharness.config.TestCredentials;
import org.solid.testharness.config.UserCredentials;
import org.solid.testharness.utils.TestHarnessInitializationException;
import org.solid.testharness.utils.TestUtils;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class AuthManagerMethodTest {
    private static final URI TEST_URI = URI.create(TestUtils.SAMPLE_BASE).resolve("/");
    private static final HttpRequest.BodyPublisher EMPTY_FORM_DATA = HttpUtils.ofFormData(Collections.emptyMap());
    private static final HttpResponse.BodyHandler<String> STRING_BODY_HANDLER = HttpResponse.BodyHandlers.ofString();
    private Client client;
    private AutoCloseable closeable;

    @Captor
    private ArgumentCaptor<OidcConfiguration> oidcConfigArgCaptor;

    @Captor
    private ArgumentCaptor<Map<Object, Object>> mapArgCaptor;

    @InjectMock
    Config config;

    @InjectMock
    ClientRegistry clientRegistry;

    @Inject
    AuthManager authManager;

    UserCredentials testUserCredentials;
    UserCredentials testClientCredentials;

    @BeforeEach
    void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        client = mock(Client.class);
        when(config.getConnectTimeout()).thenReturn(2000);
        when(config.getReadTimeout()).thenReturn(2000);
        when(config.getAgent()).thenReturn("AGENT");
        testUserCredentials = createUserPwd();
        testClientCredentials = createIdSecret();
    }

    @AfterEach
    void close() throws Exception {
        closeable.close();
    }

    @Test
    void registerUserNoCredentials() {
        when(config.getCredentials("test")).thenReturn(null);
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.registerUser("test"));
        assertEquals("No user credentials were provided for [test]", exception.getMessage());
    }

    @Test
    void authenticateNullUser() {
        assertThrows(ConstraintViolationException.class, () -> authManager.authenticate(null));
    }

    @Test
    void authenticateAlreadyExists() {
        final Client client = new Client.Builder("existing").build();
        when(clientRegistry.hasClient("existing")).thenReturn(true);
        when(clientRegistry.getClient("existing")).thenReturn(client);
        final SolidClientProvider solidClientProvider = authManager.authenticate("existing");
        assertEquals("existing", solidClientProvider.getClient().getUser());
        verify(config, never()).getCredentials("existing");
    }

    @Test
    void authenticateNullCredentials() {
        when(config.getCredentials("test")).thenReturn(null);
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.authenticate("test"));
        assertEquals("No user credentials were provided for test", exception.getMessage());
    }

    @Test
    void exchangeRefreshToken() {
        final OidcConfiguration oidcConfig = mockOidcConfig(List.of(HttpConstants.REFRESH_TOKEN));
        authManager.exchangeRefreshToken(client, testClientCredentials, oidcConfig);

        verify(client).saveTokenRequestData(oidcConfigArgCaptor.capture(), eq("id"), eq("secret"),
                mapArgCaptor.capture());
        assertEquals(TEST_URI, oidcConfigArgCaptor.getValue().getTokenEndpoint());
        assertEquals(HttpConstants.REFRESH_TOKEN, mapArgCaptor.getValue().get(HttpConstants.GRANT_TYPE));
        verify(client).requestAccessToken();
    }

    @Test
    void exchangeRefreshTokenWrongGrant() {
        final OidcConfiguration oidcConfig = mockOidcConfig(Collections.emptyList());
        when(client.getUser()).thenReturn("USER");
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.exchangeRefreshToken(client, testClientCredentials, oidcConfig));
        assertEquals("Identity Provider does not support grant type: " + HttpConstants.REFRESH_TOKEN,
                exception.getMessage());
    }

    @Test
    void clientCredentialsAccessToken() {
        final OidcConfiguration oidcConfig = mockOidcConfig(List.of(HttpConstants.CLIENT_CREDENTIALS));
        authManager.clientCredentialsAccessToken(client, testClientCredentials, oidcConfig);

        verify(client).saveTokenRequestData(oidcConfigArgCaptor.capture(), eq("id"), eq("secret"),
                mapArgCaptor.capture());
        assertEquals(TEST_URI, oidcConfigArgCaptor.getValue().getTokenEndpoint());
        assertEquals(HttpConstants.CLIENT_CREDENTIALS, mapArgCaptor.getValue().get(HttpConstants.GRANT_TYPE));
        verify(client).requestAccessToken();
    }

    @Test
    void clientCredentialsAccessTokenWrongGrant() {
        final OidcConfiguration oidcConfig = mockOidcConfig(Collections.emptyList());
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.clientCredentialsAccessToken(client, testClientCredentials, oidcConfig));
        assertEquals("Identity Provider does not support grant type: " + HttpConstants.CLIENT_CREDENTIALS,
                exception.getMessage());
    }

    @Test
    void loginAndGetAccessTokenStartSession() {
        final OidcConfiguration oidcConfig = mockOidcConfig(List.of(HttpConstants.AUTHORIZATION_CODE_TYPE));
        when(config.getUserRegistrationEndpoint()).thenReturn(null);
        when(config.getLoginEndpoint()).thenReturn(TEST_URI.resolve("/login"));
        when(config.getOrigin()).thenReturn("https://ORIGIN");
        final HttpResponse<String> mockResponseSession = TestUtils.mockStringResponse(200, "OK");
        final HttpResponse<String> mockResponseRegisterClient = TestUtils.mockStringResponse(200,
                "{\"client_id\":\"CLIENTID\"}");
        final HttpResponse<String> mockResponseAuthorize = TestUtils.mockStringResponse(
                302, "",
                Map.of(HttpConstants.HEADER_LOCATION, List.of("https://ORIGIN?" + HttpConstants.CODE + "=CODE")));

        doReturn(mockResponseSession)
                .doReturn(mockResponseRegisterClient)
                .doReturn(mockResponseAuthorize)
                .when(client).send(any(), any());
        authManager.loginAndGetAccessToken(client, testUserCredentials, oidcConfig, client);

        verify(client).saveTokenRequestData(oidcConfigArgCaptor.capture(), eq("CLIENTID"), any(),
                mapArgCaptor.capture());
        assertEquals(TEST_URI, oidcConfigArgCaptor.getValue().getTokenEndpoint());
        assertEquals(HttpConstants.AUTHORIZATION_CODE_TYPE, mapArgCaptor.getValue().get(HttpConstants.GRANT_TYPE));
        assertEquals("CODE", mapArgCaptor.getValue().get(HttpConstants.CODE));
        verify(client).requestAccessToken();
    }

    @Test
    void loginAndGetAccessTokenUserRegistration() {
        final OidcConfiguration oidcConfig = mockOidcConfig(List.of(HttpConstants.AUTHORIZATION_CODE_TYPE));
        when(config.getUserRegistrationEndpoint()).thenReturn(TEST_URI.resolve("/register"));
        when(config.getOrigin()).thenReturn("https://ORIGIN");
        final HttpResponse<String> mockResponseRegisterClient = TestUtils.mockStringResponse(200,
                "{\"client_id\":\"CLIENTID\"}");
        final HttpResponse<String> mockResponseAuthorizeForm = TestUtils.mockStringResponse(200,
                "<form method=\"POST\"");
        when(mockResponseAuthorizeForm.uri()).thenReturn(TEST_URI.resolve("/login"));
        final HttpResponse<String> mockResponseAuthorizeLogin = TestUtils.mockStringResponse(200,
                "{\"location\":\"newloc\"}");
        final HttpResponse<String> mockResponseAuthorize = TestUtils.mockStringResponse(
                302, "",
                Map.of(HttpConstants.HEADER_LOCATION, List.of("https://ORIGIN?" + HttpConstants.CODE + "=CODE")));

        doReturn(mockResponseRegisterClient)
                .doReturn(mockResponseAuthorizeForm)
                .doReturn(mockResponseAuthorizeLogin)
                .doReturn(mockResponseAuthorize)
                .when(client).send(any(), any());

        authManager.loginAndGetAccessToken(client, testUserCredentials, oidcConfig, client);

        verify(client).saveTokenRequestData(oidcConfigArgCaptor.capture(), eq("CLIENTID"), any(),
                mapArgCaptor.capture());
        assertEquals(TEST_URI, oidcConfigArgCaptor.getValue().getTokenEndpoint());
        assertEquals(HttpConstants.AUTHORIZATION_CODE_TYPE, mapArgCaptor.getValue().get(HttpConstants.GRANT_TYPE));
        assertEquals("CODE", mapArgCaptor.getValue().get(HttpConstants.CODE));
        verify(client).requestAccessToken();
    }

    @Test
    void loginAndGetAccessTokenBadGrant() {
        final OidcConfiguration oidcConfig = mockOidcConfig(Collections.emptyList());
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.loginAndGetAccessToken(client, testUserCredentials, oidcConfig, client));
        assertEquals("Identity Provider does not support grant type: " + HttpConstants.AUTHORIZATION_CODE_TYPE,
                exception.getMessage());
    }

    @Test
    void requestOidcConfiguration() {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200,
                "{\"issuer\":\"" + TEST_URI + "\"}");
        doReturn(mockResponse).when(client).send(any(), any());
        final OidcConfiguration oidcConfig = authManager.requestOidcConfiguration(client, TEST_URI);
        assertEquals(TEST_URI, oidcConfig.getIssuer());
    }

    @Test
    void requestOidcConfigurationNoMatch() {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "{\"issuer\":\"BAD\"}");
        doReturn(mockResponse).when(client).send(any(), any());
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.requestOidcConfiguration(client, TEST_URI));
        assertEquals("The configured issuer [" + TEST_URI +
                "] does not match the Solid IdP [BAD/]", exception.getMessage());
    }

    @Test
    void requestOidcConfigurationJsonError() {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "not json");
        doReturn(mockResponse).when(client).send(any(), any());
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.requestOidcConfiguration(client, TEST_URI));
        assertTrue(exception.getMessage().startsWith("Failed to read the OIDC config at " +
                TEST_URI.resolve(HttpConstants.OPENID_CONFIGURATION)));
    }

    @Test
    void requestJwks() throws Exception {
        final OidcConfiguration oidcConfig = mockOidcConfig(null);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200,
                TestUtils.loadStringFromFile("src/test/resources/jwks.json"));
        doReturn(mockResponse).when(client).send(any(), any());
        final JsonWebKeySet jsonWebKeySet = authManager.requestJwks(client, oidcConfig);
        assertEquals(1, jsonWebKeySet.getJsonWebKeys().size());
    }

    @Test
    void requestJwksJsonError() {
        final OidcConfiguration oidcConfig = mockOidcConfig(null);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "not json");
        doReturn(mockResponse).when(client).send(any(), any());
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.requestJwks(client, oidcConfig));
        assertTrue(exception.getMessage().startsWith("Failed to read the JSON Web Key Set at " + TEST_URI));
    }
    @Test
    void startLoginSession() {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "OK");
        doReturn(mockResponse).when(client).send(any(), any());
        assertDoesNotThrow(() -> authManager.startLoginSession(client, testUserCredentials, TEST_URI));
    }

    @Test
    void registerClient() {
        final OidcConfiguration oidcConfig = mockOidcConfig(null);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "{\"client_id\":\"CLIENTID\"}");
        doReturn(mockResponse).when(client).send(any(), any());
        final Registration registration = authManager.registerClient(client, oidcConfig, "ORIGIN");
        assertEquals("CLIENTID", registration.getClientId());
    }

    @Test
    void registerClientFailed() {
        final OidcConfiguration oidcConfig = mockOidcConfig(null);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "not json");
        doReturn(mockResponse).when(client).send(any(), any());
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.registerClient(client, oidcConfig, "ORIGIN"));
        assertTrue(exception.getMessage().startsWith("Failed to process JSON in client registration"));
    }

    @Test
    void requestAuthorizationCodeImmediate() {
        final OidcConfiguration oidcConfig = mockOidcConfig(null);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(
                302, "",
                Map.of(HttpConstants.HEADER_LOCATION, List.of("https://ORIGIN?" + HttpConstants.CODE + "=CODE")));
        doReturn(mockResponse).when(client).send(any(), any());
        final String code = authManager.requestAuthorizationCode(client, oidcConfig, "https://ORIGIN", "CLIENTID",
                testUserCredentials, "CODE_VERIFIER");
        assertEquals("CODE", code);
    }

    @Test
    void requestAuthorizationCodeRedirect() {
        final OidcConfiguration oidcConfig = mockOidcConfig(null);
        final HttpResponse<String> mockResponse1 = TestUtils.mockStringResponse(
                302, "",
                Map.of(HttpConstants.HEADER_LOCATION, List.of("redirect")));
        final HttpResponse<String> mockResponse2 = TestUtils.mockStringResponse(
                302, "",
                Map.of(HttpConstants.HEADER_LOCATION, List.of("https://ORIGIN?" + HttpConstants.CODE + "=CODE")));
        doReturn(mockResponse1).doReturn(mockResponse2).when(client).send(any(), any());
        final String code = authManager.requestAuthorizationCode(client, oidcConfig, "https://ORIGIN", "CLIENTID",
                testUserCredentials, "CODE_VERIFIER");
        assertEquals("CODE", code);
    }

    @Test
    void requestAuthorizationCodeRedirectForm() {
        final OidcConfiguration oidcConfig = mockOidcConfig(null);
        final HttpResponse<String> mockResponse1 = TestUtils.mockStringResponse(200, "<form method=\"POST\"");
        when(mockResponse1.uri()).thenReturn(TEST_URI.resolve("/login"));
        final HttpResponse<String> mockResponse2 = TestUtils.mockStringResponse(200, "{\"location\":\"newloc\"}");
        final HttpResponse<String> mockResponse3 = TestUtils.mockStringResponse(
                302, "",
                Map.of(HttpConstants.HEADER_LOCATION, List.of("https://ORIGIN?" + HttpConstants.CODE + "=CODE")));
        doReturn(mockResponse1).doReturn(mockResponse2).doReturn(mockResponse3).when(client).send(any(), any());
        final String code = authManager.requestAuthorizationCode(client, oidcConfig, "https://ORIGIN", "CLIENTID",
                testUserCredentials, "CODE_VERIFIER");
        assertEquals("CODE", code);
    }

    @Test
    void requestAuthorizationCodeImmediateNoCode() {
        final OidcConfiguration oidcConfig = mockOidcConfig(null);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(
                302, "",
                Map.of(HttpConstants.HEADER_LOCATION, List.of("https://ORIGIN")));
        doReturn(mockResponse).when(client).send(any(), any());
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.requestAuthorizationCode(client, oidcConfig, "https://ORIGIN", "CLIENTID",
                        testUserCredentials, "CODE_VERIFIER"));
        assertEquals("Failed to get authorization code", exception.getMessage());
    }

    @Test
    void requestAuthorizationCodeNoRedirectNotForm() {
        final OidcConfiguration oidcConfig = mockOidcConfig(null);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "NOFORM");
        doReturn(mockResponse).when(client).send(any(), any());
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.requestAuthorizationCode(client, oidcConfig, "https://ORIGIN", "CLIENTID",
                        testUserCredentials, "CODE_VERIFIER"));
        assertEquals("Failed to follow authentication redirects", exception.getMessage());
    }

    @Test
    void idpLoginRedirect() {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(
                302, "",
                Map.of(HttpConstants.HEADER_LOCATION, List.of(TEST_URI.resolve("/redirect").toString())));
        doReturn(mockResponse).when(client).send(any(), any());
        final URI uri = authManager.idpLogin(client, TEST_URI,  testUserCredentials, TEST_URI.resolve("/auth"));
        assertEquals(TEST_URI.resolve("/redirect"), uri);
    }

    @Test
    void idpLoginRedirectNoHeader() {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(302, "");
        doReturn(mockResponse).when(client).send(any(), any());
        final URI uri = authManager.idpLogin(client, TEST_URI,  testUserCredentials, TEST_URI.resolve("/auth"));
        assertNull(uri);
    }

    @Test
    void idpLoginJson() {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "{\"location\":\"newloc\"}");
        doReturn(mockResponse).when(client).send(any(), any());
        final URI uri = authManager.idpLogin(client, TEST_URI,  testUserCredentials, TEST_URI.resolve("/auth"));
        assertEquals(TEST_URI.resolve("/newloc"), uri);
    }

    @Test
    void idpLoginJsonFailed() {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "no location");
        doReturn(mockResponse).when(client).send(any(), any());
        final URI uri = authManager.idpLogin(client, TEST_URI,  testUserCredentials, TEST_URI.resolve("/auth"));
        assertNull(uri);
    }

    @Test
    void requestToken() {
        final OidcConfiguration oidcConfig = mockOidcConfig(null);
        authManager.requestToken(client, oidcConfig, "id", "secret", Collections.emptyMap());
        verify(client).saveTokenRequestData(oidcConfig, "id", "secret", Collections.emptyMap());
        verify(client).requestAccessToken();
    }

    @Test
    void getRequest() {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "Good response");
        doReturn(mockResponse).when(client).send(any(), any());
        final HttpResponse<String> response = authManager.getRequest(client, TEST_URI,
                        HttpConstants.MEDIA_TYPE_TEXT_TURTLE, "Test");
        assertEquals(200, response.statusCode());
        assertEquals("Good response", response.body());
    }

    @Test
    void getRequestFailed() {
        when(client.send(any(), any())).thenThrow(TestUtils.createException("FAIL"));
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.getRequest(client, TEST_URI,
                        HttpConstants.MEDIA_TYPE_TEXT_TURTLE, "Test"));
        assertTrue(exception.getMessage().startsWith("Test GET request failed"));
    }

    @Test
    void getRequestBadResponse() {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(404, "Not found");
        doReturn(mockResponse).when(client).send(any(), any());
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.getRequest(client, TEST_URI,
                        HttpConstants.MEDIA_TYPE_TEXT_TURTLE, "Test"));
        assertEquals("Test GET request failed with status code 404", exception.getMessage());
    }

    @Test
    void postRequest() {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "Good response");
        doReturn(mockResponse).when(client).send(any(), any());
        final HttpResponse<String> response = authManager.postRequest(client, TEST_URI,
                EMPTY_FORM_DATA,
                HttpConstants.MEDIA_TYPE_APPLICATION_FORM_URLENCODED,
                STRING_BODY_HANDLER, "Test");
        assertEquals(200, response.statusCode());
        assertEquals("Good response", response.body());
    }

    @Test
    void postRequestFailed() {
        when(client.send(any(), any())).thenThrow(TestUtils.createException("FAIL"));
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.postRequest(client, TEST_URI,
                        EMPTY_FORM_DATA,
                        HttpConstants.MEDIA_TYPE_APPLICATION_FORM_URLENCODED,
                        STRING_BODY_HANDLER, "Test"));
        assertTrue(exception.getMessage().startsWith("Test POST request failed"));
    }

    @Test
    void postRequestBadResponse() {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(404, "Not found");
        doReturn(mockResponse).when(client).send(any(), any());
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.postRequest(client, TEST_URI,
                        EMPTY_FORM_DATA,
                        HttpConstants.MEDIA_TYPE_APPLICATION_FORM_URLENCODED,
                        STRING_BODY_HANDLER, "Test"));
        assertEquals("Test POST request failed with status code 404", exception.getMessage());
    }

    @Test
    void generateCodeChallenge() {
        assertDoesNotThrow(() -> authManager.generateCodeChallenge("CODE_VERIFIER", "SHA-256"));
    }

    @Test
    void generateCodeChallengeThrows() {
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.generateCodeChallenge("CODE_VERIFIER", "UNKNOWN"));
        assertTrue(exception.getMessage().startsWith("Failed to generate code challenge"));
    }

    OidcConfiguration mockOidcConfig(final List<String> grantTypes) {
        final OidcConfiguration oidcConfig = mock(OidcConfiguration.class);
        when(oidcConfig.getIssuer()).thenReturn(TEST_URI);
        when(oidcConfig.getTokenEndpoint()).thenReturn(TEST_URI);
        when(oidcConfig.getJwksEndpoint()).thenReturn(TEST_URI);
        when(oidcConfig.getRegistrationEndpoint()).thenReturn(TEST_URI.resolve("/registration"));
        when(oidcConfig.getAuthorizeEndpoint()).thenReturn(TEST_URI.resolve("/authorize"));
        if (grantTypes != null) {
            when(oidcConfig.getGrantTypesSupported()).thenReturn(grantTypes);
        }
        return oidcConfig;
    }

    TestCredentials createUserPwd() {
        final TestCredentials testCredentials = new TestCredentials();
        testCredentials.username = Optional.of("username");
        testCredentials.password = Optional.of("password");
        return testCredentials;
    }

    TestCredentials createIdSecret() {
        final TestCredentials testCredentials = new TestCredentials();
        testCredentials.clientId = Optional.of("id");
        testCredentials.clientSecret = Optional.of("secret");
        return testCredentials;
    }

}
