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
import org.solid.testharness.http.Client;
import org.solid.testharness.http.ClientRegistry;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.http.SolidClientProvider;
import org.solid.testharness.utils.SolidContainerProvider;
import org.solid.testharness.utils.TestHarnessInitializationException;
import org.solid.testharness.utils.TestUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.solid.testharness.config.Config.AccessControlMode.*;

@QuarkusTest
@TestProfile(ConfigTestNormalProfile.class)
public class TestSubjectTest {
    private static final URI TEST_URL = URI.create("https://localhost/container/");

    @InjectMock
    Config config;

    @Inject
    TestSubject testSubject;

    @InjectMock
    ClientRegistry clientRegistry;

    @Test
    void prepareServerWithoutServer() {
        testSubject.setTargetServer(null);
        assertThrows(TestHarnessInitializationException.class, () -> testSubject.prepareServer());
    }

    @Test
    void prepareServerAccessControlModeNoAcl() throws Exception {
        final Client mockClient = setupMockConfig(false, WAC);

        final HttpResponse<Void> mockVoidResponseNoLink = TestUtils.mockVoidResponse(200);
        when(mockClient.head(eq(URI.create("https://server/test/")))).thenReturn(mockVoidResponseNoLink);

        final Exception exception = assertThrows(TestHarnessInitializationException.class,
                () -> testSubject.prepareServer());
        assertEquals("Failed to determine access control mode: java.lang.Exception: Cannot get ACL url for " +
                "root test container: https://server/test/", exception.getMessage());
    }

    @Test
    void prepareServerAccessControlModeThrows() throws Exception {
        final Client mockClient = setupMockConfig(false, WAC);

        final HttpResponse<Void> mockVoidResponseLink = TestUtils.mockVoidResponse(200,
                Map.of(HttpConstants.HEADER_LINK, List.of("<https://server/test/.acl>; rel=\"acl\"")));
        when(mockClient.head(eq(URI.create("https://server/test/")))).thenReturn(mockVoidResponseLink);
        when(mockClient.head(eq(URI.create("https://server/test/.acl")))).thenThrow(new IOException());

        final Exception exception = assertThrows(TestHarnessInitializationException.class,
                () -> testSubject.prepareServer());
        assertEquals("Failed to determine access control mode: java.io.IOException", exception.getMessage());
    }

    @Test
    void prepareServerNoSetupRootAclWacMode() throws Exception {
        final Client mockClient = setupMockConfig(false, WAC);

        final HttpResponse<String> mockStringResponse = TestUtils.mockStringResponse(200, "");
        when(mockClient.getAsTurtle(any())).thenReturn(mockStringResponse);
        doReturn(mockStringResponse).when(mockClient).sendAuthorized(any(), any());
        final HttpResponse<Void> mockVoidResponse = TestUtils.mockVoidResponse(200, Map.of(HttpConstants.HEADER_LINK,
                List.of("<https://example.org/.acl>; rel=\"acl\"")));
        when(mockClient.head(any())).thenReturn(mockVoidResponse);

        assertDoesNotThrow(() -> testSubject.prepareServer());
        verify(mockClient, never()).put(any(), any(), any());
        assertNotNull(testSubject.getTestRunContainer());
    }

    @Test
    void prepareServerNoSetupRootAclAcpMode() throws Exception {
        final Client mockClient = setupMockConfig(false, ACP);

        final HttpResponse<String> mockStringResponse = TestUtils.mockStringResponse(200, "");
        when(mockClient.getAsTurtle(any())).thenReturn(mockStringResponse);
        doReturn(mockStringResponse).when(mockClient).sendAuthorized(any(), any());
        final HttpResponse<Void> mockVoidResponse = TestUtils.mockVoidResponse(200, Map.of(HttpConstants.HEADER_LINK,
                List.of("<https://example.org/.acl>; rel=\"acl\"",
                        "<http://www.w3.org/ns/solid/acp#AccessControlResource>; rel=\"type\"")));
        when(mockClient.head(any())).thenReturn(mockVoidResponse);

        assertDoesNotThrow(() -> testSubject.prepareServer());
        verify(mockClient, never()).put(any(), any(), any());
        verify(config, times(1)).setAccessControlMode(ACP);
        assertNotNull(testSubject.getTestRunContainer());
    }

    @Test
    void prepareServerNoSetupRootAclAcpLegacyMode() throws Exception {
        final Client mockClient = setupMockConfig(false, ACP_LEGACY);

        final HttpResponse<String> mockStringResponse = TestUtils.mockStringResponse(200, "");
        when(mockClient.getAsTurtle(any())).thenReturn(mockStringResponse);
        doReturn(mockStringResponse).when(mockClient).sendAuthorized(any(), any());
        final HttpResponse<Void> mockVoidResponse = TestUtils.mockVoidResponse(200, Map.of(HttpConstants.HEADER_LINK,
                List.of("<https://example.org/.acl>; rel=\"acl\"",
                        "<http://www.w3.org/ns/solid/acp#AccessControlResource>; rel=\"type\"")));
        when(mockClient.head(any())).thenReturn(mockVoidResponse);

        assertDoesNotThrow(() -> testSubject.prepareServer());
        verify(mockClient, never()).put(any(), any(), any());
        verify(config, times(1)).setAccessControlMode(ACP_LEGACY);
        assertNotNull(testSubject.getTestRunContainer());
    }

    @Test
    void prepareServerNoSetupRootAclThrows() throws Exception {
        final Client mockClient = setupMockConfig(false, WAC);

        final HttpResponse<Void> mockVoidResponseLink = TestUtils.mockVoidResponse(200,
                Map.of(HttpConstants.HEADER_LINK, List.of("<https://server/test/.acl>; rel=\"acl\"")));
        when(mockClient.head(eq(URI.create("https://server/test/")))).thenReturn(mockVoidResponseLink);
        when(mockClient.head(eq(URI.create("https://server/test/.acl")))).thenReturn(mockVoidResponseLink);

        when(mockClient.getAsTurtle(any())).thenThrow(new IOException());

        final Exception exception = assertThrows(TestHarnessInitializationException.class,
                () -> testSubject.prepareServer());
        assertEquals("Failed to prepare server: java.io.IOException", exception.getMessage());
    }

    @Test
    void prepareServerNoSetupRootAclContainerFails() throws Exception {
        final Client mockClient = setupMockConfig(false, WAC);

        final HttpResponse<Void> mockVoidResponseLink = TestUtils.mockVoidResponse(200,
                Map.of(HttpConstants.HEADER_LINK, List.of("<https://server/test/.acl>; rel=\"acl\"")));
        when(mockClient.head(eq(URI.create("https://server/test/")))).thenReturn(mockVoidResponseLink);
        when(mockClient.head(eq(URI.create("https://server/test/.acl")))).thenReturn(mockVoidResponseLink);

        final HttpResponse<String> mockStringResponse = TestUtils.mockStringResponse(200, "");
        when(mockClient.getAsTurtle(any())).thenReturn(mockStringResponse);
        when(mockClient.sendAuthorized(any(), any())).thenThrow(new IOException());

        final Exception exception = assertThrows(TestHarnessInitializationException.class,
                () -> testSubject.prepareServer());
        assertEquals("Failed to prepare server: java.io.IOException",
                exception.getMessage());
    }

    @Test
    void prepareServerWithSetupRootAcl() throws IOException, InterruptedException {
        final Client mockClient = setupMockConfig(true, WAC);

        final HttpResponse<Void> mockVoidResponseLink = TestUtils.mockVoidResponse(200,
                Map.of(HttpConstants.HEADER_LINK, List.of("<https://target.example.org/.acl>; rel=\"acl\"")));
        final HttpResponse<Void> mockVoidResponseOk = TestUtils.mockVoidResponse(200);
        when(mockClient.head(any())).thenReturn(mockVoidResponseLink);
        when(mockClient.put(eq(URI.create("https://target.example.org/.acl")), any(),
                eq(HttpConstants.MEDIA_TYPE_TEXT_TURTLE)))
                .thenReturn(mockVoidResponseOk);

        final HttpResponse<String> mockStringResponse = TestUtils.mockStringResponse(200, "");
        when(mockClient.getAsTurtle(any())).thenReturn(mockStringResponse);
        doReturn(mockStringResponse).when(mockClient).sendAuthorized(any(), any());

        assertDoesNotThrow(() -> testSubject.prepareServer());
        final String expectedAcl = "@prefix acl: <http://www.w3.org/ns/auth/acl#> .\n\n" +
                "[] a acl:Authorization;\n" +
                "  acl:accessTo <https://server/>;\n" +
                "  acl:agent <https://alice.target.example.org/profile/card#me>;\n" +
                "  acl:mode acl:Read, acl:Write, acl:Control .\n\n" +
                "[] a acl:Authorization;\n" +
                "  acl:default <https://server/>;\n" +
                "  acl:agent <https://alice.target.example.org/profile/card#me>;\n" +
                "  acl:mode acl:Read, acl:Write, acl:Control .\n";
        verify(mockClient).put(URI.create("https://target.example.org/.acl"), expectedAcl,
                HttpConstants.MEDIA_TYPE_TEXT_TURTLE);
    }

    @Test
    void prepareServerWithSetupRootAclThrows() throws IOException, InterruptedException {
        final Client mockClient = setupMockConfig(true, WAC);

        final HttpResponse<Void> mockVoidResponseLink = TestUtils.mockVoidResponse(200,
                Map.of(HttpConstants.HEADER_LINK, List.of("<https://server/test/.acl>; rel=\"acl\"")));
        when(mockClient.head(eq(URI.create("https://server/test/")))).thenReturn(mockVoidResponseLink);
        when(mockClient.head(eq(URI.create("https://server/test/.acl")))).thenReturn(mockVoidResponseLink);

        when(mockClient.head(eq(URI.create("https://server/")))).thenReturn(mockVoidResponseLink);
        when(mockClient.put(any(), any(), any())).thenThrow(new IOException());

        final Exception exception = assertThrows(TestHarnessInitializationException.class,
                () -> testSubject.prepareServer());
        assertEquals("Failed to create root ACL: java.io.IOException", exception.getMessage());
    }

    @Test
    void prepareServerWithSetupRootAclFails() throws IOException, InterruptedException {
        final Client mockClient = setupMockConfig(true, WAC);

        final HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        final Map<String, List<String>> headerMap = Map.of(HttpConstants.HEADER_LINK,
                List.of("<http://localhost/.acl>; rel=\"acl\""));
        final HttpHeaders mockHeaders = HttpHeaders.of(headerMap, (k, v) -> true);
        final HttpResponse<Void> mockResponseFail = TestUtils.mockVoidResponse(500);

        when(mockClient.head(any())).thenReturn(mockResponse);
        when(mockResponse.headers()).thenReturn(mockHeaders);
        when(mockClient.put(eq(URI.create("http://localhost/.acl")), any(),
                eq(HttpConstants.MEDIA_TYPE_TEXT_TURTLE)))
                .thenReturn(mockResponseFail);

        final Exception exception = assertThrows(TestHarnessInitializationException.class,
                () -> testSubject.prepareServer());
        assertEquals("Failed to create root ACL: java.lang.Exception: Error response=500 trying to apply ACL",
                exception.getMessage());
    }

    @Test
    void loadTestSubjectConfigTarget1() throws MalformedURLException {
        final URL testFileUrl = TestUtils.getFileUrl("src/test/resources/config/config-sample.ttl");
        final String subject = new URL(testFileUrl, "testserver").toString();
        when(config.getSubjectsUrl()).thenReturn(testFileUrl);
        when(config.getTestSubject()).thenReturn(iri(subject));
        testSubject.loadTestSubjectConfig();
        final TargetServer targetServer = testSubject.getTargetServer();
        assertNotNull(targetServer);
        assertEquals(subject, targetServer.getSubject());
    }

    @Test
    void loadTestSubjectConfigTarget2() throws Exception {
        final URL testFileUrl = TestUtils.getFileUrl("src/test/resources/config/config-sample.ttl");
        final String subject = new URL(testFileUrl, "testserver2").toString();
        when(config.getSubjectsUrl()).thenReturn(testFileUrl);
        when(config.getTestSubject()).thenReturn(iri(subject));
        testSubject.loadTestSubjectConfig();
        final TargetServer targetServer = testSubject.getTargetServer();
        assertNotNull(targetServer);
        assertEquals(subject, targetServer.getSubject());
    }

    @Test
    void setTargetServer() {
        final TargetServer targetServer = mock(TargetServer.class);
        when(targetServer.getFeatures()).thenReturn(Map.of("feature1", true));
        testSubject.setTargetServer(targetServer);
        assertTrue(testSubject.getTargetServer().getFeatures().get("feature1"));
    }

    @Test
    void getTargetServerDefault() throws Exception {
        final URL testFileUrl = TestUtils.getFileUrl("src/test/resources/config/config-sample-single.ttl");
        when(config.getSubjectsUrl()).thenReturn(testFileUrl);
        testSubject.loadTestSubjectConfig();
        final TargetServer targetServer = testSubject.getTargetServer();
        assertNotNull(targetServer);
        assertEquals(new URL(testFileUrl, "default").toString(), targetServer.getSubject());
    }

    @Test
    void tearDownServer() throws Exception {
        final SolidClientProvider mockSolidClientProvider = mock(SolidClientProvider.class);
        testSubject.setTestRunContainer(new SolidContainerProvider(mockSolidClientProvider, TEST_URL));
        assertDoesNotThrow(() -> testSubject.tearDownServer());
        verify(mockSolidClientProvider).deleteResourceRecursively(eq(TEST_URL));
    }

    @Test
    void tearDownServerFails() throws Exception {
        final SolidClientProvider mockSolidClientProvider = mock(SolidClientProvider.class);
        testSubject.setTestRunContainer(new SolidContainerProvider(mockSolidClientProvider, TEST_URL));
        doThrow(new Exception("FAIL")).when(mockSolidClientProvider).deleteResourceRecursively(any());
        assertDoesNotThrow(() -> testSubject.tearDownServer());
        verify(mockSolidClientProvider).deleteResourceRecursively(eq(TEST_URL));
    }

    private Client setupMockConfig(final boolean setupRootAcl, final Config.AccessControlMode mode) {
        final TargetServer targetServer = mock(TargetServer.class);
        testSubject.setTargetServer(targetServer);
        if (mode.equals(ACP_LEGACY)) {
            when(targetServer.getFeatures()).thenReturn(Map.of("acp-legacy", true));
        }
        when(config.isSetupRootAcl()).thenReturn(setupRootAcl);
        when(config.getWebIds())
                .thenReturn(Map.of(HttpConstants.ALICE, "https://alice.target.example.org/profile/card#me"));
        when(config.getTestContainer()).thenReturn("https://server/test/");
        when(config.getServerRoot()).thenReturn(URI.create("https://server/"));
        when(config.getReadTimeout()).thenReturn(5000);
        when(config.getAgent()).thenReturn("AGENT");
        when(config.generateResourceId()).thenReturn("abcdef");
        if (mode.equals(WAC)) {
            when(config.getAccessControlMode()).thenReturn(WAC);
        } else {
            when(config.getAccessControlMode()).thenReturn(ACP);
        }
        final TestCredentials credentials = new TestCredentials();
        credentials.webId = "https://alice.target.example.org/profile/card#me";
        when(config.getCredentials(HttpConstants.ALICE)).thenReturn(credentials);
        final Client mockClient = mock(Client.class);
        when(clientRegistry.getClient(HttpConstants.ALICE)).thenReturn(mockClient);
        return mockClient;
    }
}
