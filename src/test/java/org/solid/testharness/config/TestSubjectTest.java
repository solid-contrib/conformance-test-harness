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
package org.solid.testharness.config;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.junit.jupiter.api.Test;
import org.solid.common.vocab.PIM;
import org.solid.testharness.http.Client;
import org.solid.testharness.http.ClientRegistry;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.http.SolidClientProvider;
import org.solid.testharness.utils.SolidContainerProvider;
import org.solid.testharness.utils.TestHarnessInitializationException;
import org.solid.testharness.utils.TestUtils;

import jakarta.inject.Inject;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.solid.testharness.config.TestSubject.AccessControlMode.ACP;
import static org.solid.testharness.config.TestSubject.AccessControlMode.WAC;

@QuarkusTest
class TestSubjectTest {
    private static final URI TEST_URL = URI.create("https://localhost/container/");
    private static final String ALICE_WEBID = "https://alice.target.example.org/profile/card#me";
    private static final String BOB_WEBID = "https://bob.target.example.org/profile/card#me";
    private static final String CONFIG_SAMPLE = "src/test/resources/config/config-sample.ttl";
    private static final String CONFIG_SAMPLE_SINGLE = "src/test/resources/config/config-sample-single.ttl";
    private static final Map<String, List<String>> STORAGE_HEADER = Map.of(HttpConstants.HEADER_LINK,
            List.of("<" + PIM.Storage.toString() + ">; rel=\"type\""));
    private static final String SERVER_TEST_STORAGE = "https://server/test/";
    private static final String SERVER_TEST_ACL = SERVER_TEST_STORAGE + ".acl";
    private static final Map<String, List<String>> ACL_HEADER = Map.of(HttpConstants.HEADER_LINK,
            List.of("<" + SERVER_TEST_ACL + ">; rel=\"acl\""));

    @InjectMock
    Config config;

    @Inject
    TestSubject testSubject;

    @InjectMock
    ClientRegistry clientRegistry;

    @Test
    void setupMissingTarget() throws Exception {
        setupMockConfigMin(CONFIG_SAMPLE, null);
        assertThrows(TestHarnessInitializationException.class, () -> testSubject.loadTestSubjectConfig());
    }

    @Test
    void setupMissingTargetSingleConfig() throws Exception {
        final URL testFileUrl = TestUtils.getFileUrl(CONFIG_SAMPLE_SINGLE);
        setupMockConfigMin(CONFIG_SAMPLE_SINGLE, null);
        testSubject.loadTestSubjectConfig();

        final TargetServer targetServer = testSubject.getTargetServer();

        assertNotNull(targetServer);
        assertEquals(testFileUrl.toURI().resolve("default").toString(), targetServer.getSubject());
        assertEquals(12, targetServer.size());
    }

    @Test
    void setupTargetMultipleConfig() throws Exception {
        final URL testFileUrl = TestUtils.getFileUrl(CONFIG_SAMPLE);
        final String subject = testFileUrl.toURI().resolve("testserver").toString();
        setupMockConfigMin(CONFIG_SAMPLE, subject);
        testSubject.loadTestSubjectConfig();

        final TargetServer targetServer = testSubject.getTargetServer();

        assertNotNull(targetServer);
        assertEquals(subject, targetServer.getSubject());
        assertEquals(14, targetServer.size());
    }

    @Test
    void setupDifferentTargetSingleConfig() throws Exception {
        setupMockConfigMin(CONFIG_SAMPLE_SINGLE,
                "https://github.com/solid-contrib/conformance-test-harness/missing");

        assertThrows(TestHarnessInitializationException.class, () -> testSubject.loadTestSubjectConfig());
    }

    @Test
    void setupConfigWithoutServer() throws Exception {
        setupMockConfigMin("src/test/resources/config/harness-sample.ttl", null);

        assertThrows(TestHarnessInitializationException.class, () -> testSubject.loadTestSubjectConfig());
    }

    @Test
    void setupMissingConfig() throws Exception {
        setupMockConfigMin("jsonld-sample.json", null);

        assertThrows(TestHarnessInitializationException.class, () -> testSubject.loadTestSubjectConfig());
    }

    @Test
    void setupBadConfig() throws Exception {
        setupMockConfigMin("src/test/resources/jsonld-sample.json", null);

        assertThrows(TestHarnessInitializationException.class, () -> testSubject.loadTestSubjectConfig());
    }

    @Test
    void prepareServerWithoutServer() throws Exception {
        setupMockConfigMin(null, null);

        testSubject.setTargetServer(null);

        assertThrows(TestHarnessInitializationException.class, () -> testSubject.prepareServer());
    }

    @Test
    void prepareServerAccessControlModeNoAcl() {
        final Client mockClient = setupMockConfig(WAC, null);
        final HttpResponse<Void> mockVoidResponseNoLink = TestUtils.mockVoidResponse(200);
        when(mockClient.head(URI.create(SERVER_TEST_STORAGE))).thenReturn(mockVoidResponseNoLink);

        final Exception exception = assertThrows(TestHarnessInitializationException.class,
                () -> testSubject.prepareServer());

        assertTrue(exception.getMessage().contains("Failed to prepare server"));
        assertTrue(exception.getMessage().contains("Cannot get ACL url for root test container: https://server/test/"));
    }

    @Test
    void prepareServerAccessControlModeThrows() {
        final Client mockClient = setupMockConfig(WAC, null);
        final HttpResponse<Void> mockVoidResponseLink = TestUtils.mockVoidResponse(200, ACL_HEADER);
        when(mockClient.head(URI.create(SERVER_TEST_STORAGE))).thenReturn(mockVoidResponseLink);
        when(mockClient.head(URI.create(SERVER_TEST_ACL))).thenThrow(TestUtils.createException("FAIL"));

        final Exception exception = assertThrows(TestHarnessInitializationException.class,
                () -> testSubject.prepareServer());

        assertTrue(exception.getMessage().contains("Failed to prepare server"));
        assertTrue(exception.getMessage().contains("FAIL"));
    }

    @Test
    void prepareServerWacMode() {
        final Client mockClient = setupMockConfig(WAC, null);
        final HttpResponse<String> mockStringResponse = TestUtils.mockStringResponse(200, "");
        when(mockClient.getAsTurtle(any())).thenReturn(mockStringResponse);
        doReturn(mockStringResponse).when(mockClient).sendAuthorized(eq(null), any(), any());
        final HttpResponse<Void> mockVoidResponse = TestUtils.mockVoidResponse(200, ACL_HEADER);
        when(mockClient.head(any())).thenReturn(mockVoidResponse);
        when(mockClient.put(eq(URI.create(SERVER_TEST_ACL)), any(), eq(HttpConstants.MEDIA_TYPE_TEXT_TURTLE)))
                .thenReturn(mockVoidResponse);

        assertDoesNotThrow(() -> testSubject.prepareServer());

        assertEquals(WAC, testSubject.getAccessControlMode());
        assertNotNull(testSubject.getTestRunContainer());
    }

    @Test
    void prepareServerAcpMode() {
        final Client mockClient = setupMockConfig(ACP, null);

        final HttpResponse<String> mockStringResponse = TestUtils.mockStringResponse(200, "");
        when(mockClient.getAsTurtle(any())).thenReturn(mockStringResponse);
        doReturn(mockStringResponse).when(mockClient).sendAuthorized(eq(null), any(), any());
        final HttpResponse<Void> mockVoidResponse = TestUtils.mockVoidResponse(200, Map.of(HttpConstants.HEADER_LINK,
                List.of("<" + SERVER_TEST_ACL + ">; rel=\"acl\"",
                        "<http://www.w3.org/ns/solid/acp#AccessControlResource>; rel=\"type\"")));
        when(mockClient.head(any())).thenReturn(mockVoidResponse);
        when(mockClient.patch(eq(URI.create(SERVER_TEST_ACL)), any(),
                eq(HttpConstants.MEDIA_TYPE_APPLICATION_SPARQL_UPDATE))).thenReturn(mockStringResponse);

        assertDoesNotThrow(() -> testSubject.prepareServer());

        assertEquals(ACP, testSubject.getAccessControlMode());
        assertNotNull(testSubject.getTestRunContainer());
    }

    @Test
    void prepareServerThrows() {
        final Client mockClient = setupMockConfig(WAC, null);

        final HttpResponse<Void> mockVoidResponseLink = TestUtils.mockVoidResponse(200, ACL_HEADER);
        when(mockClient.head(URI.create(SERVER_TEST_STORAGE))).thenReturn(mockVoidResponseLink);
        when(mockClient.head(URI.create(SERVER_TEST_ACL))).thenReturn(mockVoidResponseLink);

        when(mockClient.getAsTurtle(any())).thenThrow(TestUtils.createException("FAIL"));

        final Exception exception = assertThrows(TestHarnessInitializationException.class,
                () -> testSubject.prepareServer());
        assertTrue(exception.getMessage().contains("Failed to prepare server"));
        assertTrue(exception.getMessage().contains("FAIL"));
    }

    @Test
    void prepareServerAclTestFails() {
        final Client mockClient = setupMockConfig(ACP, null);

        final HttpResponse<String> mockStringResponse = TestUtils.mockStringResponse(200, "");
        when(mockClient.getAsTurtle(any())).thenReturn(mockStringResponse);
        doReturn(mockStringResponse).when(mockClient).sendAuthorized(eq(null), any(), any());
        final HttpResponse<Void> mockVoidResponse = TestUtils.mockVoidResponse(200, Map.of(HttpConstants.HEADER_LINK,
                List.of("<" + SERVER_TEST_ACL + ">; rel=\"acl\"",
                        "<http://www.w3.org/ns/solid/acp#AccessControlResource>; rel=\"type\"")));
        when(mockClient.head(any())).thenReturn(mockVoidResponse);
        final HttpResponse<String> mockStringFailResponse = TestUtils.mockStringResponse(400, "");
        when(mockClient.patch(eq(URI.create(SERVER_TEST_ACL)), any(), any())).thenReturn(mockStringFailResponse);

        final Exception exception = assertThrows(TestHarnessInitializationException.class,
                () -> testSubject.prepareServer());

        assertTrue(exception.getMessage().contains("Failed to prepare server"));
        assertTrue(exception.getCause().getMessage().contains("Failed to create a container"));
        assertTrue(exception.getCause().getCause().getMessage().contains("Error response=400 trying to apply ACL"));
    }

    @Test
    void prepareServerContainerFails() {
        final Client mockClient = setupMockConfig(WAC, null);
        final HttpResponse<Void> mockVoidResponseLink = TestUtils.mockVoidResponse(200, ACL_HEADER);
        when(mockClient.head(URI.create(SERVER_TEST_STORAGE))).thenReturn(mockVoidResponseLink);
        when(mockClient.head(URI.create(SERVER_TEST_ACL))).thenReturn(mockVoidResponseLink);

        final HttpResponse<String> mockStringResponse = TestUtils.mockStringResponse(200, "");
        when(mockClient.getAsTurtle(any())).thenReturn(mockStringResponse);
        when(mockClient.sendAuthorized(eq(null), any(), any())).thenThrow(TestUtils.createException("FAIL"));

        final Exception exception = assertThrows(TestHarnessInitializationException.class,
                () -> testSubject.prepareServer());

        assertTrue(exception.getMessage().contains("Failed to prepare server"));
        assertTrue(exception.getMessage().contains("FAIL"));
    }

    @Test
    void findTestContainerProfileBased() throws Exception {
        final Client ownerClient = setupMockConfig(null, List.of("/storage1/"));
        final HttpResponse<Void> mockResponse = TestUtils.mockVoidResponse(204, STORAGE_HEADER);
        when(ownerClient.head(any())).thenReturn(mockResponse);
        when(config.getTestContainer()).thenReturn("");

        assertEquals(URI.create("https://example.org/storage1/"), testSubject.findTestContainer());
    }

    @Test
    void findTestContainerAbsolute() throws Exception {
        setupMockConfig(null, null);
        when(config.getTestContainer()).thenReturn(SERVER_TEST_STORAGE);

        assertEquals(URI.create(SERVER_TEST_STORAGE), testSubject.findTestContainer());
    }

    @Test
    void findTestContainerRootRelative() throws Exception {
        setupMockConfig(null, null);
        when(config.getServerRoot()).thenReturn("https://server/");
        when(config.getTestContainer()).thenReturn("/test/");

        assertEquals(URI.create(SERVER_TEST_STORAGE), testSubject.findTestContainer());
    }

    @Test
    void findTestContainerProfileRelative() throws Exception {
        final Client ownerClient = setupMockConfig(null, List.of("/storage1/"));
        final HttpResponse<Void> mockResponse = TestUtils.mockVoidResponse(204, STORAGE_HEADER);
        when(ownerClient.head(any())).thenReturn(mockResponse);
        when(config.getServerRoot()).thenReturn("");
        when(config.getTestContainer()).thenReturn("test/");

        assertEquals(URI.create("https://example.org/storage1/test/"), testSubject.findTestContainer());
    }

    @Test
    void findStorageProfile() throws Exception {
        final Client ownerClient = setupMockConfig(null,
                List.of("/badStorage/", "/missingStorage/", "/notStorage/", "/storage/"));
        final HttpResponse<Void> mockMissingResponse = TestUtils.mockVoidResponse(404);
        final HttpResponse<Void> mockNoHeaderResponse = TestUtils.mockVoidResponse(204);
        final HttpResponse<Void> mockGoodResponse = TestUtils.mockVoidResponse(204, STORAGE_HEADER);
        when(ownerClient.head(any())).thenThrow(TestUtils.createException("BAD POD"))
                .thenReturn(mockMissingResponse)
                .thenReturn(mockNoHeaderResponse)
                .thenReturn(mockGoodResponse);

        final URI storage = testSubject.findStorage();

        assertEquals("/storage/", storage.getPath());
    }

    @Test
    void findStorageProfileException() {
        final Client webIdClient = mock(Client.class);
        when(webIdClient.getAsTurtle(any())).thenThrow(TestUtils.createException("FAIL"));
        when(clientRegistry.getClient(ClientRegistry.ALICE_WEBID)).thenReturn(webIdClient);
        setupMockConfig(null, null);

        final Exception exception = assertThrows(TestHarnessInitializationException.class,
                () -> testSubject.findStorage());

        assertTrue(exception.getMessage().contains("Failed to read WebID Document for [" + ALICE_WEBID));
    }

    @Test
    void findStorageNoReferences() {
        setupMockConfig(null, Collections.emptyList());

        final Exception exception = assertThrows(TestHarnessInitializationException.class,
                () -> testSubject.findStorage());

        assertTrue(exception.getMessage().contains("No Pod references found in the WebID Document"));
    }

    @Test
    void findStorageProvisionFails() {
        final Client ownerClient = setupMockConfig(null, List.of("/missingStorage/"));
        final HttpResponse<Void> mockMissingResponse = TestUtils.mockVoidResponse(404);
        when(ownerClient.head(any())).thenReturn(mockMissingResponse);

        final Exception exception = assertThrows(TestHarnessInitializationException.class,
                () -> testSubject.findStorage());

        assertTrue(exception.getMessage().contains("No accessible Pods were found for test user"));
    }

    @Test
    void loadTestSubjectConfigTarget1() throws Exception {
        final URL testFileUrl = TestUtils.getFileUrl(CONFIG_SAMPLE);
        final String subject = testFileUrl.toURI().resolve("testserver").toString();
        when(config.getSubjectsUrl()).thenReturn(testFileUrl);
        when(config.getTestSubject()).thenReturn(iri(subject));
        testSubject.loadTestSubjectConfig();

        final TargetServer targetServer = testSubject.getTargetServer();

        assertNotNull(targetServer);
        assertEquals(subject, targetServer.getSubject());
    }

    @Test
    void loadTestSubjectConfigTarget2() throws Exception {
        final URL testFileUrl = TestUtils.getFileUrl(CONFIG_SAMPLE);
        final String subject = testFileUrl.toURI().resolve("testserver2").toString();
        when(config.getSubjectsUrl()).thenReturn(testFileUrl);
        when(config.getTestSubject()).thenReturn(iri(subject));
        testSubject.loadTestSubjectConfig();

        final TargetServer targetServer = testSubject.getTargetServer();

        assertNotNull(targetServer);
        assertEquals(subject, targetServer.getSubject());
    }

    @Test
    void getTargetServerDefault() throws Exception {
        final URL testFileUrl = TestUtils.getFileUrl(CONFIG_SAMPLE_SINGLE);
        when(config.getSubjectsUrl()).thenReturn(testFileUrl);
        testSubject.loadTestSubjectConfig();

        final TargetServer targetServer = testSubject.getTargetServer();

        assertNotNull(targetServer);
        assertEquals(testFileUrl.toURI().resolve("default").toString(), targetServer.getSubject());
    }

    @Test
    void tearDownServer() throws Exception {
        final SolidClientProvider mockSolidClientProvider = mock(SolidClientProvider.class);
        testSubject.setTestRunContainer(new SolidContainerProvider(mockSolidClientProvider, TEST_URL));

        assertDoesNotThrow(() -> testSubject.tearDownServer());

        verify(mockSolidClientProvider).deleteResourceRecursively(TEST_URL);
    }

    @Test
    void tearDownServerNoContainer() {
        testSubject.setTestRunContainer(null);

        assertDoesNotThrow(() -> testSubject.tearDownServer());
    }

    @Test
    void tearDownServerFails() throws Exception {
        final SolidClientProvider mockSolidClientProvider = mock(SolidClientProvider.class);
        testSubject.setTestRunContainer(new SolidContainerProvider(mockSolidClientProvider, TEST_URL));
        doThrow(TestUtils.createException("FAIL")).when(mockSolidClientProvider).deleteResourceRecursively(any());

        assertDoesNotThrow(() -> testSubject.tearDownServer());

        verify(mockSolidClientProvider).deleteResourceRecursively(TEST_URL);
    }

    private void setupMockConfigMin(final String subjectsFile, final String subject) throws Exception {
        when(config.getWebIds()).thenReturn(Map.of(HttpConstants.ALICE, ALICE_WEBID));
        if (subjectsFile != null) {
            when(config.getSubjectsUrl()).thenReturn(TestUtils.getFileUrl(subjectsFile));
        }
        if (subject != null) {
            when(config.getTestSubject()).thenReturn(iri(subject));
        }
    }

    private Client setupMockConfig(final TestSubject.AccessControlMode mode, final List<String> storageList) {
        when(config.getWebIds()).thenReturn(Map.of(HttpConstants.ALICE, ALICE_WEBID, HttpConstants.BOB, BOB_WEBID));
        when(config.getTestContainer()).thenReturn("/test/");
        when(config.getServerRoot()).thenReturn("https://server/");
        when(config.getReadTimeout()).thenReturn(5000);
        when(config.getAgent()).thenReturn("AGENT");
        when(config.generateResourceId()).thenReturn("abcdef");
        final TestCredentials credentials = new TestCredentials();
        credentials.webId = ALICE_WEBID;
        when(config.getCredentials(HttpConstants.ALICE)).thenReturn(credentials);
        // register webid client
        final Client webIdClient = mock(Client.class);
        if (storageList != null) {
            final ModelBuilder builder = new ModelBuilder().subject(ALICE_WEBID);
            storageList.forEach(s -> builder.add(
                    PIM.storage, iri(URI.create(TestUtils.SAMPLE_BASE).resolve(s).toString()))
            );
            final Model model = builder.build();
            final HttpResponse<String> turtleResponse = TestUtils.mockStringResponse(200, TestUtils.toTurtle(model));
            when(webIdClient.getAsTurtle(any())).thenReturn(turtleResponse);
        }
        when(clientRegistry.getClient(ClientRegistry.ALICE_WEBID)).thenReturn(webIdClient);
        // register owner client
        final Client mockClient = mock(Client.class);
        when(mockClient.getUser()).thenReturn(HttpConstants.ALICE);
        when(clientRegistry.getClient(HttpConstants.ALICE)).thenReturn(mockClient);
        if (mode != null) {
            final TargetServer targetServer = mock(TargetServer.class);
            testSubject.setTargetServer(targetServer);
        }
        return mockClient;
    }
}
