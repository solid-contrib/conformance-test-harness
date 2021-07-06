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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.http.SolidClient;

import java.net.URI;
import java.net.http.HttpResponse;
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
        when(solidClient.getContentAsTurtle(testUrl)).thenReturn("containmentData");
        final SolidContainer container = SolidContainer.create(solidClient, testUrl.toString());
        assertEquals("member", container.listMembers().get(0));
    }

    @Test
    void listMembersException() throws Exception {
        when(solidClient.getContentAsTurtle(testUrl)).thenThrow(new Exception("Error"));
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
    void instantiate() throws Exception {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(200, "");
        when(solidClient.createContainer(any())).thenReturn(mockResponse);
        final SolidContainer container = SolidContainer.create(solidClient, testUrl.toString());
        assertNotNull(container.instantiate());
    }

    @Test
    void instantiateBadResponse() throws Exception {
        final HttpResponse<String> mockResponse = TestUtils.mockStringResponse(400, "BAD RESPONSE");
        when(solidClient.createContainer(any())).thenReturn(mockResponse);
        final SolidContainer container = SolidContainer.create(solidClient, testUrl.toString());
        assertNull(container.instantiate());
    }

    @Test
    void instantiateException() throws Exception {
        when(solidClient.createContainer(any())).thenThrow(new Exception("FAIL"));
        final SolidContainer container = SolidContainer.create(solidClient, testUrl.toString());
        assertNull(container.instantiate());
    }

    @Test
    void generateChildContainer() {
        final SolidContainer container = SolidContainer.create(solidClient, testUrl.toString());
        final SolidContainer childContainer = container.generateChildContainer();
        assertTrue(childContainer.isContainer());
        assertTrue(childContainer.getUrl().startsWith(TEST_URL) &&
                childContainer.getUrl().length() > TEST_URL.length());
    }

    @Test
    void generateChildResource() {
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
                ".suffix", "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN
        );
        assertFalse(childResource.isContainer());
        assertTrue(childResource.getUrl().startsWith(TEST_URL) && childResource.getUrl().endsWith(".suffix"));
    }

    @Test
    void createChildResourcException() {
        final SolidContainer container = SolidContainer.create(solidClient, testUrl.toString());
        assertNull(container.createChildResource(".suffix", "hello", null));
    }

    @Test
    void deleteContents() throws Exception {
        final SolidContainer container = SolidContainer.create(solidClient, testUrl.toString());
        assertDoesNotThrow(() -> container.deleteContents());
        verify(solidClient).deleteContentsRecursively(testUrl);
    }
}
