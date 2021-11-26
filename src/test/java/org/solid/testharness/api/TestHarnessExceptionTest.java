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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestHarnessExceptionTest {
    @Test
    void simpleMessage() {
        final TestHarnessException exception = new TestHarnessException("message", new Exception("FAIL"));
        assertEquals("org.solid.testharness.api.TestHarnessException: message\n" +
                "Caused by: java.lang.Exception: FAIL", exception.getMessage());
    }

    @Test
    void noMessage() {
        final TestHarnessException exception = new TestHarnessException(null, new Exception("FAIL"));
        assertEquals("org.solid.testharness.api.TestHarnessException: \n" +
                "Caused by: java.lang.Exception: FAIL", exception.getMessage());
    }

    @Test
    void noCause() {
        final TestHarnessException exception = new TestHarnessException("message", null);
        assertEquals("org.solid.testharness.api.TestHarnessException: message", exception.getMessage());
    }
}
