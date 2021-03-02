package org.solid.testharness.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.http.SolidClient;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SolidResourceTest {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.utils.SolidResourceTest");

    // Style question: extract variables or re-use literals in tests: KISS > DRY?
    URI testUrl = URI.create("http://localhost/resource");
    URI aclUrl = URI.create(testUrl + ".acl");

    @BeforeEach
    void setUp() {
    }

    @Test
    void testConstructorExceptions() {
        SolidClient solidClient = mock(SolidClient.class);
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
                new SolidResource(solidClient, "http://localhost/resource", "", null)
        );
        assertEquals("Parameter type is required", exception.getMessage());
    }

    @Test
    void testConstructorNoBody() {
        SolidClient solidClient = mock(SolidClient.class);
    }


    @Test
    void resourceExistsWithAcl() throws Exception {
        SolidClient solidClient = mock(SolidClient.class);
        Map<String,List<String>> headerMap = Map.of("Link", List.of("<"+aclUrl.toString()+">; rel=\"acl\""));
        HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenReturn(headers);
        when(solidClient.getAclLink(headers)).thenReturn(aclUrl);
        when(solidClient.getResourceAclLink(testUrl.toString())).thenReturn(aclUrl);

        SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello", "text/plain");

        // TODO: Consider AssertJ assertThat instead of assertTrue as it gives better error messages
        assertTrue(resource.exists());
        assertEquals(aclUrl.toString(), resource.getAclUrl());
    }

    @Test
    void resourceExistsWithoutAcl() throws Exception {
        SolidClient solidClient = mock(SolidClient.class);
        Map<String,List<String>> headerMap = Map.of("Link", List.of("<"+aclUrl+">; rel=\"noacl\""));
        HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenReturn(headers);
        when(solidClient.getResourceAclLink(testUrl.toString())).thenReturn(null);

        SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello", "text/plain");

        assertTrue(resource.exists());
        assertNull(resource.getAclUrl());
    }

    @Test
    void resourceExistsWithAclAfterLookup() throws Exception {
        SolidClient solidClient = mock(SolidClient.class);
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenReturn(null);
        URI lookedUpAclUrl = URI.create("http://localhost/lookupedUpAcl");
        when(solidClient.getResourceAclLink(testUrl.toString())).thenReturn(lookedUpAclUrl);

        SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello", "text/plain");

        assertTrue(resource.exists());
        assertEquals(lookedUpAclUrl.toString(), resource.getAclUrl());
    }

    @Test
    void resourceNotExists() throws Exception {
        SolidClient solidClient = mock(SolidClient.class);
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenThrow(new Exception());

        SolidResource resource = new SolidResource(solidClient, testUrl.toString(), "hello", "text/plain");

        assertFalse(resource.exists());
    }

    @Test
    void getContainer() throws Exception {
        SolidClient solidClient = mock(SolidClient.class);
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenReturn(null);

        SolidResource resource = new SolidResource(solidClient, "http://localhost/resource", "hello", "text/plain");
        SolidContainer container = resource.getContainer();

        assertTrue(resource.exists());
        assertTrue(container.exists());
        assertEquals("http://localhost/", container.getUrl());
        assertEquals("/", container.getPath());
    }

    @Test
    void getParent() throws Exception {
        SolidClient solidClient = mock(SolidClient.class);
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
        SolidClient solidClient = mock(SolidClient.class);
        when(solidClient.createResource(testUrl, "hello", "text/plain")).thenReturn(null);

        SolidResource resource = new SolidResource(solidClient, "http://localhost/resource", "hello", "text/plain");
        SolidContainer container = resource.getContainer();
        SolidContainer nextContainer = container.getContainer();

        assertTrue(resource.exists());
        assertTrue(container.exists());
        assertEquals("http://localhost/", container.getUrl());
        assertEquals("/", container.getPath());
        assertNull(nextContainer);
    }

    @Test
    void setAcl() {
    }

    @Test
    void getUrl() {
    }

    @Test
    void getAclUrl() {
    }
}
