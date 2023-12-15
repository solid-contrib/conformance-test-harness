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

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.common.vocab.ACP;
import org.solid.common.vocab.PIM;
import org.solid.testharness.accesscontrol.AccessDataset;
import org.solid.testharness.api.TestHarnessApiException;
import org.solid.testharness.config.Config;
import org.solid.testharness.config.TestSubject;
import org.solid.testharness.utils.TestHarnessException;
import org.solid.testharness.utils.TestUtils;

import jakarta.inject.Inject;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class SolidClientProviderTest {
    private static final URI BASE_URL = URI.create("https://example.org/");
    private static final URI TEST_URL = BASE_URL.resolve("/test");
    private static final String ENTRY = "<%s> <" + LDP.CONTAINS + "> <%s>.";

    @Inject
    ClientRegistry clientRegistry;
    @InjectMock
    Config config;
    @InjectMock
    TestSubject testSubject;

    @BeforeEach
    void setup() {
        when(config.getReadTimeout()).thenReturn(5000);
        when(config.getAgent()).thenReturn("AGENT");
    }

    @Test
    void createDefaultClient() {
        final SolidClientProvider solidClientProvider = new SolidClientProvider();
        final Client client = solidClientProvider.getClient();
        assertNotNull(client.getHttpClient());
        assertEquals("", client.getUser());
        assertNull(client.getAccessToken());
    }

    @Test
    void createMissingNamedClient() {
        when(config.getWebIds()).thenReturn(Map.of(HttpConstants.ALICE,
                "https://alice.target.example.org/profile/card#me"));
        assertThrows(TestHarnessException.class, () -> new SolidClientProvider("nobody"));
    }

    @Test
    void createWithExistingClient() {
        final Client newClient = new Client.Builder("newuser").build();
        final SolidClientProvider solidClientProvider = new SolidClientProvider(newClient);
        final Client client = solidClientProvider.getClient();
        assertNotNull(client.getHttpClient());
        assertEquals("newuser", client.getUser());
        assertNull(client.getAccessToken());
    }

    @Test
    void createNamedClient() throws Exception {
        clientRegistry.register("user1", new Client.Builder("user1").build());
        final SolidClientProvider solidClientProvider = new SolidClientProvider("user1");
        final Client client = solidClientProvider.getClient();
        assertNotNull(client.getHttpClient());
        assertEquals("user1", client.getUser());
        assertNull(client.getAccessToken());
    }

    @Test
    void createStatically() throws Exception {
        clientRegistry.register("user2", new Client.Builder("user2").build());
        final SolidClientProvider solidClientProvider = SolidClientProvider.create("user2");
        final Client client = solidClientProvider.getClient();
        assertNotNull(client.getHttpClient());
        assertEquals("user2", client.getUser());
        assertNull(client.getAccessToken());
    }

    @Test
    void createResource() throws Exception {
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponse = TestUtils.mockVoidResponse(200);
        when(mockClient.put(TEST_URL, "DATA", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(mockResponse);

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        final HttpHeaders headers = solidClientProvider.createResource(TEST_URL,
                "DATA", HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertTrue(headers.map().isEmpty());
        verify(mockClient).put(TEST_URL, "DATA", HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
    }

    @Test
    void createResourceFails() {
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponse = TestUtils.mockVoidResponse(412);
        when(mockClient.put(TEST_URL, "DATA", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(mockResponse);

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        final Exception exception = assertThrows(Exception.class,
                () -> solidClientProvider.createResource(TEST_URL, "DATA",
                        HttpConstants.MEDIA_TYPE_TEXT_PLAIN)
        );
        assertEquals("Failed to create " + TEST_URL + ", response=412", exception.getMessage());
    }

    @Test
    void createContainer() throws Exception {
        final Client mockClient = mock(Client.class);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(201, "");
        doReturn(mockResponse).when(mockClient).sendAuthorized(eq(null), any(), any());

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        final HttpHeaders headers = solidClientProvider.createContainer(BASE_URL.resolve("/test/"));
        assertTrue(headers.map().isEmpty());
    }

    @Test
    void createContainerFails() {
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(412);
        doReturn(mockResponse).when(mockClient).sendAuthorized(eq(null), any(), any());

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        final Exception exception = assertThrows(Exception.class,
                () -> solidClientProvider.createContainer(BASE_URL.resolve("/test/"))
        );
        assertEquals("Failed to create " + BASE_URL.resolve("/test/") + ", response=412", exception.getMessage());
    }

    @Test
    void getAclUriFromUri() {
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponse = TestUtils.mockVoidResponse(204, Map.of(HttpConstants.HEADER_LINK,
                List.of("<" + BASE_URL.resolve("test.acl") + ">; rel=\"acl\"")));
        when(mockClient.head(any())).thenReturn(mockResponse);

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        final URI uri = solidClientProvider.getAclUri(TEST_URL);
        assertEquals(BASE_URL.resolve("/test.acl"), uri);
        verify(mockClient).head(TEST_URL);
    }

    @Test
    void getAclUriFails() {
        final Client mockClient = mock(Client.class);
        when(mockClient.head(any())).thenThrow(TestUtils.createException("Failed"));

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        assertThrows(Exception.class, () -> solidClientProvider.getAclUri(TEST_URL));
    }

    @Test
    void getAclUriFromHeadersWAC() {
        when(config.getWebIds()).thenReturn(Map.of(HttpConstants.ALICE,
                "https://alice.target.example.org/profile/card#me"));
        final Map<String, List<String>> headerMap = Map.of("Link",
                List.of("<" + BASE_URL.resolve("test.acl") + ">; rel=\"acl\""));
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);

        final SolidClientProvider solidClientProvider = new SolidClientProvider();
        final URI uri = solidClientProvider.getAclUri(headers);
        assertEquals(BASE_URL.resolve("/test.acl"), uri);
    }

    @Test
    void getAclUriFromHeadersACP() {
        when(config.getWebIds()).thenReturn(Map.of(HttpConstants.ALICE,
                "https://alice.target.example.org/profile/card#me"));
        final Map<String, List<String>> headerMap = Map.of("Link",
                List.of("<" + BASE_URL.resolve("test?ext=acr") +
                        ">; rel=\"http://www.w3.org/ns/solid/acp#accessControl\""));
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);

        final SolidClientProvider solidClientProvider = new SolidClientProvider();
        final URI uri = solidClientProvider.getAclUri(headers);
        assertEquals(BASE_URL.resolve("/test?ext=acr"), uri);
    }

    @Test
    void getAclUriFromHeadersWrongLink() {
        final Map<String, List<String>> headerMap = Map.of("Link",
                List.of("<" + BASE_URL.resolve("test?ext=acr") + ">; rel=\"https://example.org\""));
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);

        final SolidClientProvider solidClientProvider = new SolidClientProvider();
        assertNull(solidClientProvider.getAclUri(headers));
    }

    @Test
    void getAclTypeWAC() {
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponse = TestUtils.mockVoidResponse(204);
        when(mockClient.head(any())).thenReturn(mockResponse);

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        final TestSubject.AccessControlMode mode = solidClientProvider.getAclType(BASE_URL.resolve("/test.acl"));
        assertEquals(TestSubject.AccessControlMode.WAC, mode);
    }

    @Test
    void getAclTypeACP() {
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponse = TestUtils.mockVoidResponse(204, Map.of(HttpConstants.HEADER_LINK,
                List.of("<" + ACP.AccessControlResource.toString() + ">; rel=\"type\"")));
        when(mockClient.head(any())).thenReturn(mockResponse);

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        final TestSubject.AccessControlMode mode = solidClientProvider.getAclType(BASE_URL.resolve("/test.acl"));
        assertEquals(TestSubject.AccessControlMode.ACP, mode);
    }

    @Test
    void getAclTypeWrongRel() {
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponse = TestUtils.mockVoidResponse(204, Map.of(HttpConstants.HEADER_LINK,
                List.of("<" + ACP.AccessControlResource.toString() + ">; rel=\"xxxx\"")));
        when(mockClient.head(any())).thenReturn(mockResponse);

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        final TestSubject.AccessControlMode mode = solidClientProvider.getAclType(BASE_URL.resolve("/test.acl"));
        assertEquals(TestSubject.AccessControlMode.WAC, mode);
    }

    @Test
    void getAclTypeWrongUrl() {
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponse = TestUtils.mockVoidResponse(204, Map.of(HttpConstants.HEADER_LINK,
                List.of("<https://example.org>; rel=\"type\"")));
        when(mockClient.head(any())).thenReturn(mockResponse);

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        final TestSubject.AccessControlMode mode = solidClientProvider.getAclType(BASE_URL.resolve("/test.acl"));
        assertEquals(TestSubject.AccessControlMode.WAC, mode);
    }

    @Test
    void getAclUriMissing() {
        final HttpHeaders headers = HttpHeaders.of(Collections.emptyMap(), (k, v) -> true);

        final SolidClientProvider solidClientProvider = new SolidClientProvider();
        assertNull(solidClientProvider.getAclUri(headers));
    }

    @Test
    void getAclUriMultiple() {
        final Map<String, List<String>> headerMap = Map.of(HttpConstants.HEADER_LINK, List.of(
                "<" + BASE_URL.resolve("test.acl") + ">; rel=\"acl\"",
                "<" + BASE_URL.resolve("test.acl2") + ">; rel=\"acl\""
                ));
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);

        final SolidClientProvider solidClientProvider = new SolidClientProvider();
        final URI uri = solidClientProvider.getAclUri(headers);
        assertEquals(BASE_URL.resolve("/test.acl"), uri);
    }

    @Test
    void getAcl() {
        final URI resourceAcl = BASE_URL.resolve("/test.acl");
        final Client mockClient = mock(Client.class);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "");
        when(mockClient.getAsTurtle(any())).thenReturn(mockResponse);
        when(testSubject.getAccessControlMode()).thenReturn(TestSubject.AccessControlMode.WAC);

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        final AccessDataset accessDataset = solidClientProvider.getAcl(resourceAcl);
        assertNotNull(accessDataset);
    }

    @Test
    void getAclFails() {
        final URI resourceAcl = BASE_URL.resolve("/test.acl");
        final Client mockClient = mock(Client.class);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(404, "");
        when(mockClient.getAsTurtle(any())).thenReturn(mockResponse);

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        final AccessDataset accessDataset = solidClientProvider.getAcl(resourceAcl);
        assertNull(accessDataset);
    }

    @Test
    void getAccessDatasetBuilder() {
        final Client mockClient = mock(Client.class);
        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        when(testSubject.getAccessControlMode()).thenReturn(TestSubject.AccessControlMode.WAC);
        assertNotNull(solidClientProvider.getAccessDatasetBuilder(BASE_URL.resolve("/test.acl")));
    }

    @Test
    void hasStorageTypeFalse() {
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponse = TestUtils.mockVoidResponse(204);
        when(mockClient.head(any())).thenReturn(mockResponse);

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        assertFalse(solidClientProvider.hasStorageType(BASE_URL));
    }

    @Test
    void hasStorageTypeTrue() {
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponse = TestUtils.mockVoidResponse(204, Map.of(HttpConstants.HEADER_LINK,
                List.of("<" + PIM.Storage.toString() + ">; rel=\"type\"")));
        when(mockClient.head(any())).thenReturn(mockResponse);

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        assertTrue(solidClientProvider.hasStorageType(BASE_URL));
    }


    @Test
    void createAcl() {
        final AccessDataset accessDataset = mock(AccessDataset.class);
        final Client mockClient = mock(Client.class);
        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        assertDoesNotThrow(() -> solidClientProvider.createAcl(BASE_URL.resolve("/test.acl"), accessDataset));
    }

    @Test
    void createAclFails() {
        final AccessDataset accessDataset = mock(AccessDataset.class);
        doThrow(new TestHarnessApiException("FAIL")).when(accessDataset).apply(any(), any());
        final Client mockClient = mock(Client.class);
        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        assertThrows(TestHarnessApiException.class,
                () -> solidClientProvider.createAcl(BASE_URL.resolve("/test.acl"), accessDataset));
    }

    @Test
    void getContainmentData() throws Exception {
        final Client mockClient = mock(Client.class);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "TEST");

        when(mockClient.getAsTurtle(TEST_URL)).thenReturn(mockResponse);

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        assertEquals("TEST", solidClientProvider.getContentAsTurtle(TEST_URL));
        verify(mockClient).getAsTurtle(TEST_URL);
    }

    @Test
    void getContainmentDataFails() {
        final Client mockClient = mock(Client.class);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(400, null);

        when(mockClient.getAsTurtle(TEST_URL)).thenReturn(mockResponse);

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        final Exception exception = assertThrows(Exception.class,
                () -> solidClientProvider.getContentAsTurtle(TEST_URL));
        assertEquals("Error response=400 trying to get content for " + TEST_URL,
                exception.getMessage());
    }

    @Test
    void getContainmentDataModel() throws Exception {
        final Client mockClient = mock(Client.class);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200,
                TestUtils.loadStringFromFile("src/test/resources/turtle-sample.ttl"));

        when(mockClient.getAsTurtle(TEST_URL)).thenReturn(mockResponse);

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        final Model model = solidClientProvider.getContentAsModel(TEST_URL);
        assertEquals(1, model.size());
        verify(mockClient).getAsTurtle(TEST_URL);
    }

    @Test
    void deleteResource() {
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponseOk = TestUtils.mockVoidResponse(204);

        when(mockClient.deleteAsync(TEST_URL))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        assertDoesNotThrow(() -> solidClientProvider.deleteResourceRecursively(TEST_URL));
        verify(mockClient).deleteAsync(TEST_URL);
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void deleteContents() {
        final String data = turtleList(BASE_URL, BASE_URL.resolve("test"), BASE_URL.resolve("test2"));
        final Client mockClient = mock(Client.class);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, data);
        final HttpResponse<Void> mockResponseOk = TestUtils.mockVoidResponse(204);

        when(mockClient.getAsTurtle(BASE_URL)).thenReturn(mockResponse);
        when(mockClient.deleteAsync(BASE_URL.resolve("/test")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(BASE_URL.resolve("/test2")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        assertDoesNotThrow(() -> solidClientProvider.deleteContentsRecursively(BASE_URL));
        verify(mockClient).getAsTurtle(BASE_URL);
        verify(mockClient).deleteAsync(BASE_URL.resolve("/test"));
        verify(mockClient).deleteAsync(BASE_URL.resolve("/test2"));
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void deleteContentsOneFails() {
        final String data = turtleList(BASE_URL, BASE_URL.resolve("test"), BASE_URL.resolve("test2"));
        final Client mockClient = mock(Client.class);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, data);
        final HttpResponse<Void> mockResponseOk = TestUtils.mockVoidResponse(204);
        final HttpResponse<Void> mockResponseFail = TestUtils.mockVoidResponse(400);

        when(mockClient.getAsTurtle(BASE_URL)).thenReturn(mockResponse);
        when(mockClient.deleteAsync(BASE_URL.resolve("/test")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(BASE_URL.resolve("/test2")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseFail));

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        assertDoesNotThrow(() -> solidClientProvider.deleteContentsRecursively(BASE_URL));
        verify(mockClient).getAsTurtle(BASE_URL);
        verify(mockClient).deleteAsync(BASE_URL.resolve("/test"));
        verify(mockClient).deleteAsync(BASE_URL.resolve("/test2"));
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void deleteContainerOneException() {
        final String data = turtleList(BASE_URL, BASE_URL.resolve("test"), BASE_URL.resolve("test2"));
        final Client mockClient = mock(Client.class);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, data);
        final HttpResponse<Void> mockResponseOk = TestUtils.mockVoidResponse(204);
        final HttpResponse<Void> mockResponseException = mock(HttpResponse.class);
        // TODO: This causes a failure in a delete but the code cannot detect which so carries on deleting other
        // resources which may fail. Better handling needed.
        when(mockResponseException.statusCode()).thenThrow(new RuntimeException("FAIL"));

        when(mockClient.getAsTurtle(BASE_URL)).thenReturn(mockResponse);
        when(mockClient.deleteAsync(BASE_URL.resolve("/test")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(BASE_URL.resolve("/test2")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseException));
        when(mockClient.deleteAsync(BASE_URL))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        assertDoesNotThrow(() -> solidClientProvider.deleteResourceRecursively(BASE_URL));
        verify(mockClient).getAsTurtle(BASE_URL);
        verify(mockClient).deleteAsync(BASE_URL.resolve("/test"));
        verify(mockClient).deleteAsync(BASE_URL.resolve("/test2"));
        verify(mockClient).deleteAsync(BASE_URL);
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void deleteContainerListFails() {
        final Client mockClient = mock(Client.class);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(400, null);

        when(mockClient.getAsTurtle(BASE_URL)).thenReturn(mockResponse);
        when(mockClient.deleteAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        assertDoesNotThrow(() -> solidClientProvider.deleteResourceRecursively(BASE_URL));
        verify(mockClient).getAsTurtle(BASE_URL);
        verify(mockClient, times(1)).deleteAsync(any());
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void deleteContainerListParseException() {
        final Client mockClient = mock(Client.class);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "NOT RDF");

        when(mockClient.getAsTurtle(BASE_URL)).thenReturn(mockResponse);
        when(mockClient.deleteAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        assertDoesNotThrow(() -> solidClientProvider.deleteResourceRecursively(BASE_URL));
        verify(mockClient).getAsTurtle(BASE_URL);
        verify(mockClient, times(1)).deleteAsync(any());
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void deleteContainer() {
        final String data = turtleList(BASE_URL, BASE_URL.resolve("test"), BASE_URL.resolve("test2"));
        final Client mockClient = mock(Client.class);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, data);
        final HttpResponse<Void> mockResponseOk = TestUtils.mockVoidResponse(204);

        when(mockClient.getAsTurtle(BASE_URL)).thenReturn(mockResponse);
        when(mockClient.deleteAsync(BASE_URL.resolve("/test")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(BASE_URL.resolve("/test2")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(BASE_URL))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        assertDoesNotThrow(() -> solidClientProvider.deleteResourceRecursively(BASE_URL));
        verify(mockClient).getAsTurtle(BASE_URL);
        verify(mockClient).deleteAsync(BASE_URL.resolve("/test"));
        verify(mockClient).deleteAsync(BASE_URL.resolve("/test2"));
        verify(mockClient).deleteAsync(BASE_URL);
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void deleteContainerDeep() {
        final String data = turtleList(BASE_URL, BASE_URL.resolve("test"), BASE_URL.resolve("child/"));
        final String data2 = turtleList(BASE_URL.resolve("child/"),
                BASE_URL.resolve("test2"), BASE_URL.resolve("test3"));
        final Client mockClient = mock(Client.class);
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, data);
        final HttpResponse<String> mockResponseChild = TestUtils.mockStringResponse(200, data2);
        final HttpResponse<Void> mockResponseOk = TestUtils.mockVoidResponse(204);

        when(mockClient.getAsTurtle(BASE_URL)).thenReturn(mockResponse);
        when(mockClient.getAsTurtle(BASE_URL.resolve("/child/"))).thenReturn(mockResponseChild);
        when(mockClient.deleteAsync(BASE_URL.resolve("/test")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(BASE_URL.resolve("/test2")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(BASE_URL.resolve("/test3"))).
                thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(BASE_URL.resolve("/child/")))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(BASE_URL))
                .thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));

        final SolidClientProvider solidClientProvider = new SolidClientProvider(mockClient);
        assertDoesNotThrow(() -> solidClientProvider.deleteResourceRecursively(BASE_URL));
        verify(mockClient).getAsTurtle(BASE_URL);
        verify(mockClient).getAsTurtle(BASE_URL.resolve("/child/"));
        verify(mockClient).deleteAsync(BASE_URL.resolve("/test"));
        verify(mockClient).deleteAsync(BASE_URL.resolve("/test2"));
        verify(mockClient).deleteAsync(BASE_URL.resolve("/test3"));
        verify(mockClient).deleteAsync(BASE_URL.resolve("/child/"));
        verify(mockClient).deleteAsync(BASE_URL);
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void deleteNull() {
        final SolidClientProvider solidClientProvider = new SolidClientProvider();
        assertThrows(IllegalArgumentException.class, () -> solidClientProvider.deleteResourceRecursively(null));
    }

    @Test
    void testToString() {
        final SolidClientProvider solidClientProvider = new SolidClientProvider();
        assertEquals("SolidClientProvider: user=, accessToken=null", solidClientProvider.toString());
    }

    @Test
    void testToStringNamed() throws Exception {
        clientRegistry.register("toStringUser", new Client.Builder("toStringUser").build());
        final SolidClientProvider solidClientProvider = new SolidClientProvider("toStringUser");
        assertEquals("SolidClientProvider: user=toStringUser, accessToken=null", solidClientProvider.toString());
    }

    String turtleList(final URI container, final URI... args) {
        return String.format(ENTRY, container,
                Arrays.stream(args).map(URI::toString).collect(Collectors.joining(">, <")));
    }
}
