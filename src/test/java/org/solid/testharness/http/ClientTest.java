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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.TestUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(ClientResource.class)
class ClientTest {
    private static final URI TEST_URL = URI.create(TestUtils.SAMPLE_BASE);
    private static final HttpResponse.BodyHandler<String> STRING_BODY_HANDLER = HttpResponse.BodyHandlers.ofString();
    private URI baseUri;

    @BeforeEach
    void setup() {
        System.clearProperty("jdk.internal.httpclient.disableHostnameVerification");
    }

    @Test
    void buildSimple() {
        final Client client = new Client.Builder().build();
        assertEquals("Client: user=, dPoP=false, session=false, local=false", client.toString());
    }

    @Test
    void buildWithUser() {
        final Client client = new Client.Builder("user").build();
        assertEquals("Client: user=user, dPoP=false, session=false, local=false", client.toString());
    }

    @Test
    void buildWithRedirects() {
        final Client client = new Client.Builder("user").followRedirects().build();
        assertEquals(HttpClient.Redirect.NORMAL, client.getHttpClient().followRedirects());
    }

    @Test
    void buildWithSessionSupport() {
        final Client client = new Client.Builder("session").withSessionSupport().build();
        assertEquals("Client: user=session, dPoP=false, session=true, local=false", client.toString());
    }

    @Test
    void buildWithLocalhostSupport() {
        final Client client = new Client.Builder("local")
                .withOptionalLocalhostSupport(URI.create("http://server/")).build();
        assertEquals("Client: user=local, dPoP=false, session=false, local=true", client.toString());
    }

    @Test
    void buildWithoutLocalhostSupport() {
        final Client client = new Client.Builder("notlocal").withOptionalLocalhostSupport(TEST_URL).build();
        assertEquals("Client: user=notlocal, dPoP=false, session=false, local=false", client.toString());
    }

    @Test
    void buildWithDpopSupport() {
        final Client client = new Client.Builder("dpop").withDpopSupport().build();
        assertEquals("Client: user=dpop, dPoP=true, session=false, local=false", client.toString());
    }

    @Test
    void getHttpClient() {
        final Client client = new Client.Builder().build();
        assertNotNull(client.getHttpClient());
    }

    @Test
    void getSetAccessToken() {
        final Client client = new Client.Builder().build();
        assertNull(client.getAccessToken());
        client.setAccessToken("ACCESS");
        assertEquals("ACCESS", client.getAccessToken());
    }

    @Test
    void getUser() {
        final Client client = new Client.Builder("testuser").build();
        assertEquals("testuser", client.getUser());
    }

    @Test
    void sendRawEmpty() throws Exception {
        final Client client = new Client.Builder().build();
        final HttpResponse<String> response = client.send("DAHU", baseUri.resolve("/dahu/no-auth"),
                "TEXT", null, null, false);
        assertEquals(HttpClient.Version.HTTP_1_1, response.version());
        assertEquals(200, response.statusCode());
        assertEquals("", response.body());
    }

    @Test
    void sendRawAuthEmpty() throws Exception {
        final Client client = new Client.Builder().withDpopSupport().build();
        client.setAccessToken("ACCESS");
        final HttpResponse<String> response = client.send("DAHU", baseUri.resolve("/dahu/auth"),
                null, null, HttpClient.Version.HTTP_2.name(), true);
        assertEquals(HttpClient.Version.HTTP_1_1, response.version());
        assertEquals(200, response.statusCode());
        assertEquals("AUTHENTICATED", response.body());
        assertEquals(HttpConstants.MEDIA_TYPE_TEXT_PLAIN,
                response.headers().firstValue(HttpConstants.HEADER_CONTENT_TYPE).orElse(null));
    }

    @Test
    void sendRawRetry() throws Exception {
        final Client client = new Client.Builder().build();
        final HttpResponse<String> response = client.send("RETRY", baseUri.resolve("/retry"),
                "TEXT", null, null, false);
        assertEquals(HttpClient.Version.HTTP_1_1, response.version());
        assertEquals(405, response.statusCode());
        assertEquals("", response.body());
    }

    @Test
    void sendRawRetryFails() {
        final Client client = new Client.Builder().build();
        client.setMaxRetries(1);
        assertThrows(ExecutionException.class, () -> client.send("RETRY", baseUri.resolve("/retryfails"),
                "TEXT", null, null, false));
    }

    @Test
    void sendRawNullMethod() {
        final Client client = new Client.Builder().build();
        assertThrows(NullPointerException.class, () -> client.send(null, baseUri, null, null, null, false));
    }

    @Test
    void sendRawNullUri() {
        final Client client = new Client.Builder().build();
        assertThrows(NullPointerException.class, () -> client.send("GET", null, null, null, null, false));
    }

    @Test
    void sendRawWithHeaders() throws Exception {
        final Client client = new Client.Builder().build();
        final Map<String, Object> headers = Map.of(
                "INT", 5,
                "FLOAT", 2.5f,
                "STRING", "HEADER",
                "LIST", List.of("ITEM1", "ITEM2"),
                "SKIP", TEST_URL
        );
        final HttpResponse<String> response = client.send("DAHU", baseUri.resolve("/dahu/headers"),
                null, headers, null, false);
        assertEquals(200, response.statusCode());
    }

    @Test
    void send() throws Exception {
        final Client client = new Client.Builder().build();
        final HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/get/404")).build();
        final HttpResponse<String> response = client.send(request, STRING_BODY_HANDLER);
        assertEquals(404, response.statusCode());
    }

    @Test
    void sendNullRequest() {
        final Client client = new Client.Builder().build();
        assertThrows(NullPointerException.class, () -> client.send(null, STRING_BODY_HANDLER));
    }

    @Test
    void sendNullHandler() {
        final Client client = new Client.Builder().build();
        final HttpRequest request = HttpRequest.newBuilder(baseUri).build();
        assertThrows(NullPointerException.class, () -> client.send(request, null));
    }

    @Test
    void sendFail() {
        final Client client = new Client.Builder().build();
        final HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/get/fault")).build();
        assertThrows(Exception.class, () -> client.send(request, STRING_BODY_HANDLER));
    }

    @Test
    void sendAuthorized() throws Exception {
        final Client client = new Client.Builder().withDpopSupport().build();
        client.setAccessToken("ACCESS");
        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(baseUri.resolve("/get/turtle"));
        final HttpResponse<String> response = client.sendAuthorized(null, requestBuilder, STRING_BODY_HANDLER);
        final Map<String, List<String>> headers = response.request().headers().map();
        assertTrue(headers.containsKey(HttpConstants.HEADER_AUTHORIZATION));
        assertTrue(headers.get(HttpConstants.HEADER_AUTHORIZATION).get(0).startsWith(HttpConstants.PREFIX_DPOP));
        assertTrue(headers.containsKey(HttpConstants.HEADER_DPOP));
    }

    @Test
    void sendAuthorizedNullRequestBuilder() {
        final Client client = new Client.Builder().build();
        assertThrows(NullPointerException.class, () -> client.sendAuthorized(null, null, STRING_BODY_HANDLER));
    }

    @Test
    void sendAuthorizedNullHandler() {
        final Client client = new Client.Builder().build();
        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(baseUri);
        assertThrows(NullPointerException.class, () -> client.sendAuthorized(null, requestBuilder, null));
    }

    @Test
    void sendAuthorizedFail() {
        final Client client = new Client.Builder().build();
        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(baseUri.resolve("/get/fault"));
        assertThrows(Exception.class,() -> client.sendAuthorized(null, requestBuilder, STRING_BODY_HANDLER));
    }

    @Test
    void getAsTurtleNoAuth() throws Exception {
        final Client client = new Client.Builder().build();
        final HttpResponse<String> response = client.getAsTurtle(baseUri.resolve("/get/turtle"));
        assertEquals("TURTLE-NOAUTH", response.body());
    }

    @Test
    void getAsTurtleDpop() throws Exception {
        final Client client = new Client.Builder().withDpopSupport().build();
        client.setAccessToken("ACCESS");
        final HttpResponse<String> response = client.getAsTurtle(baseUri.resolve("/get/turtle"));
        assertEquals("TURTLE-DPOP", response.body());
    }

    @Test
    void getAsTurtleBearer() throws Exception {
        final Client client = new Client.Builder().build();
        client.setAccessToken("ACCESS");
        final HttpResponse<String> response = client.getAsTurtle(baseUri.resolve("/get/turtle"));
        assertEquals("TURTLE-BEARER", response.body());
    }

    @Test
    void getAsTurtleNull() {
        final Client client = new Client.Builder().build();
        assertThrows(NullPointerException.class, () -> client.getAsTurtle(null));
    }

    @Test
    void patch() throws Exception {
        final Client client = new Client.Builder().build();
        final HttpResponse<String> response = client.patch(baseUri.resolve("/patch"), "TEXT",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertEquals("PATCHED", response.body());
    }

    @Test
    void putText() throws Exception {
        final Client client = new Client.Builder().build();
        final HttpResponse<Void> response = client.put(baseUri.resolve("/put"), "TEXT",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertEquals(200, response.statusCode());
    }

    @Test
    void putTurtle() throws Exception {
        final Client client = new Client.Builder().build();
        final HttpResponse<Void> response = client.put(baseUri.resolve("/put"), "TURTLE",
                HttpConstants.MEDIA_TYPE_TEXT_TURTLE);
        assertEquals(201, response.statusCode());
    }

    @Test
    void putNullUrl() {
        final Client client = new Client.Builder().build();
        assertThrows(NullPointerException.class, () -> client.put(null, "", ""));
    }

    @Test
    void putNullData() {
        final Client client = new Client.Builder().build();
        assertThrows(NullPointerException.class, () -> client.put(baseUri, null, ""));
    }

    @Test
    void putNullType() {
        final Client client = new Client.Builder().build();
        assertThrows(NullPointerException.class, () -> client.put(baseUri, "DATA", null));
    }

    @Test
    void head() throws Exception {
        final Client client = new Client.Builder().build();
        final HttpResponse<Void> response = client.head(baseUri.resolve("/head"));
        assertEquals(200, response.statusCode());
        assertEquals(2, response.headers().allValues("HEADER").size());
        assertEquals("VALUE1", response.headers().allValues("HEADER").get(0));
    }

    @Test
    void headNullUrl() {
        final Client client = new Client.Builder().build();
        assertThrows(NullPointerException.class, () -> client.head(null));
    }

    @Test
    void deleteAsync() throws ExecutionException, InterruptedException {
        final Client client = new Client.Builder().build();
        final CompletableFuture<HttpResponse<Void>> futureResponse = client.deleteAsync(baseUri.resolve("/delete"));
        assertNotNull(futureResponse);
        final HttpResponse<Void> response = futureResponse.get();
        assertEquals(204, response.statusCode());
    }

    @Test
    void deleteAsyncNull() {
        final Client client = new Client.Builder().build();
        assertThrows(NullPointerException.class, () -> client.deleteAsync(null));
    }

    @Test
    void signRequestNoDpop() {
        final Client client = new Client.Builder().build();
        final HttpRequest.Builder builder = HttpRequest.newBuilder(TEST_URL);
        final HttpRequest request = client.signRequest(builder).build();
        assertFalse(request.headers().map().containsKey(HttpConstants.HEADER_DPOP));
    }

    @Test
    void signRequest() {
        final Client client = new Client.Builder().withDpopSupport().build();
        final HttpRequest.Builder builder = HttpRequest.newBuilder(TEST_URL);
        final HttpRequest request = client.signRequest(builder).build();
        assertTrue(request.headers().map().containsKey(HttpConstants.HEADER_DPOP));
    }

    @Test
    void signRequestNull() {
        final Client client = new Client.Builder().build();
        assertThrows(NullPointerException.class, () -> client.signRequest(null));
    }

    @Test
    void getAuthHeadersNoAccessToken() {
        final Client client = new Client.Builder().build();
        assertTrue(client.getAuthHeaders("GET", TEST_URL).isEmpty());
    }

    @Test
    void getAuthHeadersNoDpop() {
        final Client client = new Client.Builder().build();
        client.setAccessToken("ACCESS");
        final Map<String, String> headers = client.getAuthHeaders("GET", TEST_URL);
        assertTrue(headers.containsKey(HttpConstants.HEADER_AUTHORIZATION));
        assertTrue(headers.get(HttpConstants.HEADER_AUTHORIZATION).startsWith(HttpConstants.PREFIX_BEARER));
        assertTrue(headers.containsKey(HttpConstants.USER_AGENT));
    }

    @Test
    void getAuthHeadersDpop() {
        final Client client = new Client.Builder().withDpopSupport().build();
        client.setAccessToken("ACCESS");
        final Map<String, String> headers = client.getAuthHeaders("GET", TEST_URL);
        assertTrue(headers.containsKey(HttpConstants.HEADER_AUTHORIZATION));
        assertTrue(headers.get(HttpConstants.HEADER_AUTHORIZATION).startsWith(HttpConstants.PREFIX_DPOP));
        assertTrue(headers.containsKey(HttpConstants.HEADER_DPOP));
        assertTrue(headers.containsKey(HttpConstants.USER_AGENT));
    }

    @Test
    void getAuthHeadersNullMethod() {
        final Client client = new Client.Builder().build();
        assertThrows(NullPointerException.class, () -> client.getAuthHeaders(null, TEST_URL));
    }

    @Test
    void getAuthHeadersNullUri() {
        final Client client = new Client.Builder().build();
        assertThrows(NullPointerException.class, () -> client.getAuthHeaders("GET", null));
    }

    public void setBaseUri(final URI baseUri) {
        this.baseUri = baseUri;
    }
}
