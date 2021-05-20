package org.solid.testharness.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.config.TargetServer;
import org.solid.testharness.config.UserCredentials;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class AuthManager {
    private static final Logger logger = LoggerFactory.getLogger(AuthManager.class);

    @Inject
    ClientRegistry clientRegistry;

    private ObjectMapper objectMapper = new ObjectMapper();

    public SolidClient authenticate(final String user, final TargetServer targetServer) throws Exception {
        if (!targetServer.getFeatures().getOrDefault("authentication", false)) {
            return new SolidClient();
        }
        final UserCredentials userConfig = targetServer.getUsers().get(user);
        final Client authClient;
        if (clientRegistry.hasClient(user)) {
            authClient = clientRegistry.getClient(user);
        } else {
            logger.debug("Build new client for {}", user);
            final Client.Builder builder = new Client.Builder(user);
            if (!targetServer.isDisableDPoP()) {
                builder.withDpopSupport();
            }
            if (targetServer.getServerRoot().contains("localhost")) {
                builder.withLocalhostSupport();
            }
            authClient = builder.build();
            clientRegistry.register(user, authClient);

            final Tokens tokens;
            if (userConfig.isUsingUsernamePassword()) {
                tokens = loginAndGetAccessToken(authClient, userConfig, targetServer);
            } else if (userConfig.isUsingRefreshToken()) {
                tokens = exchangeRefreshToken(authClient, userConfig, targetServer);
            } else {
                logger.warn("UserCredentials for {}: {}", user, userConfig);
                throw new Exception("Neither login credentials nor refresh token details provided for " + user);
            }
            final String accessToken = tokens.getAccessToken();
            logger.debug("access_token ({}) {}", user, accessToken);
            authClient.setAccessToken(accessToken);
        }
        return new SolidClient(authClient);
    }

    private Tokens exchangeRefreshToken(final Client authClient, final UserCredentials userConfig,
                                        final TargetServer targetServer) throws Exception {
        final String solidIdentityProvider = targetServer.getSolidIdentityProvider();
        logger.info("Exchange refresh token at {} for {}", solidIdentityProvider, authClient.getUser());

        final Map<Object, Object> data = new HashMap<>();
        data.put(HttpConstants.GRANT_TYPE, HttpConstants.REFRESH_TOKEN);
        data.put(HttpConstants.REFRESH_TOKEN, userConfig.getRefreshToken());

        // TODO: This should get the token endpoint from the oidc configuration
        final URI tokenEndpoint = URI.create(solidIdentityProvider + "/token");
        final HttpRequest request = authClient.signRequest(
                HttpUtils.newRequestBuilder(tokenEndpoint)
                        .header(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.PREFIX_BASIC +
                                base64Encode(userConfig.getClientId() + ':' + userConfig.getClientSecret()))
                        .header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.MEDIA_TYPE_APPLICATION_FORM_URLENCODED)
                        .header(HttpConstants.HEADER_ACCEPT, HttpConstants.MEDIA_TYPE_APPLICATION_JSON)
                        .POST(HttpUtils.ofFormData(data))
        ).build();

        final HttpResponse<String> response = authClient.send(request, HttpResponse.BodyHandlers.ofString());
        final String body = response.body();
        final int statusCode = response.statusCode();
        logger.debug("Response {}: {}", statusCode, body);
        if (statusCode == HttpConstants.STATUS_OK) {
            return objectMapper.readValue(response.body(), Tokens.class);
        } else {
            logger.error("FAILED TO GET ACCESS TOKEN {}", body);
            throw new Exception("Token exchange failed - do you need a new refresh_token");
        }
    }

    private Tokens loginAndGetAccessToken(final Client authClient, final UserCredentials userConfig,
                                          final TargetServer config) throws Exception {
        final String solidIdentityProvider = config.getSolidIdentityProvider();
        final String appOrigin = config.getOrigin();
        logger.info("Login and get access token at {} for {}", solidIdentityProvider, authClient.getUser());
        final Client client = clientRegistry.getClient(ClientRegistry.SESSION_BASED);
        final URI uri = URI.create(solidIdentityProvider);

        final Map<Object, Object> data = new HashMap<>();
        data.put(HttpConstants.USERNAME, userConfig.getUsername());
        data.put(HttpConstants.PASSWORD, userConfig.getPassword());
        HttpRequest request = HttpUtils.newRequestBuilder(config.getLoginEndpoint())
                .header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.MEDIA_TYPE_APPLICATION_FORM_URLENCODED)
                .POST(HttpUtils.ofFormData(data))
                .build();
        HttpUtils.logRequest(logger, request);
        final HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        HttpUtils.logResponse(logger, response);

        logger.debug("\n========== GET CONFIGURATION");
        request = HttpUtils.newRequestBuilder(uri.resolve(HttpConstants.OPENID_CONFIGURATION))
                .header(HttpConstants.HEADER_ACCEPT, HttpConstants.MEDIA_TYPE_APPLICATION_JSON)
                .build();
        HttpUtils.logRequest(logger, request);
        final HttpResponse<String> jsonResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        HttpUtils.logResponse(logger, jsonResponse);
        final OidcConfiguration oidcConfig = objectMapper.readValue(jsonResponse.body(), OidcConfiguration.class);

        if (!solidIdentityProvider.equals(oidcConfig.getIssuer())) {
            throw new Exception("The configured issuer does not match the Solid Identity Provider");
        }

        final URI authorizeEndpoint = URI.create(oidcConfig.getAuthorizeEndpoint());
        final URI tokenEndpoint = URI.create(oidcConfig.getTokenEndpoint());
        final URI registrationEndpoint = URI.create(oidcConfig.getRegistrationEndpoint());

        logger.debug("\n========== REGISTER");
        final Registration registration = new Registration();
        registration.setApplicationType("web");
        registration.setRedirectUris(List.of(appOrigin));
        registration.setTokenEndpointAuthMethod("client_secret_basic");
        final String registrationBody = objectMapper.writeValueAsString(registration);
        request = HttpUtils.newRequestBuilder(registrationEndpoint)
                .POST(HttpRequest.BodyPublishers.ofString(registrationBody))
                .header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.MEDIA_TYPE_APPLICATION_JSON)
                .build();
        HttpUtils.logRequest(logger, request);
        final HttpResponse<String> regResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        HttpUtils.logResponse(logger, regResponse);
        final Registration clientRegistration = objectMapper.readValue(regResponse.body(), Registration.class);

        final String clientId = clientRegistration.getClientId();
        final String clientSecret = clientRegistration.getClientSecret();

        logger.debug("\n========== AUTHORIZE");
        final Map<String, String> requestParams = new HashMap<>();
        requestParams.put(HttpConstants.RESPONSE_TYPE, HttpConstants.CODE);
        requestParams.put(HttpConstants.REDIRECT_URI, appOrigin);
        requestParams.put(HttpConstants.SCOPE, HttpConstants.OPENID);
        requestParams.put(HttpConstants.CLIENT_ID, clientId);
        final String authorizaUrl = requestParams.keySet().stream()
                .map(key -> key + "=" + HttpUtils.encodeValue(requestParams.get(key)))
                .collect(Collectors.joining("&", authorizeEndpoint + "?", ""));
        URI redirectUrl = URI.create(authorizaUrl);
        do {
            logger.debug("Authorize URL {}", redirectUrl);
            request = HttpUtils.newRequestBuilder(redirectUrl).build();
            HttpUtils.logRequest(logger, request);
            final HttpResponse<Void> authResponse = client.send(request, HttpResponse.BodyHandlers.discarding());
            HttpUtils.logResponse(logger, authResponse);
            final Optional<String> locationHeader = authResponse.headers().firstValue("Location");
            redirectUrl = locationHeader.map(authorizeEndpoint::resolve).orElse(null);
        } while (redirectUrl != null && !redirectUrl.toString().startsWith(appOrigin));

        if (redirectUrl == null) {
            // Please make sure the cookie is valid, and add "${appOrigin}" as a trusted app!
            throw new Exception("Failed to follow authentication redirects");
        }
        final Map<String, List<String>> params = HttpUtils.splitQuery(redirectUrl);
        final String authCode = params.containsKey(HttpConstants.CODE) ? params.get(HttpConstants.CODE).get(0) : null;
        if (authCode == null) {
            // Please make sure the cookie is valid, and add "${appOrigin}" as a trusted app!
            throw new Exception("Failed to get auth code");
        }
        logger.debug("authCode {}}", authCode);

        logger.debug("\n========== ACCESS TOKEN");
        final Map<Object, Object> tokenRequestData = new HashMap<>();
        tokenRequestData.put(HttpConstants.GRANT_TYPE, HttpConstants.AUTHORIZATION_CODE_TYPE);
        tokenRequestData.put(HttpConstants.CODE, authCode);
        tokenRequestData.put(HttpConstants.REDIRECT_URI, appOrigin);
        tokenRequestData.put(HttpConstants.CLIENT_ID, clientId);

        request = authClient.signRequest(
                HttpUtils.newRequestBuilder(tokenEndpoint)
                        .header(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.PREFIX_BASIC +
                                base64Encode(clientId + ':' + clientSecret))
                        .header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.MEDIA_TYPE_APPLICATION_FORM_URLENCODED)
                        .header(HttpConstants.HEADER_ACCEPT, HttpConstants.MEDIA_TYPE_APPLICATION_JSON)
                        .POST(HttpUtils.ofFormData(tokenRequestData))
        ).build();
        HttpUtils.logRequest(logger, request);
        final HttpResponse<String> tokenResponse = authClient.send(request, HttpResponse.BodyHandlers.ofString());
        HttpUtils.logResponse(logger, tokenResponse);
        return objectMapper.readValue(tokenResponse.body(), Tokens.class);
    }

    public String base64Encode(final String data) {
        return new String(Base64.getEncoder().encode(data.getBytes()));
    }

//    private String generateCodeVerifier() {
//        SecureRandom secureRandom = new SecureRandom();
//        byte[] codeVerifier = new byte[32];
//        secureRandom.nextBytes(codeVerifier);
//        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
//    }

//    private String generateCodeChallange(String codeVerifier) throws NoSuchAlgorithmException {
//        byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
//        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
//        messageDigest.update(bytes, 0, bytes.length);
//        byte[] digest = messageDigest.digest();
//        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
//    }
}
