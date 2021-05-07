package org.solid.testharness.http;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
class SolidClientTest {
    private static final String PREFIX = "@prefix ldp: <http://www.w3.org/ns/ldp#> .";

    @Test
    void createDefaultClient() {
        SolidClient solidClient = new SolidClient();
        Client client = solidClient.getClient();
        assertNotNull(client.getHttpClient());
        assertEquals("", client.getUser());
        assertNull(client.getAccessToken());
    }

    @Test
    void createMissingNamedClient() {
        SolidClient solidClient = new SolidClient("nobody");
        assertNull(solidClient.getClient());
    }

    @Test
    void createWithExistingClient() {
        Client newClient = new Client.Builder("newuser").build();
        SolidClient solidClient = new SolidClient(newClient);
        Client client = solidClient.getClient();
        assertNotNull(client.getHttpClient());
        assertEquals("newuser", client.getUser());
        assertNull(client.getAccessToken());
    }

    @Test
    void createNamedClient() {
        ClientRegistry.register("user1", new Client.Builder("user1").build());
        SolidClient solidClient = new SolidClient("user1");
        Client client = solidClient.getClient();
        assertNotNull(client.getHttpClient());
        assertEquals("user1", client.getUser());
        assertNull(client.getAccessToken());
    }

    @Test
    void createStatically() {
        ClientRegistry.register("user2", new Client.Builder("user2").build());
        SolidClient solidClient = SolidClient.create("user2");
        Client client = solidClient.getClient();
        assertNotNull(client.getHttpClient());
        assertEquals("user2", client.getUser());
        assertNull(client.getAccessToken());
    }

    @Test
    void setupRootAcl() throws IOException, InterruptedException {
        Client mockClient = mock(Client.class);
        HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        Map<String, List<String>> headerMap = Map.of("Link", List.of("<http://localhost:3000/.acl>; rel=\"acl\""));
        HttpHeaders mockHeaders = HttpHeaders.of(headerMap, (k, v) -> true);
        HttpResponse<Void> mockResponseOk = mockVoidResponse(200);

        when(mockClient.head(any())).thenReturn(mockResponse);
        when(mockResponse.headers()).thenReturn(mockHeaders);
        when(mockClient.put(eq(URI.create("http://localhost:3000/.acl")), any(), eq("text/turtle"))).thenReturn(mockResponseOk);

        SolidClient solidClient = new SolidClient(mockClient);
        boolean res = solidClient.setupRootAcl("http://localhost:3000", "https://example.org/webid");
        assertTrue(res);
        String expectedAcl = "@prefix acl: <http://www.w3.org/ns/auth/acl#>. " +
                "<#alice> a acl:Authorization ; " +
                "  acl:agent <https://example.org/webid> ;" +
                "  acl:accessTo </>;" +
                "  acl:default </>;" +
                "  acl:mode acl:Read, acl:Write, acl:Control .";
        verify(mockClient).put(URI.create("http://localhost:3000/.acl"), expectedAcl, "text/turtle");
    }

    @Test
    void setupRootAclNoSlash() throws IOException, InterruptedException {
        Client mockClient = mock(Client.class);
        HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        Map<String, List<String>> headerMap = Map.of("Link", List.of("<http://localhost:3000/.acl>; rel=\"acl\""));
        HttpHeaders mockHeaders = HttpHeaders.of(headerMap, (k, v) -> true);
        HttpResponse<Void> mockResponseOk = mockVoidResponse(200);

        when(mockClient.head(any())).thenReturn(mockResponse);
        when(mockResponse.headers()).thenReturn(mockHeaders);
        when(mockClient.put(eq(URI.create("http://localhost:3000/.acl")), any(), eq("text/turtle"))).thenReturn(mockResponseOk);

        SolidClient solidClient = new SolidClient(mockClient);
        boolean res = solidClient.setupRootAcl("http://localhost:3000/", "https://example.org/webid");
        assertTrue(res);
        String expectedAcl = "@prefix acl: <http://www.w3.org/ns/auth/acl#>. " +
                "<#alice> a acl:Authorization ; " +
                "  acl:agent <https://example.org/webid> ;" +
                "  acl:accessTo </>;" +
                "  acl:default </>;" +
                "  acl:mode acl:Read, acl:Write, acl:Control .";
        verify(mockClient).put(URI.create("http://localhost:3000/.acl"), expectedAcl, "text/turtle");
    }

    @Test
    void setupRootAclFails() throws IOException, InterruptedException {
        Client mockClient = mock(Client.class);
        HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        Map<String, List<String>> headerMap = Map.of("Link", List.of("<http://localhost:3000/.acl>; rel=\"acl\""));
        HttpHeaders mockHeaders = HttpHeaders.of(headerMap, (k, v) -> true);
        HttpResponse<Void> mockResponseFail = mockVoidResponse(500);

        when(mockClient.head(any())).thenReturn(mockResponse);
        when(mockResponse.headers()).thenReturn(mockHeaders);
        when(mockClient.put(eq(URI.create("http://localhost:3000/.acl")), any(), eq("text/turtle"))).thenReturn(mockResponseFail);

        SolidClient solidClient = new SolidClient(mockClient);
        boolean res = solidClient.setupRootAcl("http://localhost:3000", "https://example.org/webid");
        assertFalse(res);
    }

    @Test
    void getAuthHeaders() {
        Client mockClient = mock(Client.class);
        when(mockClient.getAuthHeaders("GET", "http://localhost:3000")).thenReturn(Collections.emptyMap());

        SolidClient solidClient = new SolidClient(mockClient);
        var headers = solidClient.getAuthHeaders("GET", "http://localhost:3000");
        assertTrue((headers.isEmpty()));
    }

    @Test
    void createResource() throws Exception {
        Client mockClient = mock(Client.class);
        HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        HttpHeaders mockHeaders = HttpHeaders.of(Collections.emptyMap(), (k, v) -> true);
        when(mockResponse.headers()).thenReturn(mockHeaders);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockClient.put(eq(URI.create("http://localhost:3000/test")), eq("DATA"), eq("text/plain"))).thenReturn(mockResponse);

        SolidClient solidClient = new SolidClient(mockClient);
        HttpHeaders headers = solidClient.createResource(URI.create("http://localhost:3000/test"), "DATA", "text/plain");
        assertTrue(headers.map().isEmpty());
        verify(mockClient).put(URI.create("http://localhost:3000/test"), "DATA", "text/plain");
    }

    @Test
    void createResourceFails() throws Exception {
        Client mockClient = mock(Client.class);
        HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        HttpHeaders mockHeaders = HttpHeaders.of(Collections.emptyMap(), (k, v) -> true);
        when(mockResponse.headers()).thenReturn(mockHeaders);
        when(mockResponse.statusCode()).thenReturn(412);
        when(mockClient.put(eq(URI.create("http://localhost:3000/test")), eq("DATA"), eq("text/plain"))).thenReturn(mockResponse);

        SolidClient solidClient = new SolidClient(mockClient);
        Exception exception = assertThrows(Exception.class, () -> solidClient.createResource(URI.create("http://localhost:3000/test"), "DATA", "text/plain"));
        assertEquals("Failed to create resource - status=412", exception.getMessage());
    }

    @Test
    void getResourceAclLink() throws IOException, InterruptedException {
        Client mockClient = mock(Client.class);
        HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        Map<String, List<String>> headerMap = Map.of("Link", List.of("<http://localhost:3000/test.acl>; rel=\"acl\""));
        HttpHeaders mockHeaders = HttpHeaders.of(headerMap, (k, v) -> true);
        when(mockResponse.headers()).thenReturn(mockHeaders);
        when(mockClient.head(any())).thenReturn(mockResponse);

        SolidClient solidClient = new SolidClient(mockClient);
        URI uri = solidClient.getResourceAclLink("http://localhost:3000/test");
        assertEquals(URI.create("http://localhost:3000/test.acl"), uri);
        verify(mockClient).head(URI.create("http://localhost:3000/test"));
    }

    @Test
    void getResourceAclLinkFails() throws IOException, InterruptedException {
        Client mockClient = mock(Client.class);
        when(mockClient.head(any())).thenThrow(new IOException("Failed"));

        SolidClient solidClient = new SolidClient(mockClient);
        assertThrows(IOException.class, () -> solidClient.getResourceAclLink("http://localhost:3000/test"));
    }

    @Test
    void getAclLink() {
        Map<String, List<String>> headerMap = Map.of("Link", List.of("<http://localhost:3000/test.acl>; rel=\"acl\""));
        HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);

        SolidClient solidClient = new SolidClient();
        URI uri = solidClient.getAclLink(headers);
        assertEquals(URI.create("http://localhost:3000/test.acl"), uri);
    }

    @Test
    void getAclLinkMissing() {
        HttpHeaders headers = HttpHeaders.of(Collections.emptyMap(), (k, v) -> true);

        SolidClient solidClient = new SolidClient();
        assertNull(solidClient.getAclLink(headers));
    }

    @Test
    void getAclLinkMultiple() {
        Map<String, List<String>> headerMap = Map.of("Link", List.of("<http://localhost:3000/test.acl>; rel=\"acl\"", "<http://localhost:3000/test.acl2>; rel=\"acl\""));
        HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);

        SolidClient solidClient = new SolidClient();
        URI uri = solidClient.getAclLink(headers);
        assertEquals(URI.create("http://localhost:3000/test.acl"), uri);
    }

    @Test
    void createAcl() throws IOException, InterruptedException {
        URI resourceAcl = URI.create("http://localhost:3000/test.acl");
        Client mockClient = mock(Client.class);
        HttpResponse<Void> mockResponse = mock(HttpResponse.class);

        when(mockClient.put(eq(resourceAcl), eq("ACL"), eq("text/turtle"))).thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(200);

        SolidClient solidClient = new SolidClient(mockClient);
        boolean res = solidClient.createAcl(resourceAcl, "ACL");
        assertTrue(res);
        verify(mockClient).put(resourceAcl, "ACL", "text/turtle");
    }

    @Test
    void createAclFails() throws IOException, InterruptedException {
        Client mockClient = mock(Client.class);
        HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        URI resourceAcl = URI.create("http://localhost:3000/test.acl");

        when(mockClient.head(any())).thenReturn(mockResponse);
        when(mockClient.put(eq(resourceAcl), eq("ACL"), eq("text/turtle"))).thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(500);

        SolidClient solidClient = new SolidClient(mockClient);
        boolean res = solidClient.createAcl(resourceAcl, "ACL");
        assertFalse(res);
        verify(mockClient).put(resourceAcl, "ACL", "text/turtle");
    }

    @Test
    void getContainmentData() throws Exception {
        Client mockClient = mock(Client.class);
        URI resourceAcl = URI.create("http://localhost:3000/test");
        HttpResponse<String> mockResponse = mockStringResponse(200, "TEST");

        when(mockClient.getAsString(eq(resourceAcl))).thenReturn(mockResponse);

        SolidClient solidClient = new SolidClient(mockClient);
        assertEquals("TEST", solidClient.getContainmentData(resourceAcl));
        verify(mockClient).getAsString(resourceAcl);
    }

    @Test
    void getContainmentDataFails() throws IOException, InterruptedException {
        Client mockClient = mock(Client.class);
        URI resourceAcl = URI.create("http://localhost:3000/test");
        HttpResponse<String> mockResponse = mockStringResponse(500, null);

        when(mockClient.getAsString(eq(resourceAcl))).thenReturn(mockResponse);

        SolidClient solidClient = new SolidClient(mockClient);
        Exception exception = assertThrows(Exception.class, () -> solidClient.getContainmentData(resourceAcl));
        assertEquals("Error response=500 trying to get container members for http://localhost:3000/test", exception.getMessage());
    }

    @Test
    void parseMembers() throws Exception {
        String data = PREFIX + "<http://localhost:3000/> ldp:contains <http://localhost:3000/test/>.";
        SolidClient solidClient = new SolidClient();
        List<String> members = solidClient.parseMembers(data, URI.create("http://localhost:3000/test/"));
        assertFalse(members.isEmpty());
        assertEquals("http://localhost:3000/test/", members.get(0));
    }

    @Test
    void parseMembersEmpty() throws Exception {
        String data = PREFIX + "<http://localhost:3000/> a ldp:Container.";
        SolidClient solidClient = new SolidClient();
        List<String> members = solidClient.parseMembers(data, URI.create("http://localhost:3000/test/"));
        assertTrue(members.isEmpty());
    }

    @Test
    void parseMembersFails() {
        SolidClient solidClient = new SolidClient();
        Exception exception = assertThrows(Exception.class, () -> solidClient.parseMembers("BAD", URI.create("http://localhost:3000/test/")));
        assertEquals("Bad container listing", exception.getMessage());
    }

    @Test
    void deleteResource() {
        String data = PREFIX + "<http://localhost:3000/> ldp:contains <http://localhost:3000/test>.";
        Client mockClient = mock(Client.class);
        HttpResponse<String> mockResponse = mockStringResponse(200, data);
        HttpResponse<Void> mockResponseOk = mockVoidResponse(200);

        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test"))).thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));

        SolidClient solidClient = new SolidClient(mockClient);
        assertDoesNotThrow(() -> solidClient.deleteResourceRecursively(URI.create("http://localhost:3000/test")));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test"));
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void deleteContents() throws IOException, InterruptedException {
        String data = PREFIX + "<http://localhost:3000/> ldp:contains <http://localhost:3000/test>, <http://localhost:3000/test2>.";
        Client mockClient = mock(Client.class);
        HttpResponse<String> mockResponse = mockStringResponse(200, data);
        HttpResponse<Void> mockResponseOk = mockVoidResponse(200);

        when(mockClient.getAsString(eq(URI.create("http://localhost:3000/")))).thenReturn(mockResponse);
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test"))).thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test2"))).thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));

        SolidClient solidClient = new SolidClient(mockClient);
        assertDoesNotThrow(() -> solidClient.deleteContentsRecursively(URI.create("http://localhost:3000/")));
        verify(mockClient).getAsString(URI.create("http://localhost:3000/"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test2"));
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void deleteContentsOneFails() throws IOException, InterruptedException {
        String data = PREFIX + "<http://localhost:3000/> ldp:contains <http://localhost:3000/test>, <http://localhost:3000/test2>.";
        Client mockClient = mock(Client.class);
        HttpResponse<String> mockResponse = mockStringResponse(200, data);
        HttpResponse<Void> mockResponseOk = mockVoidResponse(200);
        HttpResponse<Void> mockResponseFail = mockVoidResponse(500);

        when(mockClient.getAsString(eq(URI.create("http://localhost:3000/")))).thenReturn(mockResponse);
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test"))).thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test2"))).thenReturn(CompletableFuture.supplyAsync(() -> mockResponseFail));

        SolidClient solidClient = new SolidClient(mockClient);
        assertDoesNotThrow(() -> solidClient.deleteContentsRecursively(URI.create("http://localhost:3000/")));
        verify(mockClient).getAsString(URI.create("http://localhost:3000/"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test2"));
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void deleteContainerOneException() throws IOException, InterruptedException {
        String data = PREFIX + "<http://localhost:3000/> ldp:contains <http://localhost:3000/test>, <http://localhost:3000/test2>.";
        Client mockClient = mock(Client.class);
        HttpResponse<String> mockResponse = mockStringResponse(200, data);
        HttpResponse<Void> mockResponseOk = mockVoidResponse(200);
        HttpResponse<Void> mockResponseException = mock(HttpResponse.class);
        // TODO: This causes a failure in a delete but the code cannto detect which so carries on deleting other resources which may fail. Better handling needed.
        when(mockResponseException.statusCode()).thenThrow(new RuntimeException("FAIL"));

        when(mockClient.getAsString(eq(URI.create("http://localhost:3000/")))).thenReturn(mockResponse);
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test"))).thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test2"))).thenReturn(CompletableFuture.supplyAsync(() -> mockResponseException));
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/"))).thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));

        SolidClient solidClient = new SolidClient(mockClient);
        assertDoesNotThrow(() -> solidClient.deleteResourceRecursively(URI.create("http://localhost:3000/")));
        verify(mockClient).getAsString(URI.create("http://localhost:3000/"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test2"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/"));
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void deleteContainerListFails() throws IOException, InterruptedException {
        String data = PREFIX + "<http://localhost:3000/> ldp:contains <http://localhost:3000/test>.";
        Client mockClient = mock(Client.class);
        HttpResponse<String> mockResponse = mockStringResponse(500, null);

        when(mockClient.getAsString(eq(URI.create("http://localhost:3000/")))).thenReturn(mockResponse);

        SolidClient solidClient = new SolidClient(mockClient);
        assertDoesNotThrow(() -> solidClient.deleteResourceRecursively(URI.create("http://localhost:3000/")));
        verify(mockClient).getAsString(URI.create("http://localhost:3000/"));
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void deleteContainer() throws IOException, InterruptedException {
        String data = PREFIX + "<http://localhost:3000/> ldp:contains <http://localhost:3000/test>, <http://localhost:3000/test2>.";
        Client mockClient = mock(Client.class);
        HttpResponse<String> mockResponse = mockStringResponse(200, data);
        HttpResponse<Void> mockResponseOk = mockVoidResponse(200);

        when(mockClient.getAsString(eq(URI.create("http://localhost:3000/")))).thenReturn(mockResponse);
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test"))).thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test2"))).thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/"))).thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));

        SolidClient solidClient = new SolidClient(mockClient);
        assertDoesNotThrow(() -> solidClient.deleteResourceRecursively(URI.create("http://localhost:3000/")));
        verify(mockClient).getAsString(URI.create("http://localhost:3000/"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test2"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/"));
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void deleteContainerDeep() throws IOException, InterruptedException {
        String data = PREFIX + "<http://localhost:3000/> ldp:contains <http://localhost:3000/test>, <http://localhost:3000/child/>.";
        String data2 = PREFIX + "<http://localhost:3000/child/> ldp:contains <http://localhost:3000/test2>, <http://localhost:3000/test3>.";
        Client mockClient = mock(Client.class);
        HttpResponse<String> mockResponse = mockStringResponse(200, data);
        HttpResponse<String> mockResponseChild = mockStringResponse(200, data2);
        HttpResponse<Void> mockResponseOk = mockVoidResponse(200);

        when(mockClient.getAsString(eq(URI.create("http://localhost:3000/")))).thenReturn(mockResponse);
        when(mockClient.getAsString(eq(URI.create("http://localhost:3000/child/")))).thenReturn(mockResponseChild);
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test"))).thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test2"))).thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/test3"))).thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/child/"))).thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));
        when(mockClient.deleteAsync(URI.create("http://localhost:3000/"))).thenReturn(CompletableFuture.supplyAsync(() -> mockResponseOk));

        SolidClient solidClient = new SolidClient(mockClient);
        assertDoesNotThrow(() -> solidClient.deleteResourceRecursively(URI.create("http://localhost:3000/")));
        verify(mockClient).getAsString(URI.create("http://localhost:3000/"));
        verify(mockClient).getAsString(URI.create("http://localhost:3000/child/"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test2"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/test3"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/child/"));
        verify(mockClient).deleteAsync(URI.create("http://localhost:3000/"));
        verifyNoMoreInteractions(mockClient);
    }

    @Test
    void deleteNull() {
        SolidClient solidClient = new SolidClient();
        assertThrows(IllegalArgumentException.class, () -> solidClient.deleteResourceRecursively(null));
    }


        private HttpResponse<String> mockStringResponse(int status, String body) {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(status);
        when(mockResponse.body()).thenReturn(body);
        return mockResponse;
    }

    private HttpResponse<Void> mockVoidResponse(int status) {
        HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(status);
        return mockResponse;
    }

    @Test
    void testToString() {
        SolidClient solidClient = new SolidClient();
        assertEquals("SolidClient: user=, accessToken=null", solidClient.toString());
    }

    @Test
    void testToStringNamed() {
        ClientRegistry.register("toStringUser", new Client.Builder("toStringUser").build());
        SolidClient solidClient = new SolidClient("toStringUser");
        solidClient.getClient().setAccessToken("ACCESS");
        assertEquals("SolidClient: user=toStringUser, accessToken=ACCESS", solidClient.toString());
    }


}
