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
import org.junit.jupiter.api.Test;
import org.solid.testharness.config.Config;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.solid.testharness.config.Config.AccessControlMode.*;

@QuarkusTest
class AccessControlFactoryTest {
    private static final String BASE = "https://example.org";

    @Inject
    Config config;

    @Inject
    AccessControlFactory accessControlFactory;

    @Test
    void getAccessDatasetBuilderWac() {
        config.setAccessControlMode(WAC);
        final AccessDatasetBuilder accessDatasetBuilder = accessControlFactory.getAccessDatasetBuilder(BASE);
        assertEquals(WAC, accessDatasetBuilder.build().getMode());
    }

    @Test
    void getAccessDatasetBuilderAcp() {
        config.setAccessControlMode(ACP);
        final AccessDatasetBuilder accessDatasetBuilder = accessControlFactory.getAccessDatasetBuilder(BASE);
        assertEquals(ACP, accessDatasetBuilder.build().getMode());
    }

    @Test
    void getAccessDatasetBuilderAcpLegacy() {
        config.setAccessControlMode(ACP_LEGACY);
        final AccessDatasetBuilder accessDatasetBuilder = accessControlFactory.getAccessDatasetBuilder(BASE);
        assertEquals(ACP_LEGACY, accessDatasetBuilder.build().getMode());
    }

    @Test
    void getAccessDatasetBuilderNull() {
        config.setAccessControlMode(null);
        assertNull(accessControlFactory.getAccessDatasetBuilder(BASE));
    }
}