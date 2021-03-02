package org.solid.testharness.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

public class AuthManager {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.http.AuthManager");
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String REFRESH_TOKEN = "refreshToken";
    private static final String CLIENT_ID = "clientId";
    private static final String CLIENT_SECRET = "clientSecret";
    private static final String ORIGIN = "origin";
    private static final String LOGIN_PATH = "loginPath";

    private static UncheckedObjectMapper objectMapper = new UncheckedObjectMapper();

    public static final synchronized SolidClient authenticate(String user, Map<String, Object> config) throws Exception {
        Map<String, Object> tokens = null;
        // TODO: All access to config will be class based, not direct access to the JSON Map
        Map<String, String> userConfig = (Map<String, String>) ((Map<String, Object>) config.get("users")).get(user);
        boolean supportsAuthentication = (boolean) ((Map<String, Object>) config.get("features")).get("authentication");
        int aclCachePause = config.containsKey("aclCachePause") ? (int) config.get("aclCachePause") : 0;
        boolean disableDPoP = config.containsKey("disableDPoP") ? (boolean) config.get("disableDPoP") : false;

        if (!supportsAuthentication) {
            return new SolidClient();
        }

        Client.Builder builder = new Client.Builder(user);
        if (!disableDPoP) {
            builder.withDpopSupport();
        }
        Client authClient = null;
        if (ClientRegistry.hasClient(user)) {
            authClient = ClientRegistry.getClient(user);
        } else {
            authClient = builder.build();
            ClientRegistry.register(user, authClient);

            if (userConfig.containsKey(USERNAME) && userConfig.containsKey(PASSWORD)) {
                tokens = loginAndGetAccessToken(authClient, userConfig, config);
            } else if (userConfig.containsKey(REFRESH_TOKEN) && userConfig.containsKey(CLIENT_ID) && userConfig.containsKey(CLIENT_SECRET)) {
                tokens = exchangeRefreshToken(authClient, userConfig, config);
            } else {
                logger.warn("Neither login credentials nor refresh token details provided");
                return null;
            }
            String accessToken = (String) tokens.get("access_token");
            logger.debug("access_token {}", accessToken);
            authClient.setAccessToken(accessToken);
        }
        SolidClient solidClient = new SolidClient(authClient, aclCachePause);
        if (user.equals("alice") && config.containsKey("setupRootAcl") && (boolean) config.get("setupRootAcl")) {
            solidClient.setupRootAcl((String) config.get("serverRoot"), userConfig.get("webID"));
        }
        return solidClient;
    }

    private static final Map<String, Object> exchangeRefreshToken(Client authClient, Map<String, String> userConfig, Map<String, Object> config) throws Exception {
        String solidIdentityProvider = (String) config.get("solidIdentityProvider");
        logger.info("Exchange refresh token at {} for {}", solidIdentityProvider, authClient.getUser());

        Map<Object, Object> data = new HashMap<>();
        data.put("grant_type", "refresh_token");
        data.put("refresh_token", userConfig.get(REFRESH_TOKEN));
        // TODO: This should get the token endpoint from the oidc configuration
        URI tokenEndpoint = URI.create(solidIdentityProvider + "/token");
        HttpRequest request = authClient.signRequest(
                HttpUtils.newRequestBuilder(tokenEndpoint)
                        .header("Authorization", "Basic " + base64Encode(userConfig.get(CLIENT_ID) + ':' + userConfig.get(CLIENT_SECRET)))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Accept", "application/json")
                        .POST(HttpUtils.ofFormData(data))
        ).build();

        HttpResponse<String> response = authClient.send(request, HttpResponse.BodyHandlers.ofString());
        logger.debug("Response {}: {}", response.statusCode(), response.body());
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body());
        } else {
            logger.error("FAILED TO GET TOKEN {}", response.body());
            throw new Exception("Token exchange failed - do you need a new refresh_token");
        }
    }

    private static final Map<String, Object> loginAndGetAccessToken(Client authClient, Map<String, String> userConfig, Map<String, Object> config) throws Exception {
        String solidIdentityProvider = (String) config.get("solidIdentityProvider");
        String appOrigin = (String) config.get(ORIGIN);

        logger.info("Login and get access token at {} for {}", solidIdentityProvider, authClient.getUser());
        Client client = ClientRegistry.getClient(ClientRegistry.SESSION_BASED);
        URI uri = URI.create(solidIdentityProvider);

        Map<Object, Object> data = new HashMap<>();
        data.put(USERNAME, userConfig.get(USERNAME));
        data.put(PASSWORD, userConfig.get(PASSWORD));
        HttpRequest request = HttpUtils.newRequestBuilder(uri.resolve((String) config.get(LOGIN_PATH)))
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
        Map<String, Object> oidcConfig = objectMapper.readValue(jsonResponse.body());

        if (!solidIdentityProvider.equals(oidcConfig.get("issuer"))) {
            throw new Exception("The configured issuer does not match the Solid Identity Provider");
        }

        URI authorizeEndpoint = URI.create((String)oidcConfig.get("authorization_endpoint"));
        URI tokenEndpoint = URI.create((String)oidcConfig.get("token_endpoint"));
        URI registrationEndpoint = URI.create((String)oidcConfig.get("registration_endpoint"));

        logger.debug("\n========== REGISTER");
        Map<String, Object> registration = new HashMap<>() {{
            put("application_type", "web");
            put("redirect_uris", List.of(appOrigin));
            put("token_endpoint_auth_method", "client_secret_basic");
        }};
        String registrationBody = objectMapper.writeValueAsString(registration);
        request = HttpUtils.newRequestBuilder(registrationEndpoint)
                .POST(HttpRequest.BodyPublishers.ofString(registrationBody))
                .header("Content-Type", "application/json")
                .build();
        HttpUtils.logRequest(logger, request);
        HttpResponse<String> regResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        HttpUtils.logResponse(logger, regResponse);
        Map<String, Object> clientRegistration = objectMapper.readValue(regResponse.body());

        String clientId = (String)clientRegistration.get("client_id");
        String clientSecret = (String)clientRegistration.get("client_secret");

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
        return objectMapper.readValue(tokenResponse.body());
    }

    public static final String base64Encode(String data) {
        return new String(Base64.getEncoder().encode(data.getBytes()));
    }

    private String generateCodeVerifier() throws UnsupportedEncodingException {
        SecureRandom secureRandom = new SecureRandom();
        byte[] codeVerifier = new byte[32];
        secureRandom.nextBytes(codeVerifier);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
    }

    private String generateCodeChallange(String codeVerifier) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(bytes, 0, bytes.length);
        byte[] digest = messageDigest.digest();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    static class UncheckedObjectMapper extends ObjectMapper {
        Map<String, Object> readValue(String content) {
            try {
                return this.readValue(content, new TypeReference<>() {
                });
            } catch (IOException ioe) {
                throw new CompletionException(ioe);
            }
        }
    }

}
