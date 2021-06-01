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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestHarnessInitializationExceptionTest {
    @Test
    void simpleMessage() {
        final TestHarnessInitializationException exception = new TestHarnessInitializationException("message");
        assertEquals("message", exception.getMessage());
    }

    @Test
    void simpleMessageNullArgs() {
        final TestHarnessInitializationException exception = new TestHarnessInitializationException("message", null);
        assertEquals("message", exception.getMessage());
    }

    @Test
    void simpleMessageArgs1() {
        final TestHarnessInitializationException exception = new TestHarnessInitializationException("message %s",
                "arg1");
        assertEquals("message arg1", exception.getMessage());
    }

    @Test
    void simpleMessageArgs2() {
        final TestHarnessInitializationException exception = new TestHarnessInitializationException("message %s %s",
                "arg1", "arg2");
        assertEquals("message arg1 arg2", exception.getMessage());
    }
}
