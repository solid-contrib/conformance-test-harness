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
package org.solid.testharness.api;

import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.junit.jupiter.api.Test;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.http.SolidClientProvider;
import org.solid.testharness.utils.SolidContainerProvider;
import org.solid.testharness.utils.SolidResourceProvider;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SolidContainerTest {
    private static final URI TEST_URL = URI.create("https://example.org/");
    private static final String CHLID = "https://example.org/test/";
    private static final String MEMBERS = String.format("<%s> <%s> <%s>.", TEST_URL, LDP.CONTAINS, CHLID);

    @Test
    void testCreate() {
        final SolidClientProvider solidClientProvider = mock(SolidClientProvider.class);
        final SolidClient solidClient = new SolidClient(solidClientProvider);
        final SolidContainer solidContainer = SolidContainer.create(solidClient, TEST_URL.toString());
        assertTrue(solidContainer.exists());
    }

    @Test
    void instantiate() {
        final SolidContainerProvider solidContainerProvider = mock(SolidContainerProvider.class);
        final SolidContainer solidContainer = new SolidContainer(solidContainerProvider);
        assertEquals(solidContainer, solidContainer.instantiate());
        verify(solidContainerProvider).instantiate();
    }

    @Test
    void reserveContainer() {
        final SolidContainerProvider solidContainerProvider = mock(SolidContainerProvider.class);
        final SolidContainerProvider newContainer = mock(SolidContainerProvider.class);
        when(solidContainerProvider.reserveContainer(any())).thenReturn(newContainer);
        final SolidContainer solidContainer = new SolidContainer(solidContainerProvider);
        assertNotNull(solidContainer.reserveContainer());
        verify(solidContainerProvider).reserveContainer(any());
        verify(newContainer, never()).instantiate();
    }

    @Test
    void createContainer() {
        final SolidContainerProvider solidContainerProvider = mock(SolidContainerProvider.class);
        final SolidContainerProvider newContainer = mock(SolidContainerProvider.class);
        when(solidContainerProvider.reserveContainer(any())).thenReturn(newContainer);
        final SolidContainer solidContainer = new SolidContainer(solidContainerProvider);
        assertNotNull(solidContainer.createContainer());
        verify(solidContainerProvider).reserveContainer(any());
        verify(newContainer).instantiate();
    }

    @Test
    void reserveResource() {
        final SolidContainerProvider solidContainerProvider = mock(SolidContainerProvider.class);
        final SolidResourceProvider newResource = mock(SolidResourceProvider.class);
        when(solidContainerProvider.reserveResource(any())).thenReturn(newResource);
        final SolidContainer solidContainer = new SolidContainer(solidContainerProvider);
        assertNotNull(solidContainer.reserveResource(".txt"));
        verify(solidContainerProvider).reserveResource(any());
    }

    @Test
    void createResource() {
        final SolidContainerProvider solidContainerProvider = mock(SolidContainerProvider.class);
        final SolidResourceProvider newResource = mock(SolidResourceProvider.class);
        when(solidContainerProvider.createResource(any(), any(), any())).thenReturn(newResource);
        final SolidContainer solidContainer = new SolidContainer(solidContainerProvider);
        assertNotNull(solidContainer.createResource(".txt", "hello", HttpConstants.MEDIA_TYPE_TEXT_PLAIN));
        verify(solidContainerProvider).createResource(any(), any(), any());
    }

    @Test
    void listMembers() throws Exception {
        final SolidContainerProvider solidContainerProvider = mock(SolidContainerProvider.class);
        when(solidContainerProvider.getContentAsTurtle()).thenReturn(MEMBERS);
        when(solidContainerProvider.getUrl()).thenReturn(TEST_URL);
        final SolidContainer solidContainer = new SolidContainer(solidContainerProvider);
        final List<String> members = solidContainer.listMembers();
        assertEquals(1, members.size());
        assertEquals(CHLID, members.get(0));
    }

    @Test
    void parseMembers() throws Exception {
        final SolidContainerProvider solidContainerProvider = mock(SolidContainerProvider.class);
        when(solidContainerProvider.getUrl()).thenReturn(TEST_URL);
        final SolidContainer solidContainer = new SolidContainer(solidContainerProvider);
        final List<String> members = solidContainer.parseMembers(MEMBERS);
        assertEquals(1, members.size());
        assertEquals(CHLID, members.get(0));
    }

    @Test
    void deleteContents() throws Exception {
        final SolidContainerProvider solidContainerProvider = mock(SolidContainerProvider.class);
        final SolidContainer solidContainer = new SolidContainer(solidContainerProvider);
        assertDoesNotThrow(solidContainer::deleteContents);
        verify(solidContainerProvider).deleteContents();
    }
}
