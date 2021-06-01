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
package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.solid.testharness.http.*;
import org.solid.testharness.utils.SolidContainer;
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
@TestProfile(ConfigTestNormalProfile.class)
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
        final Client mockClient = mock(Client.class);
        when(clientRegistry.getClient(HttpConstants.ALICE)).thenReturn(mockClient);

        testSubject.loadTestSubjectConfig();
        assertDoesNotThrow(() -> testSubject.prepareServer());
        verify(mockClient, never()).head(any());
        assertNotNull(testSubject.getRootTestContainer());
    }

    @Test
    void prepareServerWithRootAcl() throws IOException, InterruptedException {
        config.setConfigUrl(TestUtils.getFileUrl("src/test/resources/config-sample-bad.ttl"));
        config.setTestSubject(iri("https://github.com/solid/conformance-test-harness/example"));
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        final Map<String, List<String>> headerMap = Map.of(HttpConstants.HEADER_LINK,
                List.of("<https://target.example.org/.acl>; rel=\"acl\""));
        final HttpHeaders mockHeaders = HttpHeaders.of(headerMap, (k, v) -> true);
        final HttpResponse<Void> mockResponseOk = TestUtils.mockVoidResponse(200);

        when(clientRegistry.getClient(HttpConstants.ALICE)).thenReturn(mockClient);
        when(mockClient.head(any())).thenReturn(mockResponse);
        when(mockResponse.headers()).thenReturn(mockHeaders);
        when(mockClient.put(eq(URI.create("https://target.example.org/.acl")), any(),
                eq(HttpConstants.MEDIA_TYPE_TEXT_TURTLE)))
                .thenReturn(mockResponseOk);

        testSubject.loadTestSubjectConfig();
        assertDoesNotThrow(() -> testSubject.prepareServer());
        final String expectedAcl = "@prefix acl: <http://www.w3.org/ns/auth/acl#>. " +
                "<#alice> a acl:Authorization ; " +
                "  acl:agent <https://alice.target.example.org/profile/card#me> ;" +
                "  acl:accessTo <./>;" +
                "  acl:default <./>;" +
                "  acl:mode acl:Read, acl:Write, acl:Control .";
        verify(mockClient).put(URI.create("https://target.example.org/.acl"), expectedAcl,
                HttpConstants.MEDIA_TYPE_TEXT_TURTLE);
    }

    @Test
    void prepareServerWithRootAclThrows() throws IOException, InterruptedException {
        config.setConfigUrl(TestUtils.getFileUrl("src/test/resources/config-sample-bad.ttl"));
        config.setTestSubject(iri("https://github.com/solid/conformance-test-harness/example"));
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        final Map<String, List<String>> headerMap = Map.of(HttpConstants.HEADER_LINK,
                List.of("<http://localhost/.acl>; rel=\"acl\""));
        final HttpHeaders mockHeaders = HttpHeaders.of(headerMap, (k, v) -> true);
        final HttpResponse<Void> mockResponseOk = TestUtils.mockVoidResponse(200);

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
        final Map<String, List<String>> headerMap = Map.of(HttpConstants.HEADER_LINK,
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
        final Map<String, List<String>> headerMap = Map.of(HttpConstants.HEADER_LINK,
                List.of("<http://localhost/.acl>; rel=\"acl\""));
        final HttpHeaders mockHeaders = HttpHeaders.of(headerMap, (k, v) -> true);
        final HttpResponse<Void> mockResponseFail = TestUtils.mockVoidResponse(500);

        when(clientRegistry.getClient(HttpConstants.ALICE)).thenReturn(mockClient);
        when(mockClient.head(any())).thenReturn(mockResponse);
        when(mockResponse.headers()).thenReturn(mockHeaders);
        when(mockClient.put(eq(URI.create("http://localhost/.acl")), any(),
                eq(HttpConstants.MEDIA_TYPE_TEXT_TURTLE)))
                .thenReturn(mockResponseFail);

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
        when(targetServer.getOrigin()).thenReturn("http://test");
        testSubject.setTargetServer(targetServer);
        assertEquals("http://test", testSubject.getTargetServer().getOrigin());
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

    @Test
    void tearDownServer() throws Exception {
        final SolidClient mockSolidClient = mock(SolidClient.class);
        testSubject.setRootTestContainer(SolidContainer.create(mockSolidClient, "https://localhost/container/"));
        assertDoesNotThrow(() -> testSubject.tearDownServer());
        verify(mockSolidClient).deleteResourceRecursively(eq(URI.create("https://localhost/container/")));
    }

    @Test
    void tearDownServerFails() throws Exception {
        final SolidClient mockSolidClient = mock(SolidClient.class);
        testSubject.setRootTestContainer(SolidContainer.create(mockSolidClient, "https://localhost/container/"));
        doThrow(new Exception("FAIL")).when(mockSolidClient).deleteResourceRecursively(any());
        assertDoesNotThrow(() -> testSubject.tearDownServer());
        verify(mockSolidClient).deleteResourceRecursively(eq(URI.create("https://localhost/container/")));
    }
}
