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
package org.solid.testharness.reporting;

import com.intuit.karate.Results;
import com.intuit.karate.Suite;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class TestSuiteResultsTest {
    @Test
    void emptyResults() {
        final TestSuiteResults testSuiteResults = TestSuiteResults.emptyResults();
        assertEquals("", testSuiteResults.getErrorMessages());
        assertEquals(0, testSuiteResults.getFailCount());
        assertEquals(0, testSuiteResults.getFeatureFailCount());
        assertEquals(0, testSuiteResults.getFeaturePassCount());
        assertEquals(0, testSuiteResults.getFeatureSkipCount());
        assertEquals(0, testSuiteResults.getFeatureTotal());
        assertEquals(0, testSuiteResults.getScenarioFailCount());
        assertEquals(0, testSuiteResults.getScenarioPassCount());
        assertEquals(0, testSuiteResults.getScenarioTotal());
        assertEquals(0, testSuiteResults.getTimeTakenMillis());
        assertEquals(0, testSuiteResults.getFailCount());
        assertNotNull(testSuiteResults.getResultDate());
    }

    @Test
    void getFeatures() {
        final Results results = mock(Results.class);
        when(results.getSuite()).thenReturn(Suite.forTempUse());
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        assertNull(testSuiteResults.getFeatures());
    }

    @Test
    void getErrorMessages() {
        final Results results = mock(Results.class);
        when(results.getErrorMessages()).thenReturn("ERRORS");
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        assertEquals("ERRORS", testSuiteResults.getErrorMessages());
    }

    @Test
    void getFailCount() {
        final Results results = mock(Results.class);
        when(results.getFailCount()).thenReturn(10);
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        assertEquals(10, testSuiteResults.getFailCount());
    }

    @Test
    void getFeatureFailCount() {
        final Results results = mock(Results.class);
        when(results.getFeaturesFailed()).thenReturn(1);
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        assertEquals(1, testSuiteResults.getFeatureFailCount());
    }

    @Test
    void getFeaturePassCount() {
        final Results results = mock(Results.class);
        when(results.getFeaturesPassed()).thenReturn(2);
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        assertEquals(2, testSuiteResults.getFeaturePassCount());
    }

    @Test
    void getFeatureTotal() {
        final Results results = mock(Results.class);
        when(results.getFeaturesTotal()).thenReturn(3);
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        assertEquals(3, testSuiteResults.getFeatureTotal());
    }

    @Test
    void getScenarioFailCount() {
        final Results results = mock(Results.class);
        when(results.getScenariosFailed()).thenReturn(4);
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        assertEquals(4, testSuiteResults.getScenarioFailCount());
    }

    @Test
    void getScenarioPassCount() {
        final Results results = mock(Results.class);
        when(results.getScenariosPassed()).thenReturn(5);
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        assertEquals(5, testSuiteResults.getScenarioPassCount());
    }

    @Test
    void getScenarioTotal() {
        final Results results = mock(Results.class);
        when(results.getScenariosTotal()).thenReturn(6);
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        assertEquals(6, testSuiteResults.getScenarioTotal());
    }

    @Test
    void getElapsedTime() {
        final TestSuiteResults testSuiteResults = new TestSuiteResults(null);
        final long startTime = System.currentTimeMillis();
        testSuiteResults.setStartTime(startTime);
        assertTrue(testSuiteResults.getElapsedTime() >= 0);
    }

    @Test
    void getResultDate() {
        final Results results = mock(Results.class);
        final Date now = new Date();
        when(results.getEndTime()).thenReturn(now.getTime());
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        assertEquals(testSuiteResults.getResultDate(), now);
    }

    @Test
    void toJson() {
        final Results results = mock(Results.class);
        when(results.getFeaturesPassed()).thenReturn(10);
        when(results.getFeaturesFailed()).thenReturn(0);
        when(results.toKarateJson()).thenReturn(Map.of("featuresSkipped", 0));
        when(results.getScenariosPassed()).thenReturn(20);
        when(results.getScenariosFailed()).thenReturn(0);
        when(results.getElapsedTime()).thenReturn(1000d);
        when(results.getTimeTakenMillis()).thenReturn(1000d);
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        assertTrue(testSuiteResults.toJson().contains("\"featuresPassed\":10"));
    }

    @Test
    void toJsonEmpty() {
        final TestSuiteResults testSuiteResults = TestSuiteResults.emptyResults();
        assertTrue(testSuiteResults.toJson().contains("\"featuresPassed\":0"));
        assertTrue(testSuiteResults.toJson().contains("\"featuresFailed\":0"));
    }

    @Test
    void toJsonFails() {
        final Results results = mock(Results.class);
        when(results.getFeaturesPassed()).thenThrow(new RuntimeException("FAIL"));
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        assertEquals("{}", testSuiteResults.toJson());
    }

    @Test
    void testToString() {
        final Results results = mock(Results.class);
        when(results.getFeaturesPassed()).thenReturn(1);
        when(results.getFeaturesFailed()).thenReturn(2);
        when(results.getFeaturesTotal()).thenReturn(3);
        when(results.getScenariosPassed()).thenReturn(10);
        when(results.getScenariosFailed()).thenReturn(20);
        when(results.getScenariosTotal()).thenReturn(30);
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        assertEquals("Results:\n  Features  passed: 1, failed: 2, total: 3\n" +
                "  Scenarios passed: 10, failed: 20, total: 30", testSuiteResults.toString());
    }

    @Test
    void testToStringEmpty() {
        final TestSuiteResults testSuiteResults = TestSuiteResults.emptyResults();
        assertEquals("Results: No features were run", testSuiteResults.toString());
    }
}
