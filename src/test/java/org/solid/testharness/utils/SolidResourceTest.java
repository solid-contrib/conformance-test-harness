package org.solid.testharness.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.testharness.http.SolidClient;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SolidResourceTest {
    private static final String TEST_URL = "http://localhost/resource";
    URI testUrl = URI.create(TEST_URL);
    URI aclUrl = URI.create(testUrl + ".acl");
    SolidClient solidClient;

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
        SolidResource resource = new SolidResource(solidClient, testUrl.toString());
        assertTrue(resource.exists());
    }

    @Test
    void testCreate() {
        SolidResource resource = SolidResource.create(solidClient, testUrl.toString(), "hello", "text/plain");
        assertTrue(resource.exists());
    }

    @Test
    void resourceExistsWithAcl() throws Exception {
        Map<String, List<String>> headerMap = Map.of("Link", List.of("<" + aclUrl.toString() + ">; rel=\"acl\""));
        HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenReturn(headers);
        when(solidClient.getAclLink(headers)).thenReturn(aclUrl);
        when(solidClient.getResourceAclLink(testUrl.toString())).thenReturn(aclUrl);

        SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello", "text/plain");

        assertTrue(resource.exists());
        assertEquals(aclUrl.toString(), resource.getAclUrl());
    }

    @Test
    void resourceExistsWithoutAcl() throws Exception {
        Map<String, List<String>> headerMap = Map.of("Link", List.of("<" + aclUrl + ">; rel=\"noacl\""));
        HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenReturn(headers);
        when(solidClient.getResourceAclLink(testUrl.toString())).thenReturn(null);

        SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello", "text/plain");

        assertTrue(resource.exists());
        assertNull(resource.getAclUrl());
    }

    @Test
    void resourceExistsNoLink() throws Exception {
        Map<String, List<String>> headerMap = Collections.emptyMap(); // Map.of("Link", List.of("<"+aclUrl+">; rel=\"noacl\""));
        HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenReturn(headers);

        SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello", "text/plain");

        assertTrue(resource.exists());
        assertNull(resource.getAclUrl());
    }

    @Test
    void resourceExistsWithAclAfterLookup() throws Exception {
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenReturn(null);
        URI lookedUpAclUrl = URI.create("http://localhost/lookupedUpAcl");
        when(solidClient.getResourceAclLink(testUrl.toString())).thenReturn(lookedUpAclUrl);

        SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello", "text/plain");

        assertTrue(resource.exists());
        assertEquals(lookedUpAclUrl.toString(), resource.getAclUrl());
    }

    @Test
    void resourceNotExists() throws Exception {
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenThrow(new Exception());

        SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello", "text/plain");

        assertFalse(resource.exists());
    }

    @Test
    void getContainer() throws Exception {
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenReturn(null);

        SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello", "text/plain");
        SolidContainer container = resource.getContainer();

        assertTrue(resource.exists());
        assertFalse(resource.isContainer());
        assertTrue(container.exists());
        assertEquals("http://localhost/", container.getUrl());
        assertEquals("/", container.getPath());
    }

    @Test
    void getParent() throws Exception {
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenReturn(null);

        SolidResource resource = new SolidResource(solidClient, "http://localhost/container/resource", "hello", "text/plain");
        SolidContainer container = resource.getContainer();
        SolidContainer nextContainer = container.getContainer();

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
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenReturn(null);

        SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello", "text/plain");
        SolidContainer container = resource.getContainer();
        SolidContainer nextContainer = container.getContainer();

        assertTrue(resource.exists());
        assertTrue(container.exists());
        assertEquals("http://localhost/", container.getUrl());
        assertEquals("/", container.getPath());
        assertNull(nextContainer);
    }

    @Test
    void setAclNoUrl() throws Exception {
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenReturn(null);
        SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello", "text/plain");
        assertFalse(resource.setAcl("acl"));
    }

    @Test
    void setAclMissing() throws Exception {
        Map<String, List<String>> headerMap = Map.of("Link", List.of("<" + aclUrl.toString() + ">; rel=\"acl\""));
        HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenReturn(headers);
        when(solidClient.getAclLink(headers)).thenReturn(null);
        when(solidClient.getResourceAclLink(testUrl.toString())).thenReturn(null);

        SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello", "text/plain");
        assertFalse(resource.setAcl("acl"));
        // second attempt short cuts as false
        assertFalse(resource.setAcl("acl"));
    }

    @Test
    void setAcl() throws Exception {
        Map<String, List<String>> headerMap = Map.of("Link", List.of("<" + aclUrl.toString() + ">; rel=\"acl\""));
        HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenReturn(headers);
        when(solidClient.getAclLink(headers)).thenReturn(aclUrl);
        when(solidClient.getResourceAclLink(testUrl.toString())).thenReturn(aclUrl);
        when(solidClient.createAcl(aclUrl, "acl")).thenReturn(true);

        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenReturn(headers);
        SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello", "text/plain");
        assertTrue(resource.setAcl("acl"));
    }

    @Test
    void getUrl() throws Exception {
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenReturn(null);
        SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello", "text/plain");
        assertEquals(TEST_URL, resource.getUrl());
        assertEquals("/resource", resource.getPath());
    }

    @Test
    void getUrlNull() throws Exception {
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenThrow(new Exception("Failed as expected"));
        SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello", "text/plain");
        assertFalse(resource.exists());
        assertNull(resource.getUrl());
        assertNull(resource.getPath());
    }

    @Test
    void getAclUrlMissing() throws Exception {
        Map<String, List<String>> headerMap = Map.of("Link", List.of("<" + aclUrl.toString() + ">; rel=\"acl\""));
        HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenReturn(headers);
        when(solidClient.getAclLink(headers)).thenReturn(null);
        when(solidClient.getResourceAclLink(testUrl.toString())).thenReturn(null);

        SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello", "text/plain");
        assertNull(resource.getAclUrl());
        // second attempt short cuts
        assertNull(resource.getAclUrl());
    }

    @Test
    void delete() throws Exception {
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenReturn(null);
        SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello", "text/plain");
        assertDoesNotThrow(() -> resource.delete());
    }

    @Test
    void deleteException() throws Exception {
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenReturn(null);
        SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello", "text/plain");
        doThrow(new Exception("Failed as expected")).when(solidClient).deleteResourceRecursively(testUrl);
        assertThrows(Exception.class, () -> resource.delete());
    }

    @Test
    void resourceToString() throws Exception {
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenReturn(null);
        SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello", "text/plain");
        assertEquals("SolidResource: " + TEST_URL, resource.toString());
    }

    @Test
    void emptyResourceToString() throws Exception {
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenThrow(new Exception("Failed as expected"));
        SolidResource resource = new SolidResource(solidClient, TEST_URL, "hello", "text/plain");
        assertEquals("SolidResource: null", resource.toString());
    }

    @Test
    void containerToString() throws Exception {
        when(solidClient.createResource(new URI("http://localhost/container/"), null, null)).thenReturn(null);
        SolidResource resource = new SolidResource(solidClient, "http://localhost/container/", null, null);
        assertEquals("SolidContainer: http://localhost/container/", resource.toString());
    }
}
