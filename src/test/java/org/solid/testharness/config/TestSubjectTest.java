package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.solid.testharness.http.*;
import org.solid.testharness.utils.TestHarnessInitializationException;
import org.solid.testharness.utils.TestUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class TestSubjectTest {
    @Inject
    Config config;

    @Inject
    TestSubject testSubject;

    @InjectMock
    AuthManager authManager;

    @InjectMock
    ClientRegistry clientRegistry;

    @Test
    void prepareServerNoRootAcl() throws Exception {
        config.setConfigUrl(TestUtils.getFileUrl("src/test/resources/config-sample.ttl"));
        config.setTestSubject(iri("https://github.com/solid/conformance-test-harness/testserver"));
        testSubject.loadTestSubjectConfig();
        assertDoesNotThrow(() -> testSubject.prepareServer());
        verify(clientRegistry, never()).getClient(HttpConstants.ALICE);
    }

    @Test
    void prepareServerSetupRootAclBadUser() throws Exception {
        config.setConfigUrl(TestUtils.getFileUrl("src/test/resources/config-sample-bad.ttl"));
        config.setTestSubject(iri("https://github.com/solid/conformance-test-harness/bad-users"));
        final Client mockClient = mock(Client.class);
        when(clientRegistry.getClient(HttpConstants.ALICE)).thenReturn(mockClient);
        testSubject.loadTestSubjectConfig();
        assertThrows(NullPointerException.class, () -> testSubject.prepareServer());
    }

    @Test
    void prepareServerWithRootAcl() throws IOException, InterruptedException {
        config.setConfigUrl(TestUtils.getFileUrl("src/test/resources/config-sample-bad.ttl"));
        config.setTestSubject(iri("https://github.com/solid/conformance-test-harness/example"));
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        final Map<String, List<String>> headerMap = Map.of("Link",
                List.of("<http://localhost/.acl>; rel=\"acl\""));
        final HttpHeaders mockHeaders = HttpHeaders.of(headerMap, (k, v) -> true);
        final HttpResponse<Void> mockResponseOk = mockVoidResponse(200);

        when(clientRegistry.getClient(HttpConstants.ALICE)).thenReturn(mockClient);
        when(mockClient.head(any())).thenReturn(mockResponse);
        when(mockResponse.headers()).thenReturn(mockHeaders);
        when(mockClient.put(eq(URI.create("http://localhost/.acl")), any(),
                eq(HttpConstants.MEDIA_TYPE_TEXT_TURTLE)))
                .thenReturn(mockResponseOk);

        testSubject.loadTestSubjectConfig();
        assertDoesNotThrow(() -> testSubject.prepareServer());
        final String expectedAcl = "@prefix acl: <http://www.w3.org/ns/auth/acl#>. " +
                "<#alice> a acl:Authorization ; " +
                "  acl:agent <https://example.org/webid> ;" +
                "  acl:accessTo <./>;" +
                "  acl:default <./>;" +
                "  acl:mode acl:Read, acl:Write, acl:Control .";
        verify(mockClient).put(URI.create("http://localhost/.acl"), expectedAcl,
                HttpConstants.MEDIA_TYPE_TEXT_TURTLE);
    }

    @Test
    void prepareServerWithRootAclThrows() throws IOException, InterruptedException {
        config.setConfigUrl(TestUtils.getFileUrl("src/test/resources/config-sample-bad.ttl"));
        config.setTestSubject(iri("https://github.com/solid/conformance-test-harness/example"));
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        final Map<String, List<String>> headerMap = Map.of("Link",
                List.of("<http://localhost/.acl>; rel=\"acl\""));
        final HttpHeaders mockHeaders = HttpHeaders.of(headerMap, (k, v) -> true);
        final HttpResponse<Void> mockResponseOk = mockVoidResponse(200);

        when(clientRegistry.getClient(HttpConstants.ALICE)).thenReturn(mockClient);
        when(mockClient.head(any())).thenThrow(new IOException());

        testSubject.loadTestSubjectConfig();
        final Exception exception = assertThrows(TestHarnessInitializationException.class,
                () -> testSubject.prepareServer());
        assertEquals("Failed to create root ACL: java.io.IOException", exception.getMessage());
    }

    @Test
    void prepareServerWithRootAclNoLink() throws IOException, InterruptedException {
        config.setConfigUrl(TestUtils.getFileUrl("src/test/resources/config-sample-bad.ttl"));
        config.setTestSubject(iri("https://github.com/solid/conformance-test-harness/example"));
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        final Map<String, List<String>> headerMap = Map.of("Link",
                List.of("<http://localhost/.acl>; rel=\"notacl\""));
        final HttpHeaders mockHeaders = HttpHeaders.of(headerMap, (k, v) -> true);

        when(clientRegistry.getClient(HttpConstants.ALICE)).thenReturn(mockClient);
        when(mockClient.head(any())).thenReturn(mockResponse);
        when(mockResponse.headers()).thenReturn(mockHeaders);

        testSubject.loadTestSubjectConfig();
        final Exception exception = assertThrows(TestHarnessInitializationException.class,
                () -> testSubject.prepareServer());
        assertEquals("Failed getting the root ACL link", exception.getMessage());
    }

    @Test
    void prepareServerWithRootAclFails() throws IOException, InterruptedException {
        config.setConfigUrl(TestUtils.getFileUrl("src/test/resources/config-sample-bad.ttl"));
        config.setTestSubject(iri("https://github.com/solid/conformance-test-harness/example"));
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        final Map<String, List<String>> headerMap = Map.of("Link",
                List.of("<http://localhost/.acl>; rel=\"acl\""));
        final HttpHeaders mockHeaders = HttpHeaders.of(headerMap, (k, v) -> true);
        final HttpResponse<Void> mockResponseOk = mockVoidResponse(500);

        when(clientRegistry.getClient(HttpConstants.ALICE)).thenReturn(mockClient);
        when(mockClient.head(any())).thenReturn(mockResponse);
        when(mockResponse.headers()).thenReturn(mockHeaders);
        when(mockClient.put(eq(URI.create("http://localhost/.acl")), any(),
                eq(HttpConstants.MEDIA_TYPE_TEXT_TURTLE)))
                .thenReturn(mockResponseOk);

        testSubject.loadTestSubjectConfig();
        final Exception exception = assertThrows(TestHarnessInitializationException.class,
                () -> testSubject.prepareServer());
        assertEquals("Failed to create root ACL", exception.getMessage());
    }

    @Test
    void registerClients() throws Exception {
        config.setConfigUrl(TestUtils.getFileUrl("src/test/resources/config-sample.ttl"));
        config.setTestSubject(iri("https://github.com/solid/conformance-test-harness/testserver"));
        final Client mockClient = mock(Client.class);
        when(authManager.authenticate(anyString(), any(TargetServer.class))).thenReturn(new SolidClient(mockClient));
        testSubject.loadTestSubjectConfig();
        testSubject.registerClients();
        final Map<String, SolidClient> clients = testSubject.getClients();
        assertNotNull(clients);
        assertEquals(2, clients.size());
    }

    @Test
    void registerClientsWithAuthException() throws Exception {
        config.setConfigUrl(TestUtils.getFileUrl("src/test/resources/config-sample.ttl"));
        config.setTestSubject(iri("https://github.com/solid/conformance-test-harness/testserver"));
        when(authManager.authenticate(anyString(), any(TargetServer.class)))
                .thenThrow(new Exception("Failed as expected"));
        testSubject.loadTestSubjectConfig();
        assertThrows(TestHarnessInitializationException.class, () -> testSubject.registerClients());
    }

    @Test
    void getTargetServer() throws Exception {
        config.setConfigUrl(TestUtils.getFileUrl("src/test/resources/config-sample.ttl"));
        config.setTestSubject(iri("https://github.com/solid/conformance-test-harness/testserver"));
        testSubject.loadTestSubjectConfig();
        final TargetServer targetServer = testSubject.getTargetServer();
        assertNotNull(targetServer);
        assertEquals("https://github.com/solid/conformance-test-harness/testserver", targetServer.getSubject());
    }

    @Test
    void getTargetServer2() throws Exception {
        config.setConfigUrl(TestUtils.getFileUrl("src/test/resources/config-sample.ttl"));
        config.setTestSubject(iri("https://github.com/solid/conformance-test-harness/testserver2"));
        testSubject.loadTestSubjectConfig();
        final TargetServer targetServer = testSubject.getTargetServer();
        assertNotNull(targetServer);
        assertEquals("https://github.com/solid/conformance-test-harness/testserver2", targetServer.getSubject());
    }

    @Test
    void setTargetServer() {
        final TargetServer targetServer = mock(TargetServer.class);
        final URI serverRoot = URI.create("http://localhost/");
        when(targetServer.getServerRoot()).thenReturn(serverRoot);
        testSubject.setTargetServer(targetServer);
        assertEquals(serverRoot, testSubject.getTargetServer().getServerRoot());
    }

    @Test
    void getTargetServerDefault() throws Exception {
        config.setConfigUrl(TestUtils.getFileUrl("src/test/resources/config-sample.ttl"));
        config.setTestSubject(iri("https://github.com/solid/conformance-test-harness/testserver"));
        testSubject.loadTestSubjectConfig();
        final TargetServer targetServer = testSubject.getTargetServer();
        assertNotNull(targetServer);
    }

    @Test
    void getClients() throws Exception {
        config.setConfigUrl(TestUtils.getFileUrl("src/test/resources/config-sample.ttl"));
        config.setTestSubject(iri("https://github.com/solid/conformance-test-harness/testserver"));
        final Client mockClient = mock(Client.class);
        when(authManager.authenticate(anyString(), any(TargetServer.class))).thenReturn(new SolidClient(mockClient));
        testSubject.loadTestSubjectConfig();
        testSubject.registerClients();
        final Map<String, SolidClient> clients = testSubject.getClients();
        assertNotNull(clients);
        assertEquals(2, clients.size());
        assertTrue(clients.containsKey(HttpConstants.ALICE));
        assertNotNull(clients.get(HttpConstants.ALICE));
        assertTrue(clients.containsKey(HttpConstants.BOB));
        assertNotNull(clients.get(HttpConstants.BOB));
    }

    private HttpResponse<Void> mockVoidResponse(final int status) {
        final HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(status);
        return mockResponse;
    }
}
