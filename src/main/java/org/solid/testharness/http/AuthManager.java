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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.config.Config;
import org.solid.testharness.config.UserCredentials;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@ApplicationScoped
public class AuthManager {
    private static final Logger logger = LoggerFactory.getLogger(AuthManager.class);

    private static final Pattern LOGIN_FORM_ACTION = Pattern.compile(
            ".*?(?><form\\s).*?(?>method)[^=]*+=[^\"]*+\"post\".*+",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern LOCATION = Pattern.compile(".*\"location\"\\s*:\\s*\"([^\"]+)\".*",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final String IDP_GRANT_ERROR = "Identity Provider does not support grant type: ";

    @Inject
    Config config;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ClientRegistry clientRegistry;

    /*
     * Track https://github.com/solid/solid-spec/issues/138 to see if any standard develops around account management
     */

    public void registerUser(@NotNull final String user) {
        logger.info("Registering user {} at {}", user, config.getUserRegistrationEndpoint());
        final UserCredentials userConfig = config.getCredentials(user);
        if (userConfig == null) {
            throw new TestHarnessInitializationException("No user credentials were provided for " + user);
        }
        final Client client = new Client.Builder()
                .withOptionalLocalhostSupport(config.getUserRegistrationEndpoint())
                .build();
        final Map<Object, Object> data = Map.of(
            HttpConstants.EMAIL, userConfig.username().orElseThrow(),
            HttpConstants.PASSWORD, userConfig.password().orElseThrow(),
            HttpConstants.CONFIRM_PASSWORD, userConfig.password().orElseThrow(),
            "podName", user,
            "register", "ok",
            "createWebId", "ok",
            "createPod", "ok"
        );
        postRequest(client, config.getUserRegistrationEndpoint(),
                HttpUtils.ofFormData(data), HttpConstants.MEDIA_TYPE_APPLICATION_FORM_URLENCODED,
                HttpResponse.BodyHandlers.ofString(),
                "User registration");
    }

    public SolidClientProvider authenticate(@NotNull final String user) {
        requireNonNull(user, "user must not be null");
        final Client authClient;
        if (clientRegistry.hasClient(user)) {
            authClient = clientRegistry.getClient(user);
        } else {
            logger.debug("Build new client for {}", user);
            final UserCredentials userConfig = config.getCredentials(user);
            if (userConfig == null) {
                logger.warn("UserCredentials missing for {}", user);
                throw new TestHarnessInitializationException("No user credentials were provided for " + user);
            }
            final URI oidcIssuer = Optional.ofNullable(userConfig.getIdp()).orElse(config.getSolidIdentityProvider());

            authClient = new Client.Builder(user)
                    .withDpopSupport()
                    .withOptionalLocalhostSupport(oidcIssuer)
                    .build();
            clientRegistry.register(user, authClient);

            final OidcConfiguration oidcConfiguration = requestOidcConfiguration(authClient, oidcIssuer);

            final Tokens tokens;
            if (userConfig.isUsingUsernamePassword()) {
                // create client with session support for login
                final Client sessionClient = new Client.Builder()
                        .withSessionSupport()
                        .withOptionalLocalhostSupport(oidcIssuer)
                        .build();
                tokens = loginAndGetAccessToken(authClient, userConfig, oidcConfiguration, sessionClient);
            } else if (userConfig.isUsingRefreshToken()) {
                tokens = exchangeRefreshToken(authClient, userConfig, oidcConfiguration);
            } else if (userConfig.isUsingClientCredentials()) {
                tokens = clientCredentialsAccessToken(authClient, userConfig, oidcConfiguration);
            } else {
                logger.warn("UserCredentials for {}: {}", user, userConfig);
                throw new TestHarnessInitializationException("Neither login credentials nor refresh token details " +
                        "provided for " + user);
            }
            authClient.setAccessToken(tokens.getAccessToken());
        }
        return new SolidClientProvider(authClient);
    }

    Tokens exchangeRefreshToken(final Client authClient, final UserCredentials userConfig,
                                final OidcConfiguration oidcConfig) {
        logger.info("Exchange refresh token for {}", authClient.getUser());
        if (!oidcConfig.getGrantTypesSupported().contains(HttpConstants.REFRESH_TOKEN)) {
            throw new TestHarnessInitializationException(IDP_GRANT_ERROR +
                    HttpConstants.REFRESH_TOKEN);
        }
        return requestToken(authClient, oidcConfig,
                userConfig.clientId().orElseThrow(),
                userConfig.clientSecret().orElseThrow(),
                Map.of(
                        HttpConstants.GRANT_TYPE, HttpConstants.REFRESH_TOKEN,
                        HttpConstants.REFRESH_TOKEN, userConfig.refreshToken()
                )
        );
    }

    Tokens clientCredentialsAccessToken(final Client authClient, final UserCredentials userConfig,
                                        final OidcConfiguration oidcConfig) {
        logger.info("Use client credentials to get access token for {}", authClient.getUser());
        if (!oidcConfig.getGrantTypesSupported().contains(HttpConstants.CLIENT_CREDENTIALS)) {
            throw new TestHarnessInitializationException(IDP_GRANT_ERROR +
                    HttpConstants.CLIENT_CREDENTIALS);
        }
        return requestToken(authClient, oidcConfig,
                userConfig.clientId().orElseThrow(),
                userConfig.clientSecret().orElseThrow(),
                Map.of(
                        HttpConstants.GRANT_TYPE, HttpConstants.CLIENT_CREDENTIALS
                )
        );
    }

    Tokens loginAndGetAccessToken(final Client authClient, final UserCredentials userConfig,
                                  final OidcConfiguration oidcConfig, final Client sessionClient) {
        logger.info("Login and get access token for {}", authClient.getUser());
        if (!oidcConfig.getGrantTypesSupported().contains(HttpConstants.AUTHORIZATION_CODE_TYPE)) {
            throw new TestHarnessInitializationException(IDP_GRANT_ERROR +
                    HttpConstants.AUTHORIZATION_CODE_TYPE);
        }

        if (config.getUserRegistrationEndpoint() == null) {
            // login to the session at the start instead of during the auth flow
            startLoginSession(sessionClient, userConfig, config.getLoginEndpoint());
        }

        final String appOrigin = config.getOrigin();
        final Registration clientRegistration = registerClient(sessionClient, oidcConfig, appOrigin);
        final String clientId = clientRegistration.getClientId();
        final String codeVerifier = generateCodeVerifier();

        final String authCode = requestAuthorizationCode(sessionClient, oidcConfig, appOrigin, clientId,
                userConfig, codeVerifier);

        return requestToken(authClient, oidcConfig,
                clientId,
                clientRegistration.getClientSecret(),
                Map.of(
                        HttpConstants.GRANT_TYPE, HttpConstants.AUTHORIZATION_CODE_TYPE,
                        HttpConstants.CODE, authCode,
                        HttpConstants.REDIRECT_URI, appOrigin,
                        HttpConstants.CLIENT_ID, clientId,
                        HttpConstants.CODE_VERIFIER, codeVerifier
                )
        );
    }

    OidcConfiguration requestOidcConfiguration(final Client client, final URI oidcIssuer) {
        logger.debug("\n========== GET CONFIGURATION");
        final URI openIdEndpoint = oidcIssuer.resolve(HttpConstants.OPENID_CONFIGURATION);
        final HttpResponse<String> response = getRequest(client, openIdEndpoint,
                HttpConstants.MEDIA_TYPE_APPLICATION_JSON, "OIDC configuration");
        try {
            final OidcConfiguration oidcConfig = objectMapper.readValue(response.body(), OidcConfiguration.class);
            if (!oidcIssuer.equals(oidcConfig.getIssuer())) {
                throw new TestHarnessInitializationException("The configured issuer does not match the Solid IdP");
            }
            return oidcConfig;
        } catch (JsonProcessingException e) {
            throw new TestHarnessInitializationException("Failed to read the OIDC config at " + openIdEndpoint, e);
        }
    }

    void startLoginSession(final Client client, final UserCredentials userConfig, final URI loginEndpoint) {
        logger.debug("\n========== START SESSION");
        postRequest(client, loginEndpoint,
                HttpUtils.ofFormData(Map.of(
                        HttpConstants.USERNAME, userConfig.username().orElseThrow(),
                        HttpConstants.PASSWORD, userConfig.password().orElseThrow()
                )),
                HttpConstants.MEDIA_TYPE_APPLICATION_FORM_URLENCODED,
                HttpResponse.BodyHandlers.discarding(),
                "Login");
    }

    Registration registerClient(final Client client, final OidcConfiguration oidcConfig,
                                        final String appOrigin) {
        logger.debug("\n========== REGISTER");
        final Registration registration = new Registration();
        registration.setApplicationType("web");
        registration.setRedirectUris(List.of(appOrigin));
        registration.setTokenEndpointAuthMethod(HttpConstants.AUTHORIZATION_METHOD);
        try {
            final String registrationBody = objectMapper.writeValueAsString(registration);
            final HttpResponse<String> response = postRequest(client, oidcConfig.getRegistrationEndpoint(),
                    HttpRequest.BodyPublishers.ofString(registrationBody),
                    HttpConstants.MEDIA_TYPE_APPLICATION_JSON,
                    HttpResponse.BodyHandlers.ofString(),
                    "Registration");
            return objectMapper.readValue(response.body(), Registration.class);
        } catch (IOException e) {
            throw new TestHarnessInitializationException("Failed to process JSON in client registration", e);
        }
    }

    String requestAuthorizationCode(final Client client, final OidcConfiguration oidcConfig,
                                            final String appOrigin, final String clientId,
                                            final UserCredentials userConfig, final String codeVerifier) {
        logger.debug("\n========== AUTHORIZE");
        final URI authorizeEndpoint = oidcConfig.getAuthorizeEndpoint();
        final Map<String, String> requestParams = Map.of(
            HttpConstants.RESPONSE_TYPE, HttpConstants.CODE,
            HttpConstants.REDIRECT_URI, appOrigin,
            HttpConstants.SCOPE, HttpConstants.OPENID,
            HttpConstants.CLIENT_ID, clientId,
            HttpConstants.CODE_CHALLENGE_METHOD, "S256",
            HttpConstants.CODE_CHALLENGE, generateCodeChallenge(codeVerifier, "SHA-256")
        );

        final String authorizeUrl = requestParams.keySet().stream()
                .map(key -> key + "=" + HttpUtils.encodeValue(requestParams.get(key)))
                .collect(Collectors.joining("&", authorizeEndpoint + "?", ""));
        URI redirectUrl = URI.create(authorizeUrl);

        do {
            logger.debug("Authorize URL {}", redirectUrl);
            final HttpResponse<String> response = getRequest(client, redirectUrl,
                    HttpConstants.MEDIA_TYPE_TEXT_HTML, "Authorization code");
            final Optional<String> locationHeader = response.headers().firstValue(HttpConstants.HEADER_LOCATION);
            redirectUrl = locationHeader.map(authorizeEndpoint::resolve).orElse(null);
            if (redirectUrl == null) {
                final Matcher m = LOGIN_FORM_ACTION.matcher(response.body());
                if (m.matches()) {
                    // login occurs during auth flow
                    redirectUrl = idpLogin(client, oidcConfig.getIssuer().resolve(response.uri()),
                            userConfig, authorizeEndpoint);
                }
            }
        } while (redirectUrl != null && !redirectUrl.toString().startsWith(appOrigin));

        if (redirectUrl == null) {
            // Please make sure the cookie is valid, and add "${appOrigin}" as a trusted app!
            throw new TestHarnessInitializationException("Failed to follow authentication redirects");
        }
        final Map<String, List<String>> params = HttpUtils.splitQuery(redirectUrl);
        final String authCode = params.containsKey(HttpConstants.CODE) ? params.get(HttpConstants.CODE).get(0) : null;
        if (authCode == null) {
            // Please make sure the cookie is valid, and add "${appOrigin}" as a trusted app!
            throw new TestHarnessInitializationException("Failed to get authorization code");
        }
        return authCode;
    }

    URI idpLogin(final Client client, final URI loginEndpoint, final UserCredentials userConfig,
                         final URI authorizeEndpoint) {
        logger.debug("\n========== IDP LOGIN");
        final HttpResponse<String> response = postRequest(client, loginEndpoint,
                HttpUtils.ofFormData(Map.of(
                        HttpConstants.EMAIL, userConfig.username().orElseThrow(),
                        HttpConstants.PASSWORD, userConfig.password().orElseThrow()
                )),
                HttpConstants.MEDIA_TYPE_APPLICATION_FORM_URLENCODED,
                HttpResponse.BodyHandlers.ofString(),
                "Authorization");
        if (HttpUtils.isRedirect(response.statusCode())) {
            final Optional<String> locationHeader = response.headers().firstValue(HttpConstants.HEADER_LOCATION);
            return locationHeader.map(authorizeEndpoint::resolve).orElse(null);
        } else {
            // CSS v3 onwards may return the location in a JSON body instead of via a redirect
            final Matcher m = LOCATION.matcher(response.body());
            if (m.matches()) {
                return authorizeEndpoint.resolve(m.group(1));
            } else {
                return null;
            }
        }
    }

    Tokens requestToken(final Client authClient, final OidcConfiguration oidcConfig,
                                final String clientId, final String clientSecret,
                                final Map<Object, Object> tokenRequestData) {
        logger.debug("\n========== ACCESS TOKEN");
        final String authHeader = HttpConstants.PREFIX_BASIC + base64Encode(clientId + ':' + clientSecret);
        final HttpRequest.Builder requestBuilder = authClient.signRequest(
                HttpUtils.newRequestBuilder(oidcConfig.getTokenEndpoint())
                        .header(HttpConstants.HEADER_AUTHORIZATION, authHeader)
                        .header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.MEDIA_TYPE_APPLICATION_FORM_URLENCODED)
                        .header(HttpConstants.HEADER_ACCEPT, HttpConstants.MEDIA_TYPE_APPLICATION_JSON)
                        .POST(HttpUtils.ofFormData(tokenRequestData))
        );
        final HttpResponse<String> response;
        try {
            final HttpRequest request = requestBuilder.build();
            HttpUtils.logRequest(logger, request);
            response = authClient.send(request, HttpResponse.BodyHandlers.ofString());
            HttpUtils.logResponse(logger, response);
        } catch (Exception e) {
            throw new TestHarnessInitializationException("Token exchange request failed", e);
        }
        final String body = response.body();
        if (response.statusCode() != HttpConstants.STATUS_OK) {
            logger.error("FAILED TO GET ACCESS TOKEN {}", body);
            throw new TestHarnessInitializationException("Token exchange failed for grant type: " +
                    tokenRequestData.get(HttpConstants.GRANT_TYPE));
        }
        try {
            return objectMapper.readValue(response.body(), Tokens.class);
        } catch (Exception e) {
            throw new TestHarnessInitializationException("Failed to parse token response", e);
        }
    }

    HttpResponse<String> getRequest(final Client client, final URI uri, final String type, final String stage) {
        final HttpRequest request = HttpUtils.newRequestBuilder(uri)
                .header(HttpConstants.HEADER_ACCEPT, type)
                .build();
        HttpUtils.logRequest(logger, request);
        final HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new TestHarnessInitializationException(stage + " GET request failed", e);
        }
        HttpUtils.logResponse(logger, response);
        if (!HttpUtils.isSuccessfulOrRedirect(response.statusCode())) {
            throw new TestHarnessInitializationException(stage + " GET request failed with status code " +
                    response.statusCode());
        }
        return response;
    }

    <T> HttpResponse<T> postRequest(final Client client, final URI uri,
                                    final HttpRequest.BodyPublisher publisher, final String type,
                                    final HttpResponse.BodyHandler<T> bodyHandler,
                                    final String stage) {
        final HttpRequest request = HttpUtils.newRequestBuilder(uri)
                .header(HttpConstants.HEADER_CONTENT_TYPE, type)
                .POST(publisher)
                .build();
        HttpUtils.logRequest(logger, request);
        final HttpResponse<T> response;
        try {
            response = client.send(request, bodyHandler);
        } catch (Exception e) {
            throw new TestHarnessInitializationException(stage + " POST request failed", e);
        }
        HttpUtils.logResponse(logger, response);
        if (!HttpUtils.isSuccessfulOrRedirect(response.statusCode())) {
            throw new TestHarnessInitializationException(stage + " POST request failed with status code " +
                    response.statusCode());
        }
        return response;
    }

    private String base64Encode(final String data) {
        return Base64.getEncoder().encodeToString(data.getBytes());
    }

    private String generateCodeVerifier() {
        final SecureRandom secureRandom = new SecureRandom();
        final byte[] codeVerifier = new byte[32];
        secureRandom.nextBytes(codeVerifier);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
    }

    String generateCodeChallenge(final String codeVerifier, final String algorithm) {
        final byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
        final MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new TestHarnessInitializationException("Failed to generate code challenge", e);
        }
        messageDigest.update(bytes, 0, bytes.length);
        final byte[] digest = messageDigest.digest();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }
}
