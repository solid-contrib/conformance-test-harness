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

import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;

@SuppressWarnings({"checkstyle:FinalClass", "PMD.AvoidDuplicateLiterals"})
// Class is not final because it needs mocking for tests
// Duplicate string are null check error messages for same parameter
public class Client {
    private static final Logger logger = LoggerFactory.getLogger(Client.class);
    private static final List<String> TRUSTED_HOSTS = List.of("localhost", "server");
    private static final int MAX_RETRY = 10;

    private HttpClient httpClient;
    private String accessToken;
    private RsaJsonWebKey clientKey;
    private boolean dpopSupported;
    private String agent;
    private String user;
    private int maxRetries = MAX_RETRY;

    public static class Builder {
        private final HttpClient.Builder clientBuilder;
        private final String user;
        private RsaJsonWebKey clientKey;

        public Builder() {
            this("");
        }
        public Builder(final String user) {
            this.user = user;
            clientBuilder = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(HttpUtils.getConnectTimeout());
        }

        public Builder followRedirects() {
            clientBuilder.followRedirects(HttpClient.Redirect.NORMAL);
            return this;
        }

        public Builder withSessionSupport() {
            CookieHandler.setDefault(new CookieManager());
            clientBuilder.cookieHandler(CookieHandler.getDefault());
            return this;
        }

        public Builder withOptionalLocalhostSupport(final URI uri) {
            requireNonNull(uri, "uri is required to check localhost support");
            if (TRUSTED_HOSTS.contains(uri.getHost())) {
                System.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
                clientBuilder.sslContext(LocalHostSupport.createSSLContext());
            }
            return this;
        }

        public Builder withDpopSupport() {
            clientKey = JwsUtils.createClientKey();
            return this;
        }

        public Client build() {
            final Client client = new Client();
            client.agent = HttpUtils.getAgent();
            client.user = user;
            client.httpClient = clientBuilder.build();
            client.clientKey = clientKey;
            client.dpopSupported = clientKey != null;
            return client;
        }
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setAccessToken(final String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getUser() {
        return user;
    }

    public void setMaxRetries(final int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * Build an HTTP request with authorization if required and send it with retry capability.
     * Used by SolidClient .
     * @param method      HTTP method
     * @param url         Target URL
     * @param data        Request body
     * @param headers     Header map
     * @param version     HTTP version 1.1 or 2
     * @param authorized  Is authorization needed
     * @return Response
     * @throws Exception any exception
     */
    @SuppressWarnings("checkstyle:MultipleStringLiterals") // cleaner to leave error messages local
    public HttpResponse<String> send(@NotNull final String method, @NotNull final URI url, final String data,
                                     final Map<String, Object> headers, final String version, final boolean authorized)
            throws ExecutionException, InterruptedException {
        requireNonNull(url, "url is required for send");
        requireNonNull(method, "method is required for send");
        // build HTTP request
        final HttpRequest.Builder builder = HttpUtils.newRequestBuilder(url);
        if (data != null) {
            builder.method(method, HttpRequest.BodyPublishers.ofString(data));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        if (version != null) {
            builder.version(HttpClient.Version.valueOf(version));
        }
        // add headers to the request
        if (headers != null) {
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                if (entry.getValue() instanceof String || entry.getValue() instanceof Number) {
                    builder.header(entry.getKey(), String.valueOf(entry.getValue()));
                } else if (entry.getValue() instanceof Collection) {
                    ((Collection)entry.getValue()).forEach(o -> builder.header(entry.getKey(), String.valueOf(o)));
                }
            }
        }
        // add authorization headers if required and complete the request building
        final HttpRequest request = authorized ? authorize(builder).build() : builder.build();
        final BodyHandler<String> handler = BodyHandlers.ofString();

        // send the request with retries
        HttpUtils.logRequestToKarate(logger, request, data);
        final HttpResponse<String> response = send(request, handler);
        HttpUtils.logResponseToKarate(logger, response);
        return response;
    }

    public <T> HttpResponse<T> send(@NotNull final HttpRequest request,
                                    @NotNull final BodyHandler<T> responseHandler)
            throws ExecutionException, InterruptedException {
        requireNonNull(request, "request is required");
        requireNonNull(responseHandler, "responseHandler is required");
        final CompletableFuture<HttpResponse<T>> responseFuture = sendAsync(request, responseHandler);
        return responseFuture.get();
    }

    public <T> HttpResponse<T> sendAuthorized(final String data, @NotNull final HttpRequest.Builder requestBuilder,
                                              @NotNull final BodyHandler<T> responseHandler)
            throws ExecutionException, InterruptedException {
        requireNonNull(requestBuilder, "requestBuilder is required");
        requireNonNull(responseHandler, "responseHandler is required");
        final HttpRequest request = authorize(requestBuilder).build();
        HttpUtils.logRequestToKarate(logger, request, data);
        final HttpResponse<T> response = send(request, responseHandler);
        HttpUtils.logResponseToKarate(logger, response);
        return response;
    }

    @SuppressWarnings("checkstyle:MultipleStringLiterals")
    public HttpResponse<String> getAsTurtle(@NotNull final URI url) throws InterruptedException, ExecutionException {
        requireNonNull(url, "url is required for getAsTurtle");
        final HttpRequest.Builder builder = HttpUtils.newRequestBuilder(url)
                .header(HttpConstants.HEADER_ACCEPT, HttpConstants.MEDIA_TYPE_TEXT_TURTLE);
        final HttpRequest request = authorize(builder).build();
        return send(request, BodyHandlers.ofString());
    }

    public HttpResponse<Void> put(@NotNull final URI url, final String data, final String type)
            throws ExecutionException, InterruptedException {
        requireNonNull(url, "url is required for put");
        requireNonNull(data, "data is required for put");
        requireNonNull(type, "type is required for put");
        final HttpRequest.Builder builder = HttpUtils.newRequestBuilder(url)
                .PUT(HttpRequest.BodyPublishers.ofString(data))
                .header(HttpConstants.HEADER_CONTENT_TYPE, type);
        return sendAuthorized(data, builder, BodyHandlers.discarding());
    }

    public HttpResponse<String> patch(@NotNull final URI url, final String data, final String type)
            throws InterruptedException, ExecutionException {
        requireNonNull(url, "url is required for patch");
        requireNonNull(data, "data is required for patch");
        requireNonNull(type, "type is required for patch");
        final HttpRequest.Builder builder = HttpUtils.newRequestBuilder(url)
                .method(HttpConstants.METHOD_PATCH, HttpRequest.BodyPublishers.ofString(data))
                .header(HttpConstants.HEADER_CONTENT_TYPE, type);
        return sendAuthorized(data, builder, BodyHandlers.ofString());
    }

    public HttpResponse<Void> head(@NotNull final URI url) throws InterruptedException, ExecutionException {
        requireNonNull(url, "url is required for head");
        final HttpRequest.Builder builder = HttpUtils.newRequestBuilder(url)
                .method(HttpConstants.METHOD_HEAD, HttpRequest.BodyPublishers.noBody());
        return sendAuthorized(null, builder, BodyHandlers.discarding());
    }

    public CompletableFuture<HttpResponse<Void>> deleteAsync(@NotNull final URI url) {
        requireNonNull(url, "url is required for deleteAsync");
        logger.debug("Deleting {}", url);
        final HttpRequest.Builder builder = HttpUtils.newRequestBuilder(url).DELETE();
        final HttpRequest request = authorize(builder).build();
        return sendAsync(request, BodyHandlers.discarding());
    }

    private <T> CompletableFuture<HttpResponse<T>> sendAsync(@NotNull final HttpRequest request,
                                                             @NotNull final BodyHandler<T> responseHandler) {
        requireNonNull(request, "request is required");
        requireNonNull(responseHandler, "responseHandler is required");
        return httpClient.sendAsync(request, responseHandler)
                .handleAsync((r, t) -> tryResend(httpClient, request, responseHandler, 1, r, t))
                .thenCompose(Function.identity());
    }

    // Retry on timeout exception
    private boolean shouldRetry(final HttpResponse<?> r, final Throwable t, final int count) {
        if (r != null || count >= maxRetries) return false;
        if (!(t.getCause() instanceof HttpTimeoutException)) return false;
        HttpUtils.logToKarate(logger, "Retry ({}) due to {}", count, t.toString());
        return true;
    }

    private <T> CompletableFuture<HttpResponse<T>> tryResend(final HttpClient client, final HttpRequest request,
                                                             final BodyHandler<T> handler,
                                                             final int count, final HttpResponse<T> resp,
                                                             final Throwable t) {
        if (shouldRetry(resp, t, count)) {
            return client.sendAsync(request, handler)
                    .handleAsync((r, x) -> tryResend(client, request, handler, count + 1, r, x))
                    .thenCompose(Function.identity());
        } else if (t != null) {
            return CompletableFuture.failedFuture(t);
        } else {
            return CompletableFuture.completedFuture(resp);
        }
    }

    /**
     * Sign a request using a DPoP token.
     * @param builder Request builder containing request to be signed
     * @return The builder with the DPoP header added
     */
    public HttpRequest.Builder signRequest(@NotNull final HttpRequest.Builder builder) {
        requireNonNull(builder, "builder is required");
        if (!dpopSupported) return builder;
        final HttpRequest provisionalRequest = builder.copy().build();
        final String dpopToken = generateDpopToken(provisionalRequest.method(), provisionalRequest.uri().toString());
        return builder.header(HttpConstants.HEADER_DPOP, dpopToken);
    }

    /**
     * Return a map of authentication headers depending on the client's authentication options. This is normally used
     * within Karate test features to add headers to a request.
     * @param method HTTP method of request
     * @param uri URI of request
     * @return Map of authentication and agent headers
     */
    public Map<String, String> getAuthHeaders(@NotNull final String method, @NotNull final URI uri) {
        requireNonNull(method, "method is required for getAuthHeaders");
        requireNonNull(uri, "uri is required for getAuthHeaders");
        final Map<String, String> headers = new HashMap<>();
        if (accessToken == null) return headers;
        if (dpopSupported) {
            headers.put(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.PREFIX_DPOP + accessToken);
            final String dpopToken = generateDpopToken(method, uri.toString());
            headers.put(HttpConstants.HEADER_DPOP, dpopToken);
        } else {
            headers.put(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.PREFIX_BEARER + accessToken);
        }
        headers.put(HttpConstants.USER_AGENT, agent);
        return headers;
    }

    /**
     * Sign and add authentication headers to a request.
     * @param builder Request builder containing request to be signed
     * @return The builder with the Authorization and DPoP headers added
     */
    private HttpRequest.Builder authorize(@NotNull final HttpRequest.Builder builder) {
        requireNonNull(builder, "builder is required");
        if (accessToken == null) return builder;
        if (dpopSupported) {
            builder.setHeader(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.PREFIX_DPOP + accessToken);
            return signRequest(builder);
        } else {
            return builder.setHeader(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.PREFIX_BEARER + accessToken);
        }
    }

    private String generateDpopToken(final String htm, final String htu) {
        requireNonNull(clientKey, "This instance does not have DPoP support added");
        final JwtClaims claims = new JwtClaims();
        claims.setJwtId(randomUUID().toString());
        claims.setStringClaim("htm", htm);
        claims.setStringClaim("htu", htu);
        claims.setIssuedAtToNow();
        return JwsUtils.generateDpopToken(clientKey, claims);
    }

    @Override
    public String toString() {
        return String.format("Client: user=%s, dPoP=%s, session=%s, local=%s",
                user, dpopSupported, httpClient.cookieHandler().isPresent(),
                System.getProperty("jdk.internal.httpclient.disableHostnameVerification", "false"));
    }

    private Client() { }
}
