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
package org.solid.testharness;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import org.junit.jupiter.api.Test;
import org.solid.testharness.reporting.TestSuiteResults;
import org.solid.testharness.utils.FeatureResultHandler;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@QuarkusTest
class TestRunnerTest {

    @Inject
    TestRunner testRunner;
    @InjectSpy
    FeatureResultHandler featureResultHandler;


    @Test
    void runTests() {
        final TestSuiteResults results = testRunner.runTests(List.of("src/test/resources/test.feature"), 1, true);
        assertNotNull(results);
        assertEquals(1, results.getFailCount());
    }

    @Test
    void runTestsEmpty() {
        final TestSuiteResults results = testRunner.runTests(Collections.emptyList(), 10, true);
        assertNotNull(results);
        assertEquals(0, results.getFailCount());
    }

    @Test
    void runTestsNoThreads() {
        final TestSuiteResults results = testRunner.runTests(Collections.emptyList(), 0, true);
        assertNotNull(results);
        assertEquals(0, results.getFailCount());
    }

    @Test
    void runTestsNoList() {
        final TestSuiteResults results = testRunner.runTests(null, 1, true);
        assertNotNull(results);
        assertEquals(0, results.getFailCount());
    }

    @Test
    void runTestsMissing() {
        assertThrows(RuntimeException.class, () -> testRunner.runTests(List.of("missing.feature"), 1, true));
    }

    @Test
    void runTestNoReporting() {
        final TestSuiteResults results = testRunner.runTests(List.of("src/test/resources/test.feature"), 1, false);
        assertNotNull(results);
        assertEquals(1, results.getFailCount());
        verify(featureResultHandler, never()).featureReport(any(), any());
    }
}
