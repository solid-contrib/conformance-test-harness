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
import org.jose4j.lang.JoseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.TestUtils;

import java.io.IOException;
import java.net.URI;
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
    void buildWithSessionSupport() {
        final Client client = new Client.Builder("session").withSessionSupport().build();
        assertEquals("Client: user=session, dPoP=false, session=true, local=false", client.toString());
    }

    @Test
    void buildWithLocalhostSUpport() {
        final Client client = new Client.Builder("local").withLocalhostSupport().build();
        assertEquals("Client: user=local, dPoP=false, session=false, local=true", client.toString());
    }

    @Test
    void buildWithDpopSupport() throws Exception {
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
    void send() throws IOException, InterruptedException {
        final Client client = new Client.Builder().build();
        final HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/get/404")).build();
        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

    @Test
    void sendNullRequest() {
        final Client client = new Client.Builder().build();
        assertThrows(NullPointerException.class, () -> client.send(null, HttpResponse.BodyHandlers.ofString()));
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
        assertThrows(IOException.class, () -> client.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void sendAuthorized() throws IOException, InterruptedException, JoseException {
        final Client client = new Client.Builder().withDpopSupport().build();
        client.setAccessToken("ACCESS");
        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(TestUtils.SAMPLE_BASE));
        final HttpResponse<String> response = client.sendAuthorized(requestBuilder,
                HttpResponse.BodyHandlers.ofString());
        final Map<String, List<String>> headers = response.request().headers().map();
        assertTrue(headers.containsKey(HttpConstants.HEADER_AUTHORIZATION));
        assertTrue(headers.get(HttpConstants.HEADER_AUTHORIZATION).get(0).startsWith(HttpConstants.PREFIX_DPOP));
        assertTrue(headers.containsKey(HttpConstants.HEADER_DPOP));
    }

    @Test
    void sendAuthorizedNullRequestBuilder() {
        final Client client = new Client.Builder().build();
        assertThrows(NullPointerException.class,
                () -> client.sendAuthorized(null, HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void sendAuthorizedNullHandler() {
        final Client client = new Client.Builder().build();
        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(baseUri);
        assertThrows(NullPointerException.class, () -> client.sendAuthorized(requestBuilder, null));
    }

    @Test
    void sendAuthorizedFail() {
        final Client client = new Client.Builder().build();
        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(baseUri.resolve("/get/fault"));
        assertThrows(IOException.class,
                () -> client.sendAuthorized(requestBuilder, HttpResponse.BodyHandlers.ofString()));
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
    void head() throws IOException, InterruptedException {
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
        final HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(TestUtils.SAMPLE_BASE));
        final HttpRequest request = client.signRequest(builder).build();
        assertFalse(request.headers().map().containsKey(HttpConstants.HEADER_DPOP));
    }

    @Test
    void signRequest() throws JoseException {
        final Client client = new Client.Builder().withDpopSupport().build();
        final HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(TestUtils.SAMPLE_BASE));
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
    void getAuthHeadersDpop() throws JoseException {
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
