package org.solid.testharness.http;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.testharness.config.Config;
import org.solid.testharness.config.TestCredentials;
import org.solid.testharness.utils.TestHarnessInitializationException;
import org.solid.testharness.utils.TestUtils;

import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import java.io.IOException;
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
public class AuthManagerMethodTest {
    private static final URI TEST_URI = URI.create(TestUtils.SAMPLE_BASE).resolve("/");
    private Client client;

    @InjectMock
    Config config;

    @InjectMock
    ClientRegistry clientRegistry;

    @Inject
    AuthManager authManager;

    @BeforeEach
    void setup() {
        client = mock(Client.class);
        when(config.getConnectTimeout()).thenReturn(2000);
        when(config.getReadTimeout()).thenReturn(2000);
        when(config.getAgent()).thenReturn("AGENT");
    }

    @Test
    void registerUserNoCredentials() {
        when(config.getCredentials("test")).thenReturn(null);
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.registerUser("test"));
        assertEquals("No user credentials were provided for test", exception.getMessage());
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
    void exchangeRefreshToken() throws IOException, InterruptedException {
        final OidcConfiguration oidcConfig = mockOidcConfig(List.of(HttpConstants.REFRESH_TOKEN));
        final Client client = mockSigningClient();
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "{\"access_token\":\"token\"}");
        doReturn(mockResponse).when(client).send(any(), any());
        final TestCredentials testCredentials = new TestCredentials();
        testCredentials.clientId = Optional.of("id");
        testCredentials.clientSecret = Optional.of("secret");
        final Tokens tokens = authManager.exchangeRefreshToken(client, testCredentials, oidcConfig);
        assertEquals("token", tokens.getAccessToken());
    }

    @Test
    void exchangeRefreshTokenWrongGrant() {
        final OidcConfiguration oidcConfig = mockOidcConfig(Collections.emptyList());
        when(client.getUser()).thenReturn("USER");
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.exchangeRefreshToken(client, null, oidcConfig));
        assertEquals("Identity Provider does not support grant type: " + HttpConstants.REFRESH_TOKEN,
                exception.getMessage());
    }

    @Test
    void clientCredentialsAccessToken() throws IOException, InterruptedException {
        final OidcConfiguration oidcConfig = mockOidcConfig(List.of(HttpConstants.CLIENT_CREDENTIALS));
        final Client client = mockSigningClient();
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "{\"access_token\":\"token\"}");
        doReturn(mockResponse).when(client).send(any(), any());
        final TestCredentials testCredentials = new TestCredentials();
        testCredentials.clientId = Optional.of("id");
        testCredentials.clientSecret = Optional.of("secret");
        final Tokens tokens = authManager.clientCredentialsAccessToken(client, testCredentials, oidcConfig);
        assertEquals("token", tokens.getAccessToken());
    }

    @Test
    void clientCredentialsAccessTokenWrongGrant() {
        final OidcConfiguration oidcConfig = mockOidcConfig(Collections.emptyList());
        final Client client = mockSigningClient();
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.clientCredentialsAccessToken(client, null, oidcConfig));
        assertEquals("Identity Provider does not support grant type: " + HttpConstants.CLIENT_CREDENTIALS,
                exception.getMessage());
    }

    @Test
    void loginAndGetAccessTokenStartSession() throws IOException, InterruptedException {
        final OidcConfiguration oidcConfig = mockOidcConfig(List.of(HttpConstants.AUTHORIZATION_CODE_TYPE));
        final Client authClient = mockSigningClient();
        when(config.getSolidIdentityProvider()).thenReturn(TEST_URI);
        when(config.getUserRegistrationEndpoint()).thenReturn(null);
        when(config.getLoginEndpoint()).thenReturn(TEST_URI.resolve("/login"));
        when(config.getOrigin()).thenReturn("https://ORIGIN");
        final HttpResponse<String> mockResponseSession = TestUtils.mockStringResponse(200, "OK");
        final HttpResponse<String> mockResponseRegisterClient = TestUtils.mockStringResponse(200,
                "{\"client_id\":\"CLIENTID\"}");
        final HttpResponse<String> mockResponseAuthorize = TestUtils.mockStringResponse(
                302, "",
                Map.of(HttpConstants.HEADER_LOCATION, List.of("https://ORIGIN?" + HttpConstants.CODE + "=CODE")));
        final HttpResponse<String> mockResponseToken = TestUtils.mockStringResponse(200,
                "{\"access_token\":\"TOKEN\"}");

        doReturn(mockResponseSession)
                .doReturn(mockResponseRegisterClient)
                .doReturn(mockResponseAuthorize)
                .doReturn(mockResponseToken)
                .when(client).send(any(), any());

        final Tokens tokens = authManager.loginAndGetAccessToken(authClient, createUserPwd(), oidcConfig, client);
        assertEquals("TOKEN", tokens.getAccessToken());
    }

    @Test
    void loginAndGetAccessTokenUserRegistration() throws IOException, InterruptedException {
        final OidcConfiguration oidcConfig = mockOidcConfig(List.of(HttpConstants.AUTHORIZATION_CODE_TYPE));
        final Client authClient = mockSigningClient();
        when(config.getSolidIdentityProvider()).thenReturn(TEST_URI);
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
        final HttpResponse<String> mockResponseToken = TestUtils.mockStringResponse(200,
                "{\"access_token\":\"TOKEN\"}");

        doReturn(mockResponseRegisterClient)
                .doReturn(mockResponseAuthorizeForm)
                .doReturn(mockResponseAuthorizeLogin)
                .doReturn(mockResponseAuthorize)
                .doReturn(mockResponseToken)
                .when(client).send(any(), any());

        final Tokens tokens = authManager.loginAndGetAccessToken(authClient, createUserPwd(), oidcConfig, client);
        assertEquals("TOKEN", tokens.getAccessToken());
    }

    @Test
    void loginAndGetAccessTokenBadGrant() {
        final OidcConfiguration oidcConfig = mockOidcConfig(Collections.emptyList());
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.loginAndGetAccessToken(client, createUserPwd(), oidcConfig, client));
        assertEquals("Identity Provider does not support grant type: " + HttpConstants.AUTHORIZATION_CODE_TYPE,
                exception.getMessage());
    }

    @Test
    void requestOidcConfiguration() throws IOException, InterruptedException {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200,
                "{\"issuer\":\"" + TEST_URI + "\"}");
        doReturn(mockResponse).when(client).send(any(), any());
        final OidcConfiguration oidcConfig = authManager.requestOidcConfiguration(client, TEST_URI);
        assertEquals(TEST_URI, oidcConfig.getIssuer());
    }

    @Test
    void requestOidcConfigurationNoMatch() throws IOException, InterruptedException {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "{\"issuer\":\"BAD\"}");
        doReturn(mockResponse).when(client).send(any(), any());
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.requestOidcConfiguration(client, TEST_URI));
        assertEquals("The configured issuer does not match the Solid IdP", exception.getMessage());
    }

    @Test
    void requestOidcConfigurationJsonError() throws IOException, InterruptedException {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "not json");
        doReturn(mockResponse).when(client).send(any(), any());
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.requestOidcConfiguration(client, TEST_URI));
        assertEquals("Failed to read the OIDC config at " + TEST_URI.resolve(HttpConstants.OPENID_CONFIGURATION),
                exception.getMessage());
    }

    @Test
    void startLoginSession() throws IOException, InterruptedException {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "OK");
        doReturn(mockResponse).when(client).send(any(), any());
        assertDoesNotThrow(() -> authManager.startLoginSession(client, createUserPwd(), TEST_URI));
    }

    @Test
    void registerClient() throws IOException, InterruptedException {
        final OidcConfiguration oidcConfig = mockOidcConfig(null);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "{\"client_id\":\"CLIENTID\"}");
        doReturn(mockResponse).when(client).send(any(), any());
        final Registration registration = authManager.registerClient(client, oidcConfig, "ORIGIN");
        assertEquals("CLIENTID", registration.getClientId());
    }

    @Test
    void registerClientFailed() throws IOException, InterruptedException {
        final OidcConfiguration oidcConfig = mockOidcConfig(null);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "not json");
        doReturn(mockResponse).when(client).send(any(), any());
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.registerClient(client, oidcConfig, "ORIGIN"));
        assertEquals("Failed to process JSON in client registration", exception.getMessage());
    }

    @Test
    void requestAuthorizationCodeImmediate() throws IOException, InterruptedException {
        final OidcConfiguration oidcConfig = mockOidcConfig(null);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(
                302, "",
                Map.of(HttpConstants.HEADER_LOCATION, List.of("https://ORIGIN?" + HttpConstants.CODE + "=CODE")));
        doReturn(mockResponse).when(client).send(any(), any());
        final String code = authManager.requestAuthorizationCode(client, oidcConfig, "https://ORIGIN", "CLIENTID",
                createUserPwd(), "CODE_VERIFIER");
        assertEquals("CODE", code);
    }

    @Test
    void requestAuthorizationCodeRedirect() throws IOException, InterruptedException {
        final OidcConfiguration oidcConfig = mockOidcConfig(null);
        final HttpResponse<String> mockResponse1 = TestUtils.mockStringResponse(
                302, "",
                Map.of(HttpConstants.HEADER_LOCATION, List.of("redirect")));
        final HttpResponse<String> mockResponse2 = TestUtils.mockStringResponse(
                302, "",
                Map.of(HttpConstants.HEADER_LOCATION, List.of("https://ORIGIN?" + HttpConstants.CODE + "=CODE")));
        doReturn(mockResponse1).doReturn(mockResponse2).when(client).send(any(), any());
        final String code = authManager.requestAuthorizationCode(client, oidcConfig, "https://ORIGIN", "CLIENTID",
                createUserPwd(), "CODE_VERIFIER");
        assertEquals("CODE", code);
    }

    @Test
    void requestAuthorizationCodeRedirectForm() throws IOException, InterruptedException {
        final OidcConfiguration oidcConfig = mockOidcConfig(null);
        final HttpResponse<String> mockResponse1 = TestUtils.mockStringResponse(200, "<form method=\"POST\"");
        when(mockResponse1.uri()).thenReturn(TEST_URI.resolve("/login"));
        final HttpResponse<String> mockResponse2 = TestUtils.mockStringResponse(200, "{\"location\":\"newloc\"}");
        final HttpResponse<String> mockResponse3 = TestUtils.mockStringResponse(
                302, "",
                Map.of(HttpConstants.HEADER_LOCATION, List.of("https://ORIGIN?" + HttpConstants.CODE + "=CODE")));
        doReturn(mockResponse1).doReturn(mockResponse2).doReturn(mockResponse3).when(client).send(any(), any());
        final String code = authManager.requestAuthorizationCode(client, oidcConfig, "https://ORIGIN", "CLIENTID",
                createUserPwd(), "CODE_VERIFIER");
        assertEquals("CODE", code);
    }

    @Test
    void requestAuthorizationCodeImmediateNoCode() throws IOException, InterruptedException {
        final OidcConfiguration oidcConfig = mockOidcConfig(null);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(
                302, "",
                Map.of(HttpConstants.HEADER_LOCATION, List.of("https://ORIGIN")));
        doReturn(mockResponse).when(client).send(any(), any());
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.requestAuthorizationCode(client, oidcConfig, "https://ORIGIN", "CLIENTID",
                        createUserPwd(), "CODE_VERIFIER"));
        assertEquals("Failed to get authorization code", exception.getMessage());
    }

    @Test
    void requestAuthorizationCodeNoRedirectNotForm() throws IOException, InterruptedException {
        final OidcConfiguration oidcConfig = mockOidcConfig(null);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "NOFORM");
        doReturn(mockResponse).when(client).send(any(), any());
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.requestAuthorizationCode(client, oidcConfig, "https://ORIGIN", "CLIENTID",
                        createUserPwd(), "CODE_VERIFIER"));
        assertEquals("Failed to follow authentication redirects", exception.getMessage());
    }

    @Test
    void idpLoginRedirect() throws IOException, InterruptedException {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(
                302, "",
                Map.of(HttpConstants.HEADER_LOCATION, List.of(TEST_URI.resolve("/redirect").toString())));
        doReturn(mockResponse).when(client).send(any(), any());
        final URI uri = authManager.idpLogin(client, TEST_URI,  createUserPwd(), TEST_URI.resolve("/auth"));
        assertEquals(TEST_URI.resolve("/redirect"), uri);
    }

    @Test
    void idpLoginRedirectNoHeader() throws IOException, InterruptedException {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(302, "");
        doReturn(mockResponse).when(client).send(any(), any());
        final URI uri = authManager.idpLogin(client, TEST_URI,  createUserPwd(), TEST_URI.resolve("/auth"));
        assertNull(uri);
    }

    @Test
    void idpLoginJson() throws IOException, InterruptedException {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "{\"location\":\"newloc\"}");
        doReturn(mockResponse).when(client).send(any(), any());
        final URI uri = authManager.idpLogin(client, TEST_URI,  createUserPwd(), TEST_URI.resolve("/auth"));
        assertEquals(TEST_URI.resolve("/newloc"), uri);
    }

    @Test
    void idpLoginJsonFailed() throws IOException, InterruptedException {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "no location");
        doReturn(mockResponse).when(client).send(any(), any());
        final URI uri = authManager.idpLogin(client, TEST_URI,  createUserPwd(), TEST_URI.resolve("/auth"));
        assertNull(uri);
    }

    @Test
    void requestToken() throws IOException, InterruptedException {
        final OidcConfiguration oidcConfig = mockOidcConfig(null);
        final Client client = mockSigningClient();
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "{\"access_token\":\"TOKEN\"}");
        doReturn(mockResponse).when(client).send(any(), any());
        final Tokens tokens = authManager.requestToken(client, oidcConfig, "id", "secret", Collections.emptyMap());
        assertEquals("TOKEN", tokens.getAccessToken());
    }

    @Test
    void requestTokenRequestFailed() throws IOException, InterruptedException {
        final OidcConfiguration oidcConfig = mockOidcConfig(null);
        final Client client = mockSigningClient();
        when(client.send(any(), any())).thenThrow(new IOException(("FAIL")));
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.requestToken(client, oidcConfig,
                        "id", "secret", Map.of(HttpConstants.GRANT_TYPE, Collections.emptyMap())));
        assertEquals("Token exchange request failed", exception.getMessage());
    }

    @Test
    void requestTokenBadResponse() throws IOException, InterruptedException {
        final OidcConfiguration oidcConfig = mockOidcConfig(null);
        final Client client = mockSigningClient();
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(400, "ERROR");
        doReturn(mockResponse).when(client).send(any(), any());
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.requestToken(client, oidcConfig,
                        "id", "secret", Map.of(HttpConstants.GRANT_TYPE, "GRANT")));
        assertEquals("Token exchange failed for grant type: GRANT", exception.getMessage());
    }

    @Test
    void requestTokenTokenParsingFail() throws IOException, InterruptedException {
        final OidcConfiguration oidcConfig = mockOidcConfig(null);
        final Client client = mockSigningClient();
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "not json");
        doReturn(mockResponse).when(client).send(any(), any());
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.requestToken(client, oidcConfig,
                        "id", "secret", Collections.emptyMap()));
        assertEquals("Failed to parse token response", exception.getMessage());
    }

    @Test
    void getRequest() throws IOException, InterruptedException {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "Good response");
        doReturn(mockResponse).when(client).send(any(), any());
        final HttpResponse<String> response = authManager.getRequest(client, TEST_URI,
                        HttpConstants.MEDIA_TYPE_TEXT_TURTLE, "Test");
        assertEquals(200, response.statusCode());
        assertEquals("Good response", response.body());
    }

    @Test
    void getRequestFailed() throws IOException, InterruptedException {
        when(client.send(any(), any())).thenThrow(new IOException("FAIL"));
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.getRequest(client, TEST_URI,
                        HttpConstants.MEDIA_TYPE_TEXT_TURTLE, "Test"));
        assertEquals("Test GET request failed", exception.getMessage());
    }

    @Test
    void getRequestBadResponse() throws IOException, InterruptedException {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(404, "Not found");
        doReturn(mockResponse).when(client).send(any(), any());
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.getRequest(client, TEST_URI,
                        HttpConstants.MEDIA_TYPE_TEXT_TURTLE, "Test"));
        assertEquals("Test GET request failed with status code 404", exception.getMessage());
    }

    @Test
    void postRequest() throws IOException, InterruptedException {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "Good response");
        doReturn(mockResponse).when(client).send(any(), any());
        final HttpResponse<String> response = authManager.postRequest(client, TEST_URI,
                        HttpUtils.ofFormData(Collections.emptyMap()),
                HttpConstants.MEDIA_TYPE_APPLICATION_FORM_URLENCODED,
                        HttpResponse.BodyHandlers.ofString(), "Test");
        assertEquals(200, response.statusCode());
        assertEquals("Good response", response.body());
    }

    @Test
    void postRequestFailed() throws IOException, InterruptedException {
        when(client.send(any(), any())).thenThrow(new IOException("FAIL"));
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.postRequest(client, TEST_URI,
                        HttpUtils.ofFormData(Collections.emptyMap()),
                        HttpConstants.MEDIA_TYPE_APPLICATION_FORM_URLENCODED,
                        HttpResponse.BodyHandlers.ofString(), "Test"));
        assertEquals("Test POST request failed", exception.getMessage());
    }

    @Test
    void postRequestBadResponse() throws IOException, InterruptedException {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(404, "Not found");
        doReturn(mockResponse).when(client).send(any(), any());
        final TestHarnessInitializationException exception = assertThrows(TestHarnessInitializationException.class,
                () -> authManager.postRequest(client, TEST_URI,
                        HttpUtils.ofFormData(Collections.emptyMap()),
                        HttpConstants.MEDIA_TYPE_APPLICATION_FORM_URLENCODED,
                        HttpResponse.BodyHandlers.ofString(), "Test"));
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
        assertEquals("Failed to generate code challenge", exception.getMessage());
    }

    OidcConfiguration mockOidcConfig(final List<String> grantTypes) {
        final OidcConfiguration oidcConfig = mock(OidcConfiguration.class);
        when(oidcConfig.getIssuer()).thenReturn(TEST_URI);
        when(oidcConfig.getTokenEndpoint()).thenReturn(TEST_URI);
        when(oidcConfig.getRegistrationEndpoint()).thenReturn(TEST_URI.resolve("/registration"));
        when(oidcConfig.getAuthorizeEndpoint()).thenReturn(TEST_URI.resolve("/authorize"));
        if (grantTypes != null) {
            when(oidcConfig.getGrantTypesSupported()).thenReturn(grantTypes);
        }
        return oidcConfig;
    }

    Client mockSigningClient() {
        final HttpRequest.Builder builder = mock(HttpRequest.Builder.class);
        final HttpRequest request = TestUtils.mockRequest();
        when(builder.build()).thenReturn(request);
        when(client.getUser()).thenReturn("USER");
        when(client.signRequest(any())).thenReturn(builder);
        return client;
    }

    TestCredentials createUserPwd() {
        final TestCredentials testCredentials = new TestCredentials();
        testCredentials.username = Optional.of("username");
        testCredentials.password = Optional.of("password");
        return testCredentials;
    }

}
