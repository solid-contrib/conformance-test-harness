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
package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class BootstrapQuarkusTest {

    @Test
    void getInstance() {
        final Bootstrap bootstrap = Bootstrap.getInstance();
        assertNotNull(bootstrap);
        final Bootstrap bootstrap2 = Bootstrap.getInstance();
        assertNotNull(bootstrap2);
        assertEquals(bootstrap, bootstrap2);
    }

    @Test
    void getTestSubject() {
        final Bootstrap bootstrap = Bootstrap.getInstance();
        assertNotNull(bootstrap.getTestSubject());
    }

    @Test
    void getTestHarness() {
        final Bootstrap bootstrap = Bootstrap.getInstance();
        assertNotNull(bootstrap.getTestHarness());
    }

    @Test
    void getConfig() {
        final Bootstrap bootstrap = Bootstrap.getInstance();
        assertNotNull(bootstrap.getConfig());
    }
}
