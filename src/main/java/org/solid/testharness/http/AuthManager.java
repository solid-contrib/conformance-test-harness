package org.solid.testharness.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.config.TargetServer;
import org.solid.testharness.config.UserCredentials;

import javax.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class AuthManager {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.http.AuthManager");
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    private ObjectMapper objectMapper = new ObjectMapper();

    public SolidClient authenticate(String user, TargetServer targetServer) throws Exception {
        if (!targetServer.getFeatures().getOrDefault("authentication", false)) {
            return new SolidClient();
        }
        UserCredentials userConfig = targetServer.getUsers().get(user);
        Client authClient;
        if (ClientRegistry.hasClient(user)) {
            authClient = ClientRegistry.getClient(user);
        } else {
            logger.debug("Build new client for {}", user);
            Client.Builder builder = new Client.Builder(user);
            if (!targetServer.isDisableDPoP()) {
                builder.withDpopSupport();
            }
            if (targetServer.getServerRoot().contains("localhost")) {
                builder.withLocalhostSupport();
            }
            authClient = builder.build();
            ClientRegistry.register(user, authClient);

            Tokens tokens;
            if (userConfig.isUsingUsernamePassword()) {
                tokens = loginAndGetAccessToken(authClient, userConfig, targetServer);
            } else if (userConfig.isUsingRefreshToken()) {
                tokens = exchangeRefreshToken(authClient, userConfig, targetServer);
            } else {
                logger.warn("Neither login credentials nor refresh token details provided");
                return null;
            }
            String accessToken = tokens.getAccessToken();
            logger.debug("access_token ({}) {}", user, accessToken);
            authClient.setAccessToken(accessToken);
        }

        SolidClient solidClient = new SolidClient(authClient);
        if (user.equals("alice") && targetServer.isSetupRootAcl()) {
            logger.debug("**** Setup root acl");
            // CSS has no root acl by default so added here TODO: check whether this is relevant
            solidClient.setupRootAcl(targetServer.getServerRoot(), userConfig.getWebID());
        }
        return solidClient;
    }

    private Tokens exchangeRefreshToken(Client authClient, UserCredentials userConfig, TargetServer targetServer) throws Exception {
        String solidIdentityProvider = targetServer.getSolidIdentityProvider();
        logger.info("Exchange refresh token at {} for {}", solidIdentityProvider, authClient.getUser());

        Map<Object, Object> data = new HashMap<>();
        data.put("grant_type", "refresh_token");
        data.put("refresh_token", userConfig.getRefreshToken());

        // TODO: This should get the token endpoint from the oidc configuration
        URI tokenEndpoint = URI.create(solidIdentityProvider + "/token");
        HttpRequest request = authClient.signRequest(
                HttpUtils.newRequestBuilder(tokenEndpoint)
                        .header("Authorization", "Basic " + base64Encode(userConfig.getClientId() + ':' + userConfig.getClientSecret()))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Accept", "application/json")
                        .POST(HttpUtils.ofFormData(data))
        ).build();

        HttpResponse<String> response = authClient.send(request, HttpResponse.BodyHandlers.ofString());
        logger.debug("Response {}: {}", response.statusCode(), response.body());
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Tokens.class);
        } else {
            logger.error("FAILED TO GET ACCESS TOKEN {}", response.body());
            throw new Exception("Token exchange failed - do you need a new refresh_token");
        }
    }

    private Tokens loginAndGetAccessToken(Client authClient, UserCredentials userConfig, TargetServer config) throws Exception {
        String solidIdentityProvider = config.getSolidIdentityProvider();
        String appOrigin = config.getOrigin();

        logger.info("Login and get access token at {} for {}", solidIdentityProvider, authClient.getUser());
        Client client = ClientRegistry.getClient(ClientRegistry.SESSION_BASED);
        URI uri = URI.create(solidIdentityProvider);

        Map<Object, Object> data = new HashMap<>();
        data.put(USERNAME, userConfig.getUsername());
        data.put(PASSWORD, userConfig.getPassword());
        HttpRequest request = HttpUtils.newRequestBuilder(config.getLoginEndpoint())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpUtils.ofFormData(data))
                .build();
        HttpUtils.logRequest(logger, request);
        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        HttpUtils.logResponse(logger, response);

        logger.debug("\n========== GET CONFIGURATION");
        request = HttpUtils.newRequestBuilder(uri.resolve("/.well-known/openid-configuration"))
                .header("Accept", "application/json")
                .build();
        HttpUtils.logRequest(logger, request);
        HttpResponse<String> jsonResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        HttpUtils.logResponse(logger, jsonResponse);
        OidcConfiguration oidcConfig = objectMapper.readValue(jsonResponse.body(), OidcConfiguration.class);

        if (!solidIdentityProvider.equals(oidcConfig.getIssuer())) {
            throw new Exception("The configured issuer does not match the Solid Identity Provider");
        }

        URI authorizeEndpoint = URI.create(oidcConfig.getAuthorizeEndpoint());
        URI tokenEndpoint = URI.create(oidcConfig.getTokenEndpoint());
        URI registrationEndpoint = URI.create(oidcConfig.getRegistrationEndpoint());

        logger.debug("\n========== REGISTER");
        Registration registration = new Registration();
        registration.setApplicationType("web");
        registration.setRedirectUris(List.of(appOrigin));
        registration.setTokenEndpointAuthMethod("client_secret_basic");
        String registrationBody = objectMapper.writeValueAsString(registration);
        request = HttpUtils.newRequestBuilder(registrationEndpoint)
                .POST(HttpRequest.BodyPublishers.ofString(registrationBody))
                .header("Content-Type", "application/json")
                .build();
        HttpUtils.logRequest(logger, request);
        HttpResponse<String> regResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        HttpUtils.logResponse(logger, regResponse);
        Registration clientRegistration = objectMapper.readValue(regResponse.body(), Registration.class);

        String clientId = clientRegistration.getClientId();
        String clientSecret = clientRegistration.getClientSecret();

        logger.debug("\n========== AUTHORIZE");
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("response_type", "code");
        requestParams.put("redirect_uri", appOrigin);
        requestParams.put("scope", "openid");
        requestParams.put("client_id", clientId);
        String authorizaUrl = requestParams.keySet().stream()
                .map(key -> key + "=" + HttpUtils.encodeValue(requestParams.get(key)))
                .collect(Collectors.joining("&", authorizeEndpoint + "?", ""));
        URI redirectUrl = URI.create(authorizaUrl);
        do {
            logger.debug("Authorize URL {}", redirectUrl);
            request = HttpUtils.newRequestBuilder(redirectUrl).build();
            HttpUtils.logRequest(logger, request);
            HttpResponse<Void> authResponse = client.send(request, HttpResponse.BodyHandlers.discarding());
            HttpUtils.logResponse(logger, authResponse);
            Optional<String> locationHeader = authResponse.headers().firstValue("Location");
            redirectUrl = locationHeader.isPresent() ? authorizeEndpoint.resolve(locationHeader.get()) : null;
        } while (redirectUrl != null && !redirectUrl.toString().startsWith(appOrigin));

        if (redirectUrl == null) {
            // Please make sure the cookie is valid, and add "${appOrigin}" as a trusted app!
            throw new Exception("Failed to follow authentication redirects");
        }
        Map<String, List<String>> params = HttpUtils.splitQuery(redirectUrl);
        String authCode = params.containsKey("code") ? params.get("code").get(0) : null;
        if (authCode == null) {
            // Please make sure the cookie is valid, and add "${appOrigin}" as a trusted app!
            throw new Exception("Failed to get auth code");
        }
        logger.debug("authCode {}}", authCode);

        logger.debug("\n========== ACCESS TOKEN");
        Map<Object, Object> tokenRequestData = new HashMap<>();
        tokenRequestData.put("grant_type", "authorization_code");
        tokenRequestData.put("code", authCode);
        tokenRequestData.put("redirect_uri", appOrigin);
        tokenRequestData.put("client_id", clientId);

        request = authClient.signRequest(
                HttpUtils.newRequestBuilder(tokenEndpoint)
                        .header("Authorization", "Basic " + base64Encode(clientId + ':' + clientSecret))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Accept", "application/json")
                        .POST(HttpUtils.ofFormData(tokenRequestData))
        ).build();
        HttpUtils.logRequest(logger, request);
        HttpResponse<String> tokenResponse = authClient.send(request, HttpResponse.BodyHandlers.ofString());
        HttpUtils.logResponse(logger, tokenResponse);
        return objectMapper.readValue(tokenResponse.body(), Tokens.class);
    }

    public String base64Encode(String data) {
        return new String(Base64.getEncoder().encode(data.getBytes()));
    }

    private String generateCodeVerifier() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] codeVerifier = new byte[32];
        secureRandom.nextBytes(codeVerifier);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
    }

    private String generateCodeChallange(String codeVerifier) throws NoSuchAlgorithmException {
        byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(bytes, 0, bytes.length);
        byte[] digest = messageDigest.digest();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }
}
