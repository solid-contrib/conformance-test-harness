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
package org.solid.testharness.utils;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.testharness.accesscontrol.AccessDataset;
import org.solid.testharness.accesscontrol.AccessDatasetBuilder;
import org.solid.testharness.config.Config;
import org.solid.testharness.http.Client;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.http.SolidClientProvider;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class SolidResourceProviderTest {
    private static final URI ROOT_URL = URI.create("https://example.org/");
    private static final URI TEST_URL = ROOT_URL.resolve("resource");
    private static final URI TEST_ACL_URL = URI.create(TEST_URL + ".acl");
    SolidClientProvider solidClientProvider;

    @InjectMock
    Config config;

    @BeforeEach
    void setUp() {
        solidClientProvider = mock(SolidClientProvider.class);
    }

    @Test
    void testConstructorExceptions() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                new SolidResourceProvider(null, null, null, null)
        );
        assertEquals("Parameter solidClientProvider is required", exception.getMessage());
        exception = assertThrows(IllegalArgumentException.class, () ->
                new SolidResourceProvider(solidClientProvider, null, null, null)
        );
        assertEquals("Parameter url is required", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () ->
                new SolidResourceProvider(solidClientProvider, URI.create("test"), null, null)
        );
        assertEquals("The url must be absolute", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () ->
                new SolidResourceProvider(solidClientProvider, TEST_URL, "", null)
        );
        assertEquals("Parameter type is required", exception.getMessage());
    }

    @Test
    void testConstructorNoBody() {
        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider, TEST_URL);
        assertEquals(TEST_URL, resource.getUrl());
    }

    @Test
    void resourceExistsWithAcl() throws Exception {
        final Map<String, List<String>> headerMap = Map.of(HttpConstants.HEADER_LINK,
                List.of("<" + TEST_ACL_URL + ">; rel=\"acl\""));
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClientProvider.createResource(TEST_URL, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenReturn(headers);
        when(solidClientProvider.getAclUri(headers)).thenReturn(TEST_ACL_URL);
        when(solidClientProvider.getAclUri(TEST_URL)).thenReturn(TEST_ACL_URL);

        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);

        assertEquals(TEST_URL, resource.getUrl());
        assertEquals(TEST_ACL_URL, resource.getAclUrl());
    }

    @Test
    void resourceExistsWithoutAcl() throws Exception {
        final Map<String, List<String>> headerMap = Map.of(HttpConstants.HEADER_LINK,
                List.of("<" + TEST_ACL_URL + ">; rel=\"noacl\""));
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClientProvider.createResource(TEST_URL, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenReturn(headers);
        when(solidClientProvider.getAclUri(TEST_URL)).thenReturn(null);

        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);

        assertEquals(TEST_URL, resource.getUrl());
        assertNull(resource.getAclUrl());
    }

    @Test
    void resourceExistsNoLink() throws Exception {
        final Map<String, List<String>> headerMap = Collections.emptyMap();
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClientProvider.createResource(TEST_URL, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenReturn(headers);

        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);

        assertEquals(TEST_URL, resource.getUrl());
        assertNull(resource.getAclUrl());
    }

    @Test
    void resourceExistsWithAclAfterLookup() throws Exception {
        when(solidClientProvider.createResource(TEST_URL, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenReturn(null);
        final URI lookedUpAclUrl = ROOT_URL.resolve("lookupedUpAcl");
        when(solidClientProvider.getAclUri(TEST_URL)).thenReturn(lookedUpAclUrl);

        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);

        assertEquals(TEST_URL, resource.getUrl());
        assertEquals(lookedUpAclUrl, resource.getAclUrl());
    }

    @Test
    void resourceNotExists() throws Exception {
        when(solidClientProvider.createResource(TEST_URL, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenThrow(new Exception());

        assertThrows(Exception.class, () -> new SolidResourceProvider(solidClientProvider, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN)
        );
    }

    @Test
    void getContainer() throws Exception {
        when(solidClientProvider.createResource(TEST_URL, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenReturn(null);

        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        final SolidContainerProvider container = resource.getContainer();

        assertEquals(TEST_URL, resource.getUrl());
        assertFalse(resource.isContainer());
        assertEquals(ROOT_URL, container.getUrl());
    }

    @Test
    void getParent() throws Exception {
        when(solidClientProvider.createResource(TEST_URL, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenReturn(null);

        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider,
                ROOT_URL.resolve("/container/resource"), "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        final SolidContainerProvider container = resource.getContainer();
        final SolidContainerProvider nextContainer = container.getContainer();

        assertEquals(ROOT_URL.resolve("/container/resource"), resource.getUrl());
        assertEquals(ROOT_URL.resolve("/container/"), container.getUrl());
        assertEquals(ROOT_URL, nextContainer.getUrl());
    }

    @Test
    void getTopLevelContainer() throws Exception {
        when(solidClientProvider.createResource(TEST_URL, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenReturn(null);

        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        final SolidContainerProvider container = resource.getContainer();
        final SolidContainerProvider nextContainer = container.getContainer();

        assertEquals(TEST_URL, resource.getUrl());
        assertEquals(ROOT_URL, container.getUrl());
        assertNull(nextContainer);
    }

    @Test
    void setAccessDatasetNoUrl() throws Exception {
        final AccessDataset accessDataset = mock(AccessDataset.class);
        when(solidClientProvider.createResource(TEST_URL, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenReturn(null);
        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertThrows(Exception.class, () -> resource.setAccessDataset(accessDataset));
    }

    @Test
    void setAccessDatasetMissing() throws Exception {
        final AccessDataset accessDataset = mock(AccessDataset.class);
        final Map<String, List<String>> headerMap = Map.of(HttpConstants.HEADER_LINK,
                List.of("<" + TEST_ACL_URL + ">; rel=\"acl\""));
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClientProvider.createResource(TEST_URL, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenReturn(headers);
        when(solidClientProvider.getAclUri(headers)).thenReturn(null);
        when(solidClientProvider.getAclUri(TEST_URL)).thenReturn(null);

        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertThrows(Exception.class, () -> resource.setAccessDataset(accessDataset));
    }

    @Test
    void setAccessDataset() throws Exception {
        final AccessDataset accessDataset = mock(AccessDataset.class);
        final Map<String, List<String>> headerMap = Map.of(HttpConstants.HEADER_LINK,
                List.of("<" + TEST_ACL_URL + ">; rel=\"acl\""));
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClientProvider.createResource(TEST_URL, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenReturn(headers);
        when(solidClientProvider.getAclUri(headers)).thenReturn(TEST_ACL_URL);
        when(solidClientProvider.getAclUri(TEST_URL)).thenReturn(TEST_ACL_URL);

        when(solidClientProvider.createResource(TEST_URL, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenReturn(headers);
        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertDoesNotThrow(() -> resource.setAccessDataset(accessDataset));
    }

    @Test
    void getUrl() throws Exception {
        when(solidClientProvider.createResource(TEST_URL, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenReturn(null);
        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertEquals(TEST_URL, resource.getUrl());
    }

    @Test
    void getUrlNull() throws Exception {
        when(solidClientProvider.createResource(TEST_URL, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenThrow(new Exception("Failed as expected"));
        assertThrows(Exception.class, () -> new SolidResourceProvider(solidClientProvider, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN)
        );
    }

    @Test
    void getAclUrlMissing() throws Exception {
        final Map<String, List<String>> headerMap = Map.of(HttpConstants.HEADER_LINK,
                List.of("<" + TEST_ACL_URL + ">; rel=\"acl\""));
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClientProvider.createResource(TEST_URL, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenReturn(headers);
        when(solidClientProvider.getAclUri(headers)).thenReturn(null);
        when(solidClientProvider.getAclUri(TEST_URL)).thenReturn(null);

        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertNull(resource.getAclUrl());
        // second attempt short cuts
        assertNull(resource.getAclUrl());
    }

    @Test
    void findStorageFromResource() throws Exception {
        when(solidClientProvider.hasStorageType(ROOT_URL.resolve("/container/resource"))).thenReturn(false);
        when(solidClientProvider.hasStorageType(ROOT_URL.resolve("/container/"))).thenReturn(false);
        when(solidClientProvider.hasStorageType(ROOT_URL)).thenReturn(true);
        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider,
                ROOT_URL.resolve("/container/resource"));
        final SolidContainerProvider storageRoot = resource.findStorage();
        assertEquals(ROOT_URL, storageRoot.url);
    }

    @Test
    void findStorageFails() throws Exception {
        when(solidClientProvider.hasStorageType(ROOT_URL.resolve("/container/resource"))).thenReturn(false);
        when(solidClientProvider.hasStorageType(ROOT_URL.resolve("/container/"))).thenReturn(false);
        when(solidClientProvider.hasStorageType(ROOT_URL)).thenReturn(false);
        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider,
                ROOT_URL.resolve("/container/resource"));
        final SolidContainerProvider storageRoot = resource.findStorage();
        assertNull(storageRoot);
    }

    @Test
    void hasStorageType() throws Exception {
        when(solidClientProvider.hasStorageType(TEST_URL)).thenReturn(true);
        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider, TEST_URL);
        assertTrue(resource.isStorageType());
    }

    @Test
    void getContentAsTurtle() throws Exception {
        when(solidClientProvider.getContentAsTurtle(TEST_URL)).thenReturn("CONTENT");
        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider, TEST_URL);
        assertEquals("CONTENT", resource.getContentAsTurtle());
    }

    @Test
    void getAccessDatasetBuilder() throws Exception {
        final AccessDatasetBuilder accessDatasetBuilder = mock(AccessDatasetBuilder.class);
        when(solidClientProvider.getAccessDatasetBuilder(any())).thenReturn(accessDatasetBuilder);
        final Client client = mock(Client.class);
        when(client.getUser()).thenReturn(HttpConstants.ALICE);
        when(solidClientProvider.getClient()).thenReturn(client);
        when(config.getWebIds()).thenReturn(Map.of(HttpConstants.ALICE,
                "https://alice.target.example.org/profile/card#me"));
        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider, TEST_URL);
        final AccessDatasetBuilder builder = resource.getAccessDatasetBuilder();
        assertNotNull(builder);
        verify(accessDatasetBuilder, times(1))
                .setOwnerAccess(TEST_URL.toString(), "https://alice.target.example.org/profile/card#me");
    }

    @Test
    void getAccessDataset() throws Exception {
        final AccessDataset accessDataset = mock(AccessDataset.class);
        when(solidClientProvider.getAclUri(TEST_URL)).thenReturn(TEST_ACL_URL);
        when(solidClientProvider.getAcl(TEST_ACL_URL)).thenReturn(accessDataset);
        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider, TEST_URL);
        assertEquals(accessDataset, resource.getAccessDataset());
    }

    @Test
    void getAccessDatasetMissing() throws Exception {
        when(solidClientProvider.getAclUri(TEST_URL)).thenReturn(null);
        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider, TEST_URL);
        assertNull(resource.getAccessDataset());
    }

    @Test
    void delete() throws Exception {
        when(solidClientProvider.createResource(TEST_URL, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenReturn(null);
        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertDoesNotThrow(resource::delete);
    }

    @Test
    void deleteException() throws Exception {
        when(solidClientProvider.createResource(TEST_URL, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenReturn(null);
        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        doThrow(new Exception("Failed as expected")).when(solidClientProvider).deleteResourceRecursively(TEST_URL);
        assertThrows(Exception.class, resource::delete);
    }

    @Test
    void resourceToString() throws Exception {
        when(solidClientProvider.createResource(TEST_URL, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenReturn(null);
        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertEquals("SolidResourceProvider: " + TEST_URL, resource.toString());
    }

    @Test
    void containerToString() throws Exception {
        when(solidClientProvider.createResource(ROOT_URL.resolve("/container/"), null, null))
                .thenReturn(null);
        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider,
                ROOT_URL.resolve("/container/"), null, null);
        assertEquals("SolidContainerProvider: " + ROOT_URL.resolve("/container/"), resource.toString());
    }

    @Test
    void generateId() {
        final SolidResourceProvider resource = new SolidResourceProvider(solidClientProvider, TEST_URL);
        when(config.generateResourceId()).thenReturn("abcdef");
        assertEquals("abcdef", resource.generateId());
    }
}
