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
package org.solid.testharness.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.solid.testharness.accesscontrol.AccessDataset;
import org.solid.testharness.accesscontrol.AccessDatasetBuilder;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.http.SolidClientProvider;
import org.solid.testharness.utils.SolidContainerProvider;
import org.solid.testharness.utils.SolidResourceProvider;
import org.solid.testharness.utils.TestHarnessException;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class SolidResourceTest {
    private static final URI TEST_URL = URI.create("https://example.org/resource");

    @Test
    void testCreate() {
        final SolidClientProvider solidClientProvider = mock(SolidClientProvider.class);
        final SolidClient solidClient = new SolidClient(solidClientProvider);
        final SolidResource solidResource = SolidResource.create(solidClient, TEST_URL.toString(), "hello",
                HttpConstants.MEDIA_TYPE_TEXT_PLAIN);
        assertNotNull(solidResource.solidResourceProvider.getUrl());
    }

    @Test
    void testCreateException() {
        final SolidClientProvider solidClientProvider = mock(SolidClientProvider.class);
        final SolidClient solidClient = new SolidClient(solidClientProvider);
        assertThrows(TestHarnessApiException.class,
                () ->  SolidResource.create(solidClient, null, null, null));
    }

    @Test
    void resourceExists() {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        when(solidResourceProvider.getUrl()).thenReturn(TEST_URL);
        final SolidResource resource = new SolidResource(solidResourceProvider);
        assertNotNull(resource.solidResourceProvider.getUrl());
    }

    @Test
    void resourceExistsFalse() {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        when(solidResourceProvider.getUrl()).thenReturn(null);
        final SolidResource resource = new SolidResource(solidResourceProvider);
        assertNull(resource.solidResourceProvider.getUrl());
    }

    @Test
    void getUrl() {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        when(solidResourceProvider.getUrl()).thenReturn(TEST_URL);
        final SolidResource resource = new SolidResource(solidResourceProvider);
        assertEquals(TEST_URL.toString(), resource.getUrl());
    }

    @Test
    void getUrlNull() {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        when(solidResourceProvider.getUrl()).thenReturn(null);
        final SolidResource resource = new SolidResource(solidResourceProvider);
        assertNull(resource.getUrl());
    }

    @Test
    void getContainer() throws Exception {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        final SolidContainerProvider solidContainerProvider = mock(SolidContainerProvider.class);
        when(solidResourceProvider.getContainer()).thenReturn(solidContainerProvider);
        when(solidContainerProvider.getUrl()).thenReturn(TEST_URL);
        final SolidResource solidResource = new SolidResource(solidResourceProvider);
        assertEquals(TEST_URL.toString(), solidResource.getContainer().getUrl());
    }

    @Test
    void getContainerNull() throws Exception {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        when(solidResourceProvider.getContainer()).thenReturn(null);
        final SolidResource solidResource = new SolidResource(solidResourceProvider);
        assertNull(solidResource.getContainer());
    }

    @Test
    void getContainerException() throws Exception {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        when(solidResourceProvider.getContainer()).thenThrow(new TestHarnessException("FAIL"));
        final SolidResource solidResource = new SolidResource(solidResourceProvider);
        assertThrows(TestHarnessApiException.class, solidResource::getContainer);
    }

    @Test
    void getAclUrl() {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        when(solidResourceProvider.getAclUrl()).thenReturn(TEST_URL);
        final SolidResource solidResource = new SolidResource(solidResourceProvider);
        assertEquals(TEST_URL.toString(), solidResource.getAclUrl());
    }

    @Test
    void getAclUrlNull() {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        when(solidResourceProvider.getAclUrl()).thenReturn(null);
        final SolidResource solidResource = new SolidResource(solidResourceProvider);
        assertNull(solidResource.getAclUrl());
    }

    @Test
    void getAclUrlException() {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        when(solidResourceProvider.getAclUrl()).thenThrow(new TestHarnessApiException("FAIL"));
        final SolidResource solidResource = new SolidResource(solidResourceProvider);
        assertThrows(TestHarnessApiException.class, solidResource::getAclUrl);
    }

    @Test
    void findStorage() throws Exception {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        final SolidContainerProvider solidContainerProvider = mock(SolidContainerProvider.class);
        when(solidResourceProvider.findStorage()).thenReturn(solidContainerProvider);
        when(solidContainerProvider.getUrl()).thenReturn(TEST_URL);
        final SolidResource solidResource = new SolidResource(solidResourceProvider);
        assertEquals(TEST_URL.toString(), solidResource.findStorage().getUrl());
    }

    @Test
    void findStorageNull() throws Exception {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        when(solidResourceProvider.findStorage()).thenReturn(null);
        final SolidResource solidResource = new SolidResource(solidResourceProvider);
        assertNull(solidResource.findStorage());
    }

    @Test
    void findStorageException() throws Exception {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        when(solidResourceProvider.findStorage()).thenThrow(new TestHarnessApiException("FAIL"));
        final SolidResource solidResource = new SolidResource(solidResourceProvider);
        assertThrows(TestHarnessApiException.class, solidResource::findStorage);
    }

    @Test
    void isStorageType() {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        when(solidResourceProvider.isStorageType()).thenReturn(true);
        final SolidResource resource = new SolidResource(solidResourceProvider);
        assertTrue(resource.isStorageType());
    }

    @Test
    void isStorageTypeException() {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        when(solidResourceProvider.isStorageType()).thenThrow(new TestHarnessApiException("FAIL"));
        final SolidResource resource = new SolidResource(solidResourceProvider);
        assertThrows(TestHarnessApiException.class, resource::isStorageType);
    }

    @Test
    void getAccessDatasetBuilder() {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        final AccessDatasetBuilder accessDatasetBuilder = mock(AccessDatasetBuilder.class);
        when(solidResourceProvider.getAccessDatasetBuilder()).thenReturn(accessDatasetBuilder);
        final SolidResource resource = new SolidResource(solidResourceProvider);
        assertEquals(accessDatasetBuilder, resource.getAccessDatasetBuilder());
    }

    @Test
    void getAccessDatasetBuilderException() {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        when(solidResourceProvider.getAccessDatasetBuilder()).thenThrow(new TestHarnessApiException("FAIL"));
        final SolidResource resource = new SolidResource(solidResourceProvider);
        assertThrows(TestHarnessApiException.class, resource::getAccessDatasetBuilder);
    }

    @Test
    void getAccessDataset() {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        final AccessDataset accessDataset = mock(AccessDataset.class);
        when(solidResourceProvider.getAccessDataset()).thenReturn(accessDataset);
        final SolidResource resource = new SolidResource(solidResourceProvider);
        assertEquals(accessDataset, resource.getAccessDataset());
    }

    @Test
    void getAccessDatasetException() {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        when(solidResourceProvider.getAccessDataset()).thenThrow(new RuntimeException("FAIL"));
        final SolidResource resource = new SolidResource(solidResourceProvider);
        assertThrows(TestHarnessApiException.class, resource::getAccessDataset);
    }

    @Test
    void setAccessDataset() throws Exception {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        final SolidResource resource = new SolidResource(solidResourceProvider);
        assertDoesNotThrow(() -> resource.setAccessDataset(null));
        verify(solidResourceProvider).setAccessDataset(null);
    }

    @Test
    void setAccessDatasetException() throws Exception {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        doThrow(new TestHarnessApiException("FAIL")).when(solidResourceProvider).setAccessDataset(any());
        final SolidResource resource = new SolidResource(solidResourceProvider);
        assertThrows(TestHarnessApiException.class, () -> resource.setAccessDataset(null));
    }

    @Test
    void delete() {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        final SolidResource resource = new SolidResource(solidResourceProvider);
        assertDoesNotThrow(resource::delete);
        verify(solidResourceProvider).delete();
    }

    @Test
    void deleteException() {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        doThrow(new TestHarnessApiException("FAIL")).when(solidResourceProvider).delete();
        final SolidResource resource = new SolidResource(solidResourceProvider);
        assertThrows(TestHarnessApiException.class, resource::delete);
    }

    @Test
    void resourceToString() {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        when(solidResourceProvider.isContainer()).thenReturn(false);
        when(solidResourceProvider.getUrl()).thenReturn(TEST_URL);
        final SolidResource resource = new SolidResource(solidResourceProvider);
        assertEquals("SolidResource: " + TEST_URL, resource.toString());
    }

    @Test
    void containerToString() {
        final SolidResourceProvider solidResourceProvider = mock(SolidResourceProvider.class);
        when(solidResourceProvider.isContainer()).thenReturn(true);
        when(solidResourceProvider.getUrl()).thenReturn(TEST_URL);
        final SolidResource resource = new SolidResource(solidResourceProvider);
        assertEquals("SolidContainer: " + TEST_URL, resource.toString());
    }
}
