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
package org.solid.testharness.utils;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.http.SolidClientProvider;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class SolidContainerProviderTest {
    private static final URI ROOT_URL = URI.create("https://example.org/");
    private static final URI TEST_URL = ROOT_URL.resolve("/container/");
    SolidClientProvider solidClientProvider;

    @BeforeEach
    void setUp() {
        solidClientProvider = mock(SolidClientProvider.class);
    }

    @Test
    void testConstructorExceptions() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                new SolidContainerProvider(solidClientProvider, null)
        );
        assertEquals("A container url must end with /", exception.getMessage());
        exception = assertThrows(IllegalArgumentException.class, () ->
                new SolidContainerProvider(solidClientProvider, ROOT_URL.resolve("/not-container"))
        );
        assertEquals("A container url must end with /", exception.getMessage());
        exception = assertThrows(IllegalArgumentException.class, () ->
                new SolidContainerProvider(null, TEST_URL)
        );
        assertEquals("Parameter solidClientProvider is required", exception.getMessage());
    }

    @Test
    void testCreate() throws Exception {
        final SolidContainerProvider container = new SolidContainerProvider(solidClientProvider, TEST_URL);
        assertTrue(container.isContainer());
        assertEquals(TEST_URL, container.getUrl());
    }

    @Test
    void instantiate() throws Exception {
        final SolidContainerProvider container = new SolidContainerProvider(solidClientProvider, TEST_URL);
        assertDoesNotThrow(container::instantiate);
    }

    @Test
    void instantiateBadResponse() throws Exception {
        when(solidClientProvider.createContainer(any())).thenThrow(new TestHarnessException("FAIL"));
        final SolidContainerProvider container = new SolidContainerProvider(solidClientProvider, TEST_URL);
        final Exception exception = assertThrows(Exception.class, container::instantiate);
        assertEquals("FAIL", exception.getMessage());
    }

    @Test
    void instantiateException() throws Exception {
        when(solidClientProvider.createContainer(any())).thenThrow(new TestHarnessException("FAIL"));
        final SolidContainerProvider container = new SolidContainerProvider(solidClientProvider, TEST_URL);
        final Exception exception = assertThrows(Exception.class, container::instantiate);
        assertEquals("FAIL", exception.getMessage());
    }

    @Test
    void reserveContainer() throws Exception {
        final SolidContainerProvider container = new SolidContainerProvider(solidClientProvider, TEST_URL);
        when(solidClientProvider.createResource(any(), any(), any())).thenReturn(null);
        final SolidResourceProvider childResource = container.reserveContainer("container");
        assertTrue(childResource.isContainer());
        assertEquals(TEST_URL.resolve("container/"), childResource.getUrl());
    }

    @Test
    void reserveResource() throws Exception {
        final SolidContainerProvider container = new SolidContainerProvider(solidClientProvider, TEST_URL);
        when(solidClientProvider.createResource(any(), any(), any())).thenReturn(null);
        final SolidResourceProvider childResource = container.reserveResource("resource.suffix");
        assertFalse(childResource.isContainer());
        assertEquals(TEST_URL.resolve("resource.suffix"), childResource.getUrl());
    }

    @Test
    void createResource() throws Exception {
        final SolidContainerProvider container = new SolidContainerProvider(solidClientProvider, TEST_URL);
        when(solidClientProvider.createResource(any(), any(), any())).thenReturn(null);
        final SolidResourceProvider childResource = container.createResource(
                "filename.suffix", "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN
        );
        assertFalse(childResource.isContainer());
        assertTrue(childResource.getUrl().toString().startsWith(TEST_URL.toString()) &&
                childResource.getUrl().toString().endsWith(".suffix"));
    }

    @Test
    void createResourceException() throws Exception {
        final SolidContainerProvider container = new SolidContainerProvider(solidClientProvider, TEST_URL);
        assertThrows(Exception.class, () -> container.createResource("filename.suffix", "hello", null));
    }

    @Test
    void deleteContents() throws Exception {
        final SolidContainerProvider container = new SolidContainerProvider(solidClientProvider, TEST_URL);
        assertDoesNotThrow(container::deleteContents);
        verify(solidClientProvider).deleteContentsRecursively(TEST_URL);
    }
}
