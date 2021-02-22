package org.solid.testharness.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.http.HttpClient;
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
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.utils.AuthManager");
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String REFRESH_TOKEN = "refreshToken";
    private static final String CLIENT_ID = "clientId";
    private static final String CLIENT_SECRET = "clientSecret";
    private static final String ORIGIN = "origin";
    private static final String LOGIN_PATH = "loginPath";

    private static UncheckedObjectMapper objectMapper = new UncheckedObjectMapper();

    public static final String getAccessToken(String solidIdentityProvider, Map<String, String> config) throws Exception {
        Map<String, Object> tokens = null;
        if (config.containsKey(USERNAME) && config.containsKey(PASSWORD)) {
            tokens = loginAndGetAccessToken(solidIdentityProvider, config);
        } else if (config.containsKey(REFRESH_TOKEN) && config.containsKey(CLIENT_ID) && config.containsKey(CLIENT_SECRET)) {
            tokens = exchangeRefreshToken(solidIdentityProvider, config);
        } else {
            logger.warn("Neither login credentials nor refresh token details provided");
            return null;
        }
        String accessToken = (String)tokens.get("access_token");
        logger.debug("access_token {}", accessToken);
        return accessToken;
    }

    private static final Map<String, Object> exchangeRefreshToken(String solidIdentityProvider, Map<String, String> config) throws Exception {
        logger.info("Exchange refresh token at {}", solidIdentityProvider);
        SolidClient solidClient = new SolidClient.Builder().build();
        HttpClient client = solidClient.getHttpClient();
        String basicAuth = AuthManager.base64Encode(config.get(CLIENT_ID) + ':' + config.get(CLIENT_SECRET));
        Map<Object, Object> data = new HashMap<>();
        data.put("grant_type", "refresh_token");
        data.put("refresh_token", config.get(REFRESH_TOKEN));
        // TODO: This should get the token endpoint from the oidc configuration
        HttpRequest request = HttpRequest.newBuilder(URI.create(solidIdentityProvider + "/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + basicAuth)
                .header("Accept", "application/json")
                .POST(HttpUtils.ofFormData(data))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        logger.debug("Response {}: {}", response.statusCode(), response.body());
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body());
        } else {
            logger.error("FAILED TO GET TOKEN {}", response.body());
            throw new Exception("Token exchange failed - do you need a new refresh_token");
        }
    }

    private static final Map<String, Object> loginAndGetAccessToken(String solidIdentityProvider, Map<String, String> config) throws Exception {
        logger.info("Login and get access token at {}", solidIdentityProvider);
        SolidClient solidClient = new SolidClient.Builder().withSessionSupport().withDpopSupport().build();
        HttpClient client = solidClient.getHttpClient();
        URI uri = URI.create(solidIdentityProvider);
        String appOrigin = config.get(ORIGIN);

        Map<Object, Object> data = new HashMap<>();
        data.put(USERNAME, config.get(USERNAME));
        data.put(PASSWORD, config.get(PASSWORD));
        HttpRequest request = HttpRequest.newBuilder(uri.resolve(config.get(LOGIN_PATH)))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpUtils.ofFormData(data))
                .build();
        HttpUtils.logRequest(logger, request);
        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        HttpUtils.logResponse(logger, response);

        logger.debug("\n========== GET CONFIGURATION");
        request = HttpRequest.newBuilder(uri.resolve("/.well-known/openid-configuration"))
                .header("Accept", "application/json")
                .build();
        HttpUtils.logRequest(logger, request);
        HttpResponse<String> jsonResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        HttpUtils.logResponse(logger, jsonResponse);
        Map<String, Object> oidcConfig = objectMapper.readValue(jsonResponse.body());

        if (!solidIdentityProvider.equals(oidcConfig.get("issuer"))) {
            throw new Exception("The configured issuer does not match the Solid Identity Provider");
        }

        String authorizeEndpoint = (String)oidcConfig.get("authorization_endpoint");
        URI authorizeEndpointUri = URI.create(authorizeEndpoint);
        String tokenEndpoint = (String)oidcConfig.get("token_endpoint");
        String registrationEndpoint = (String)oidcConfig.get("registration_endpoint");

        logger.debug("\n========== REGISTER");
        Map<String, Object> registration = new HashMap<>() {{
            put("application_type", "web");
            put("redirect_uris", List.of(appOrigin));
            put("token_endpoint_auth_method", "client_secret_basic");
//                put("code_challenge_method", "S256");
        }};
        String registrationBody = objectMapper.writeValueAsString(registration);
        request = HttpRequest.newBuilder(URI.create(registrationEndpoint))
                .POST(HttpRequest.BodyPublishers.ofString(registrationBody))
                .header("Content-Type", "application/json")
                .build();
        HttpUtils.logRequest(logger, request);
        HttpResponse<String> regResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        HttpUtils.logResponse(logger, regResponse);
        Map<String, Object> clientRegistration = objectMapper.readValue(regResponse.body());

        String clientId = (String)clientRegistration.get("client_id");
        String clientSecret = (String)clientRegistration.get("client_secret");

//            String codeVerifier = generateCodeVerifier();
//            String codeChallenge = generateCodeChallange(codeVerifier);

        logger.debug("\n========== AUTHORIZE");
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("response_type", "code"); //id_token
        requestParams.put("redirect_uri", appOrigin);
        requestParams.put("scope", "openid"); // profile  offline_access
        requestParams.put("client_id", clientId);
//            requestParams.put("code_challenge_method", "S256");
//            requestParams.put("code_challenge", codeChallenge);
//            requestParams.put("state", "global");
        String authorizaUrl = requestParams.keySet().stream()
                .map(key -> key + "=" + HttpUtils.encodeValue(requestParams.get(key)))
                .collect(Collectors.joining("&", authorizeEndpoint + "?", ""));
        URI redirectUrl = URI.create(authorizaUrl);
        do {
            logger.debug("Authorize URL {}", redirectUrl);
            request = HttpRequest.newBuilder(redirectUrl).build();
            HttpUtils.logRequest(logger, request);
            HttpResponse<Void> authResponse = client.send(request, HttpResponse.BodyHandlers.discarding());
            HttpUtils.logResponse(logger, authResponse);
            Optional<String> locationHeader = authResponse.headers().firstValue("Location");
            redirectUrl = locationHeader.isPresent() ? authorizeEndpointUri.resolve(locationHeader.get()) : null;
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
        String basicAuth = base64Encode(clientId + ':' + clientSecret);

        Map<Object, Object> tokenRequestData = new HashMap<>();
        data.put("grant_type", "authorization_code");
//            data.put("code_verifier", codeVerifier);
        data.put("code", authCode);
        data.put("redirect_uri", appOrigin);
        data.put("client_id", clientId);

        request = solidClient.signRequest(
                HttpRequest.newBuilder(uri.resolve(tokenEndpoint))
                        .header("Authorization", "Basic " + basicAuth)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpUtils.ofFormData(data))
        ).build();
        HttpUtils.logRequest(logger, request);
        HttpResponse<String> tokenResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
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
