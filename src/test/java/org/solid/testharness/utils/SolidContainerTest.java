package org.solid.testharness.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.testharness.http.SolidClient;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SolidContainerTest {
    private static final String TEST_URL = "http://localhost/container/";
    URI testUrl = URI.create(TEST_URL);
    SolidClient solidClient;

    @BeforeEach
    void setUp() {
        solidClient = mock(SolidClient.class);
    }

    @Test
    void testConstructorExceptions() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                new SolidContainer(solidClient, null)
        );
        assertEquals("A container url must end with /", exception.getMessage());
        exception = assertThrows(IllegalArgumentException.class, () ->
                new SolidContainer(solidClient, "not a container")
        );
        assertEquals("A container url must end with /", exception.getMessage());
        exception = assertThrows(IllegalArgumentException.class, () ->
                new SolidContainer(null, TEST_URL)
        );
        assertEquals("Parameter solidClient is required", exception.getMessage());
    }

    @Test
    void testCreate() {
        final SolidContainer container = SolidContainer.create(solidClient, testUrl.toString());
        assertTrue(container.isContainer());
        assertTrue(container.exists());
    }

    @Test
    void listMembers() throws Exception {
        when(solidClient.parseMembers("containmentData", testUrl)).thenReturn(List.of("member"));
        when(solidClient.getContainmentData(testUrl)).thenReturn("containmentData");
        final SolidContainer container = SolidContainer.create(solidClient, testUrl.toString());
        assertEquals("member", container.listMembers().get(0));
    }

    @Test
    void listMembersException() throws Exception {
        when(solidClient.getContainmentData(testUrl)).thenThrow(new Exception("Error"));
        final SolidContainer container = SolidContainer.create(solidClient, testUrl.toString());
        assertThrows(Exception.class, () -> container.listMembers());
    }

    @Test
    void parseMembers() throws Exception {
        when(solidClient.parseMembers("containmentData", testUrl)).thenReturn(List.of("member"));
        final SolidContainer container = SolidContainer.create(solidClient, testUrl.toString());
        assertEquals("member", container.parseMembers("containmentData").get(0));
    }

    @Test
    void parseMembersException() throws Exception {
        when(solidClient.parseMembers("containmentData", testUrl)).thenThrow(new Exception("Error"));
        final SolidContainer container = SolidContainer.create(solidClient, testUrl.toString());
        assertThrows(Exception.class, () -> container.parseMembers("containmentData"));
    }

    @Test
    void generateChildContainer() throws Exception {
        final SolidContainer container = SolidContainer.create(solidClient, testUrl.toString());
        final SolidContainer childContainer = container.generateChildContainer();
        assertTrue(childContainer.isContainer());
        assertTrue(childContainer.getUrl().startsWith(TEST_URL) &&
                childContainer.getUrl().length() > TEST_URL.length());
    }

    @Test
    void generateChildResource() throws Exception {
        final SolidContainer container = SolidContainer.create(solidClient, testUrl.toString());
        final SolidResource childResource = container.generateChildResource(".suffix");
        assertFalse(childResource.isContainer());
        assertTrue(childResource.getUrl().startsWith(TEST_URL) && childResource.getUrl().endsWith(".suffix"));
    }

    @Test
    void createChildResource() throws Exception {
        final SolidContainer container = SolidContainer.create(solidClient, testUrl.toString());
        when(solidClient.createResource(any(), any(), any())).thenReturn(null);
        final SolidResource childResource = container.createChildResource(
                ".suffix", "hello", "text/plain"
        );
        assertFalse(childResource.isContainer());
        assertTrue(childResource.getUrl().startsWith(TEST_URL) && childResource.getUrl().endsWith(".suffix"));
    }

    @Test
    void createChildResourcException() throws Exception {
        final SolidContainer container = SolidContainer.create(solidClient, testUrl.toString());
        assertNull(container.createChildResource(".suffix", "hello", null));
    }
}
