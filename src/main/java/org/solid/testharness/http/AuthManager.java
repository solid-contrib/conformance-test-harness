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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.config.Config;
import org.solid.testharness.config.TargetServer;
import org.solid.testharness.config.UserCredentials;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@ApplicationScoped
public class AuthManager {
    private static final Logger logger = LoggerFactory.getLogger(AuthManager.class);
    private static final String LOCALHOST = "localhost";

    @Inject
    Config config;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ClientRegistry clientRegistry;

    public SolidClient authenticate(@NotNull final String user, @NotNull final TargetServer targetServer)
            throws Exception {
        requireNonNull(user, "user must not be null");
        requireNonNull(targetServer, "targetServer must not be null");
        if (!targetServer.getFeatures().getOrDefault("authentication", false)) {
            return new SolidClient();
        }
        final Client authClient;
        if (clientRegistry.hasClient(user)) {
            authClient = clientRegistry.getClient(user);
        } else {
            logger.debug("Build new client for {}", user);
            final Client.Builder builder = new Client.Builder(user);
            if (!targetServer.isDisableDPoP()) {
                builder.withDpopSupport();
            }
            if (LOCALHOST.equals(config.getServerRoot().getHost())) {
                builder.withLocalhostSupport();
            }
            authClient = builder.build();
            clientRegistry.register(user, authClient);

            final OidcConfiguration oidcConfiguration = requestOidcConfiguration(authClient);

            final UserCredentials userConfig = config.getCredentials(user);
            final Tokens tokens;
            if (userConfig != null && userConfig.isUsingUsernamePassword()) {
                tokens = loginAndGetAccessToken(authClient, userConfig, oidcConfiguration, targetServer);
            } else if (userConfig != null && userConfig.isUsingRefreshToken()) {
                tokens = exchangeRefreshToken(authClient, userConfig, oidcConfiguration);
            } else {
                logger.warn("UserCredentials for {}: {}", user, userConfig);
                throw new TestHarnessInitializationException("Neither login credentials nor refresh token details " +
                        "provided for " + user);
            }

            final String accessToken = tokens.getAccessToken();
            logger.debug("access_token ({}) {}", user, accessToken);
            authClient.setAccessToken(accessToken);
        }
        return new SolidClient(authClient);
    }

    private Tokens exchangeRefreshToken(final Client authClient, final UserCredentials userConfig,
                                        final OidcConfiguration oidcConfig) throws Exception {
        logger.info("Exchange refresh token for {}", authClient.getUser());
        final Map<Object, Object> data = Map.of(
                HttpConstants.GRANT_TYPE, HttpConstants.REFRESH_TOKEN,
                HttpConstants.REFRESH_TOKEN, userConfig.refreshToken
        );
        return requestToken(authClient, oidcConfig, userConfig.clientId.get(), userConfig.clientSecret.get(), data);
    }

    private Tokens loginAndGetAccessToken(final Client authClient, final UserCredentials userConfig,
                                          final OidcConfiguration oidcConfig, final TargetServer targetServer)
            throws Exception {
        logger.info("Login and get access token for {}", authClient.getUser());
        final Client client = clientRegistry.getClient(ClientRegistry.SESSION_BASED);

        startLoginSession(client, userConfig, config.getLoginEndpoint());

        final String appOrigin = targetServer.getOrigin();
        final Registration clientRegistration = registerClient(client, oidcConfig, appOrigin);
        final String clientId = clientRegistration.getClientId();

        final String authCode = requestAuthorizationCode(client, oidcConfig, appOrigin, clientId);

        final Map<Object, Object> tokenRequestData = new HashMap<>();
        tokenRequestData.put(HttpConstants.GRANT_TYPE, HttpConstants.AUTHORIZATION_CODE_TYPE);
        tokenRequestData.put(HttpConstants.CODE, authCode);
        tokenRequestData.put(HttpConstants.REDIRECT_URI, appOrigin);
        tokenRequestData.put(HttpConstants.CLIENT_ID, clientId);
        return requestToken(authClient, oidcConfig, clientId, clientRegistration.getClientSecret(), tokenRequestData);
    }

    private OidcConfiguration requestOidcConfiguration(final Client client) throws IOException, InterruptedException {
        logger.debug("\n========== GET CONFIGURATION");
        final URI solidIdentityProvider = config.getSolidIdentityProvider();
        final URI openIdEndpoint = solidIdentityProvider.resolve(HttpConstants.OPENID_CONFIGURATION);
        final HttpRequest request = HttpUtils.newRequestBuilder(openIdEndpoint)
                .header(HttpConstants.HEADER_ACCEPT, HttpConstants.MEDIA_TYPE_APPLICATION_JSON)
                .build();
        HttpUtils.logRequest(logger, request);
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        HttpUtils.logResponse(logger, response);
        try {
            final OidcConfiguration oidcConfig = objectMapper.readValue(response.body(), OidcConfiguration.class);
            if (!solidIdentityProvider.toString().equals(oidcConfig.getIssuer())) {
                throw new TestHarnessInitializationException("The configured issuer does not match the Solid IdP");
            }
            return oidcConfig;
        } catch (JsonProcessingException e) {
            throw (TestHarnessInitializationException) new TestHarnessInitializationException(
                    "Failed to read the OpenId configuration at %s: %s", openIdEndpoint.toString(), e.toString()
            ).initCause(e);
        }
    }

    private void startLoginSession(final Client client, final UserCredentials userConfig, final URI loginEndpoint)
            throws IOException, InterruptedException {
        final Map<Object, Object> data = new HashMap<>();
        data.put(HttpConstants.USERNAME, userConfig.username.get());
        data.put(HttpConstants.PASSWORD, userConfig.password.get());
        final HttpRequest request = HttpUtils.newRequestBuilder(loginEndpoint)
                .header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.MEDIA_TYPE_APPLICATION_FORM_URLENCODED)
                .POST(HttpUtils.ofFormData(data))
                .build();
        HttpUtils.logRequest(logger, request);
        final HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        HttpUtils.logResponse(logger, response);
        if (!HttpUtils.isSuccessfulOrRedirect(response.statusCode())) {
            throw new TestHarnessInitializationException("Login failed with status code " + response.statusCode());
        }
    }

    private Registration registerClient(final Client client, final OidcConfiguration oidcConfig, final String appOrigin)
            throws IOException, InterruptedException {
        logger.debug("\n========== REGISTER");
        final Registration registration = new Registration();
        registration.setApplicationType("web");
        registration.setRedirectUris(List.of(appOrigin));
        registration.setTokenEndpointAuthMethod(HttpConstants.AUTHORIZATION_METHOD);
        final String registrationBody = objectMapper.writeValueAsString(registration);
        final HttpRequest request = HttpUtils.newRequestBuilder(URI.create(oidcConfig.getRegistrationEndpoint()))
                .POST(HttpRequest.BodyPublishers.ofString(registrationBody))
                .header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.MEDIA_TYPE_APPLICATION_JSON)
                .build();
        HttpUtils.logRequest(logger, request);
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        HttpUtils.logResponse(logger, response);
        if (!HttpUtils.isSuccessfulOrRedirect(response.statusCode())) {
            throw new TestHarnessInitializationException("Registration failed with status code " +
                    response.statusCode());
        }
        return objectMapper.readValue(response.body(), Registration.class);
    }

    private String requestAuthorizationCode(final Client client, final OidcConfiguration oidcConfig,
                                            final String appOrigin, final String clientId)
            throws IOException, InterruptedException {
        logger.debug("\n========== AUTHORIZE");
        final URI authorizeEndpoint = URI.create(oidcConfig.getAuthorizeEndpoint());

        final Map<String, String> requestParams = new HashMap<>();
        requestParams.put(HttpConstants.RESPONSE_TYPE, HttpConstants.CODE);
        requestParams.put(HttpConstants.REDIRECT_URI, appOrigin);
        requestParams.put(HttpConstants.SCOPE, HttpConstants.OPENID);
        requestParams.put(HttpConstants.CLIENT_ID, clientId);
        final String authorizeUrl = requestParams.keySet().stream()
                .map(key -> key + "=" + HttpUtils.encodeValue(requestParams.get(key)))
                .collect(Collectors.joining("&", authorizeEndpoint + "?", ""));
        URI redirectUrl = URI.create(authorizeUrl);

        HttpRequest request;
        do {
            logger.debug("Authorize URL {}", redirectUrl);
            request = HttpUtils.newRequestBuilder(redirectUrl).build();
            HttpUtils.logRequest(logger, request);
            final HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            HttpUtils.logResponse(logger, response);
            if (!HttpUtils.isRedirect(response.statusCode())) {
                throw new TestHarnessInitializationException("Authorization failed with status code " +
                        response.statusCode());
            }
            final Optional<String> locationHeader = response.headers().firstValue(HttpConstants.HEADER_LOCATION);
            redirectUrl = locationHeader.map(authorizeEndpoint::resolve).orElse(null);
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

    private Tokens requestToken(final Client authClient, final OidcConfiguration oidcConfig, final String clientId,
                                final String clientSecret, final Map<Object, Object> tokenRequestData
    ) throws IOException, InterruptedException {
        logger.debug("\n========== ACCESS TOKEN");
        final HttpRequest request = authClient.signRequest(
                HttpUtils.newRequestBuilder(URI.create(oidcConfig.getTokenEndpoint()))
                        .header(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.PREFIX_BASIC +
                                base64Encode(clientId + ':' + clientSecret))
                        .header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.MEDIA_TYPE_APPLICATION_FORM_URLENCODED)
                        .header(HttpConstants.HEADER_ACCEPT, HttpConstants.MEDIA_TYPE_APPLICATION_JSON)
                        .POST(HttpUtils.ofFormData(tokenRequestData))
        ).build();
        HttpUtils.logRequest(logger, request);
        final HttpResponse<String> response = authClient.send(request, HttpResponse.BodyHandlers.ofString());
        HttpUtils.logResponse(logger, response);
        final String body = response.body();
        if (response.statusCode() == HttpConstants.STATUS_OK) {
            return objectMapper.readValue(response.body(), Tokens.class);
        } else {
            logger.error("FAILED TO GET ACCESS TOKEN {}", body);
            throw new TestHarnessInitializationException("Token exchange failed for grant type: %s",
                    (String) tokenRequestData.get(HttpConstants.GRANT_TYPE));
        }
    }

    private String base64Encode(final String data) {
        return Base64.getEncoder().encodeToString(data.getBytes());
    }
}
