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
import org.solid.testharness.accesscontrol.AccessControlFactory;
import org.solid.testharness.accesscontrol.AccessDataset;
import org.solid.testharness.accesscontrol.AccessDatasetBuilder;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.http.SolidClient;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class SolidResourceTest {
    private static final String TEST_URL = "http://localhost/resource";
    URI testUrl = URI.create(TEST_URL);
    URI aclUrl = URI.create(testUrl + ".acl");
    SolidClient solidClient;

    @InjectMock
    AccessControlFactory accessControlFactory;

    @BeforeEach
    void setUp() {
        solidClient = mock(SolidClient.class);
    }

    @Test
    void testConstructorExceptions() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                new SolidResource(null, null, null, null)
        );
        assertEquals("Parameter solidClient is required", exception.getMessage());
        exception = assertThrows(IllegalArgumentException.class, () ->
                new SolidResource(solidClient, null, null, null)
        );
        assertEquals("Parameter url is required", exception.getMessage());
        exception = assertThrows(IllegalArgumentException.class, () ->
                new SolidResource(solidClient, "", null, null)
        );
        assertEquals("Parameter url is required", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () ->
                new SolidResource(solidClient, "test", null, null)
        );
        assertEquals("The url must be absolute", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () ->
                new SolidResource(solidClient, TEST_URL, "", null)
        );
        assertEquals("Parameter type is required", exception.getMessage());
    }

    @Test
    void testConstructorNoBody() {
        final SolidResource resource = new SolidResource(solidClient, testUrl.toString());
        assertTrue(resource.exists());
    }

    @Test
    void testCreate() {
        final SolidResource resource = SolidResource.create(solidClient, testUrl.toString(), "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertTrue(resource.exists());
    }

    @Test
    void resourceExistsWithAcl() throws Exception {
        final Map<String, List<String>> headerMap = Map.of(HttpConstants.HEADER_LINK,
                List.of("<" + aclUrl.toString() + ">; rel=\"acl\""));
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(headers);
        when(solidClient.getAclUri(headers)).thenReturn(aclUrl);
        when(solidClient.getAclUri(testUrl)).thenReturn(aclUrl);

        final SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);

        assertTrue(resource.exists());
        assertEquals(aclUrl.toString(), resource.getAclUrl());
    }

    @Test
    void resourceExistsWithoutAcl() throws Exception {
        final Map<String, List<String>> headerMap = Map.of(HttpConstants.HEADER_LINK,
                List.of("<" + aclUrl + ">; rel=\"noacl\""));
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(headers);
        when(solidClient.getAclUri(testUrl)).thenReturn(null);

        final SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);

        assertTrue(resource.exists());
        assertNull(resource.getAclUrl());
    }

    @Test
    void resourceExistsNoLink() throws Exception {
        final Map<String, List<String>> headerMap = Collections.emptyMap();
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(headers);

        final SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);

        assertTrue(resource.exists());
        assertNull(resource.getAclUrl());
    }

    @Test
    void resourceExistsWithAclAfterLookup() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(null);
        final URI lookedUpAclUrl = URI.create("http://localhost/lookupedUpAcl");
        when(solidClient.getAclUri(testUrl)).thenReturn(lookedUpAclUrl);

        final SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);

        assertTrue(resource.exists());
        assertEquals(lookedUpAclUrl.toString(), resource.getAclUrl());
    }

    @Test
    void resourceNotExists() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenThrow(new Exception());

        final SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);

        assertFalse(resource.exists());
    }

    @Test
    void getContainer() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(null);

        final SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        final SolidContainer container = resource.getContainer();

        assertTrue(resource.exists());
        assertFalse(resource.isContainer());
        assertTrue(container.exists());
        assertEquals("http://localhost/", container.getUrl());
        assertEquals("/", container.getPath());
    }

    @Test
    void getParent() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(null);

        final SolidResource resource = new SolidResource(solidClient, "http://localhost/container/resource",
                "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        final SolidContainer container = resource.getContainer();
        final SolidContainer nextContainer = container.getContainer();

        assertTrue(resource.exists());
        assertTrue(container.exists());
        assertEquals("http://localhost/container/", container.getUrl());
        assertEquals("/container/", container.getPath());
        assertTrue(nextContainer.exists());
        assertEquals("http://localhost/", nextContainer.getUrl());
        assertEquals("/", nextContainer.getPath());
    }

    @Test
    void getTopLevelContainer() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(null);

        final SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        final SolidContainer container = resource.getContainer();
        final SolidContainer nextContainer = container.getContainer();

        assertTrue(resource.exists());
        assertTrue(container.exists());
        assertEquals("http://localhost/", container.getUrl());
        assertEquals("/", container.getPath());
        assertNull(nextContainer);
    }

    @Test
    void setAccessDatasetNoUrl() throws Exception {
        final AccessDataset accessDataset = mock(AccessDataset.class);
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(null);
        final SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertFalse(resource.setAccessDataset(accessDataset));
    }

    @Test
    void setAccessDatasetMissing() throws Exception {
        final AccessDataset accessDataset = mock(AccessDataset.class);
        final Map<String, List<String>> headerMap = Map.of(HttpConstants.HEADER_LINK,
                List.of("<" + aclUrl.toString() + ">; rel=\"acl\""));
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(headers);
        when(solidClient.getAclUri(headers)).thenReturn(null);
        when(solidClient.getAclUri(testUrl)).thenReturn(null);

        final SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertFalse(resource.setAccessDataset(accessDataset));
        // second attempt short cuts as false
        assertFalse(resource.setAccessDataset(accessDataset));
    }

    @Test
    void setAccessDataset() throws Exception {
        final AccessDataset accessDataset = mock(AccessDataset.class);
        final Map<String, List<String>> headerMap = Map.of(HttpConstants.HEADER_LINK,
                List.of("<" + aclUrl.toString() + ">; rel=\"acl\""));
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(headers);
        when(solidClient.getAclUri(headers)).thenReturn(aclUrl);
        when(solidClient.getAclUri(testUrl)).thenReturn(aclUrl);
        when(solidClient.createAcl(aclUrl, accessDataset)).thenReturn(true);

        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(headers);
        final SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertTrue(resource.setAccessDataset(accessDataset));
    }

    @Test
    void getUrl() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(null);
        final SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertEquals(TEST_URL, resource.getUrl());
        assertEquals("/resource", resource.getPath());
    }

    @Test
    void getUrlNull() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenThrow(new Exception("Failed as expected"));
        final SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertFalse(resource.exists());
        assertNull(resource.getUrl());
        assertNull(resource.getPath());
    }

    @Test
    void getAclUrlMissing() throws Exception {
        final Map<String, List<String>> headerMap = Map.of(HttpConstants.HEADER_LINK,
                List.of("<" + aclUrl.toString() + ">; rel=\"acl\""));
        final HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(headers);
        when(solidClient.getAclUri(headers)).thenReturn(null);
        when(solidClient.getAclUri(testUrl)).thenReturn(null);

        final SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertNull(resource.getAclUrl());
        // second attempt short cuts
        assertNull(resource.getAclUrl());
    }

    @Test
    void findStorageFromResource() throws Exception {
        when(solidClient.hasStorageType(URI.create("http://localhost/container/resource"))).thenReturn(false);
        when(solidClient.hasStorageType(URI.create("http://localhost/container/"))).thenReturn(false);
        when(solidClient.hasStorageType(URI.create("http://localhost/"))).thenReturn(true);
        final SolidResource resource = new SolidResource(solidClient, "http://localhost/container/resource");
        final SolidResource storageRoot = resource.findStorage();
        assertEquals("http://localhost/", storageRoot.url.toString());
    }

    @Test
    void findStorageFails() throws Exception {
        when(solidClient.hasStorageType(URI.create("http://localhost/container/resource"))).thenReturn(false);
        when(solidClient.hasStorageType(URI.create("http://localhost/container/"))).thenReturn(false);
        when(solidClient.hasStorageType(URI.create("http://localhost/"))).thenReturn(false);
        final SolidResource resource = new SolidResource(solidClient, "http://localhost/container/resource");
        final SolidResource storageRoot = resource.findStorage();
        assertNull(storageRoot);
    }

    @Test
    void hasStorageType() throws Exception {
        when(solidClient.hasStorageType(testUrl)).thenReturn(true);
        final SolidResource resource = new SolidResource(solidClient, testUrl.toString());
        assertTrue(resource.hasStorageType());
    }

    @Test
    void getContentAsTurtle() throws Exception {
        when(solidClient.getContentAsTurtle(testUrl)).thenReturn("CONTENT");
        final SolidResource resource = new SolidResource(solidClient, testUrl.toString());
        assertEquals("CONTENT", resource.getContentAsTurtle());
    }

    @Test
    void getContentAsTurtleEmpty() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenThrow(new Exception("FAIL"));
        final SolidResource resource = new SolidResource(solidClient, testUrl.toString(),"hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertEquals("", resource.getContentAsTurtle());
    }

    @Test
    void getAccessDatasetBuilder() throws Exception {
        when(solidClient.getAclUri(testUrl)).thenReturn(aclUrl);
        final AccessDatasetBuilder accessDatasetBuilder = mock(AccessDatasetBuilder.class);
        when(accessControlFactory.getAccessDatasetBuilder(aclUrl.toString())).thenReturn(accessDatasetBuilder);
        final SolidResource resource = new SolidResource(solidClient, testUrl.toString());
        final AccessDatasetBuilder builder = resource.getAccessDatasetBuilder(
                "https://alice.target.example.org/profile/card#me");
        assertNotNull(builder);
        verify(accessDatasetBuilder, times(1)).setOwnerAccess(any(), any());
    }

    @Test
    void getAccessDataset() throws Exception {
        final AccessDataset accessDataset = mock(AccessDataset.class);
        when(solidClient.getAclUri(testUrl)).thenReturn(aclUrl);
        when(solidClient.getAcl(aclUrl)).thenReturn(accessDataset);
        final SolidResource resource = new SolidResource(solidClient, testUrl.toString());
        assertEquals(accessDataset, resource.getAccessDataset());
    }

    @Test
    void getAccessDatasetMissing() throws Exception {
        when(solidClient.getAclUri(testUrl)).thenReturn(null);
        final SolidResource resource = new SolidResource(solidClient, testUrl.toString());
        assertEquals(null, resource.getAccessDataset());
    }

    @Test
    void delete() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(null);
        final SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertDoesNotThrow(() -> resource.delete());
    }

    @Test
    void deleteException() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(null);
        final SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        doThrow(new Exception("Failed as expected")).when(solidClient).deleteResourceRecursively(testUrl);
        assertThrows(Exception.class, () -> resource.delete());
    }

    @Test
    void resourceToString() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN)).thenReturn(null);
        final SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertEquals("SolidResource: " + TEST_URL, resource.toString());
    }

    @Test
    void emptyResourceToString() throws Exception {
        when(solidClient.createResource(testUrl, "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN))
                .thenThrow(new Exception("Failed as expected"));
        final SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertEquals("SolidResource: null", resource.toString());
    }

    @Test
    void containerToString() throws Exception {
        when(solidClient.createResource(new URI("http://localhost/container/"), null, null))
                .thenReturn(null);
        final SolidResource resource = new SolidResource(solidClient, "http://localhost/container/", null, null);
        assertEquals("SolidContainer: http://localhost/container/", resource.toString());
    }
}
