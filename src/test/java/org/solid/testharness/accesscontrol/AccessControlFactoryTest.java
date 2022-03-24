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
package org.solid.testharness.accesscontrol;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.solid.testharness.config.TestSubject;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.solid.testharness.config.TestSubject.AccessControlMode.*;
import static org.solid.testharness.utils.TestUtils.SAMPLE_BASE;

@QuarkusTest
class AccessControlFactoryTest {
    private static final URI BASE_URI = URI.create(SAMPLE_BASE);

    @InjectMock
    TestSubject testSubject;

    @Inject
    AccessControlFactory accessControlFactory;

    @Test
    void getAccessDatasetBuilderWac() {
        when(testSubject.getAccessControlMode()).thenReturn(WAC);
        final AccessDatasetBuilder accessDatasetBuilder = accessControlFactory.getAccessDatasetBuilder(SAMPLE_BASE);
        assertEquals(WAC, accessDatasetBuilder.build().getMode());
    }

    @Test
    void getAccessDatasetBuilderAcp() {
        when(testSubject.getAccessControlMode()).thenReturn(ACP);
        final AccessDatasetBuilder accessDatasetBuilder = accessControlFactory.getAccessDatasetBuilder(SAMPLE_BASE);
        assertEquals(ACP, accessDatasetBuilder.build().getMode());
    }

    @Test
    void getAccessDatasetBuilderAcpLegacy() {
        when(testSubject.getAccessControlMode()).thenReturn(ACP_LEGACY);
        final AccessDatasetBuilder accessDatasetBuilder = accessControlFactory.getAccessDatasetBuilder(SAMPLE_BASE);
        assertEquals(ACP_LEGACY, accessDatasetBuilder.build().getMode());
    }

    @Test
    void getAccessDatasetBuilderNull() {
        when(testSubject.getAccessControlMode()).thenReturn(null);
        assertNull(accessControlFactory.getAccessDatasetBuilder(SAMPLE_BASE));
    }

    @Test
    void createAccessDatasetWac() throws IOException {
        when(testSubject.getAccessControlMode()).thenReturn(WAC);
        final AccessDataset accessDataset = accessControlFactory.createAccessDataset("", BASE_URI);
        assertEquals(WAC, accessDataset.getMode());
    }

    @Test
    void createAccessDatasetAcp() throws IOException {
        when(testSubject.getAccessControlMode()).thenReturn(ACP);
        final AccessDataset accessDataset = accessControlFactory.createAccessDataset("", BASE_URI);
        assertEquals(ACP, accessDataset.getMode());
    }

    @Test
    void createAccessDatasetAcpLegacy() throws IOException {
        when(testSubject.getAccessControlMode()).thenReturn(ACP_LEGACY);
        final AccessDataset accessDataset = accessControlFactory.createAccessDataset("", BASE_URI);
        assertEquals(ACP_LEGACY, accessDataset.getMode());
    }

    @Test
    void createAccessDatasetNull() throws IOException {
        when(testSubject.getAccessControlMode()).thenReturn(null);
        assertNull(accessControlFactory.createAccessDataset("", BASE_URI));
    }
}