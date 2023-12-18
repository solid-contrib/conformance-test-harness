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
package org.solid.testharness.reporting;

import com.intuit.karate.Results;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.TestUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class TestSuiteResultsTest {
    @InjectMock
    DataRepository dataRepository;

    @Test
    void emptyResults() {
        final TestSuiteResults testSuiteResults = TestSuiteResults.emptyResults();
        assertNull(testSuiteResults.getResults());
        assertEquals("", testSuiteResults.getErrorMessages());
        assertFalse(testSuiteResults.hasFailures());
        assertEquals(0, testSuiteResults.getFeatureTotal());
        assertEquals(0, testSuiteResults.getTimeTakenMillis());
        assertNotNull(testSuiteResults.getResultDate());
    }

    @Test
    void getFeatures() {
        final Results results = mock(Results.class);
        when(results.getSuite()).thenReturn(TestUtils.createEmptySuite());
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        assertNull(testSuiteResults.getFeatures());
    }

    @Test
    void getFeatureScores() {
        final TestSuiteResults testSuiteResults = TestSuiteResults.emptyResults();
        assertEquals(0, testSuiteResults.getFeatureScores().size());
    }

    @Test
    void getScenarioScores() {
        final TestSuiteResults testSuiteResults = TestSuiteResults.emptyResults();
        assertEquals(0, testSuiteResults.getScenarioScores().size());
    }

    @Test
    void getErrorMessages() {
        final Results results = mock(Results.class);
        when(results.getErrorMessages()).thenReturn("ERRORS");
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        assertEquals("ERRORS", testSuiteResults.getErrorMessages());
    }

    @Test
    void hasFailures() {
        final TestSuiteResults testSuiteResults = TestSuiteResults.emptyResults();
        addOutcomes(testSuiteResults);
        assertTrue(testSuiteResults.hasFailures());
    }

    @Test
    void hasFailuresSomeTolerable() {
        final TestSuiteResults testSuiteResults = TestSuiteResults.emptyResults();
        when(dataRepository.countToleratedFailures()).thenReturn(1);
        addOutcomes(testSuiteResults);
        assertTrue(testSuiteResults.hasFailures());
    }

    @Test
    void hasFailuresAllTolerable() {
        final TestSuiteResults testSuiteResults = TestSuiteResults.emptyResults();
        when(dataRepository.countToleratedFailures()).thenReturn(24);
        addOutcomes(testSuiteResults);
        assertFalse(testSuiteResults.hasFailures());
    }

    @Test
    void getFeatureTotal() {
        final TestSuiteResults testSuiteResults = TestSuiteResults.emptyResults();
        addOutcomes(testSuiteResults);
        assertEquals(18, testSuiteResults.getFeatureTotal());
    }

    @Test
    void getElapsedTime() {
        final TestSuiteResults testSuiteResults = TestSuiteResults.emptyResults();
        final long startTime = System.currentTimeMillis();
        testSuiteResults.setStartTime(startTime);
        assertTrue(testSuiteResults.getElapsedTime() >= 0);
    }

    @Test
    void getTimeTakenMillis() {
        final Results results = mock(Results.class);
        when(results.getTimeTakenMillis()).thenReturn(1234.0);
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        assertEquals(1234.0, testSuiteResults.getTimeTakenMillis());
    }

    @Test
    void getResultDate() {
        final Results results = mock(Results.class);
        final ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Z"));
        when(results.getEndTime()).thenReturn(now.toInstant().toEpochMilli());
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        assertEquals(testSuiteResults.getResultDate().toInstant().toEpochMilli(), now.toInstant().toEpochMilli());
    }

    @Test
    void log() {
        final Results results = mock(Results.class);
        final ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Z"));
        when(results.getEndTime()).thenReturn(now.toInstant().toEpochMilli());
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        addOutcomes(testSuiteResults);
        assertDoesNotThrow(testSuiteResults::log);
    }

    @Test
    void logEmpty() {
        final TestSuiteResults testSuiteResults = TestSuiteResults.emptyResults();
        assertDoesNotThrow(testSuiteResults::log);
    }

    @Test
    void testToString() {
        final TestSuiteResults testSuiteResults = TestSuiteResults.emptyResults();
        addOutcomes(testSuiteResults);
        assertEquals("Results:\n  MustFeatures  passed: 3, failed: 4\n  Total features:  18\n" +
                "  MustScenarios passed: 23, failed: 24\n  Total scenarios: 138", testSuiteResults.toString());
    }

    @Test
    void testToStringEmpty() {
        final TestSuiteResults testSuiteResults = TestSuiteResults.emptyResults();
        assertEquals("Results: No features were run", testSuiteResults.toString());
    }

    private void addOutcomes(final TestSuiteResults testSuiteResults) {
        when(dataRepository.getFeatureScores()).thenReturn(Map.of(
                TestSuiteResults.MUST, new Scores(1, 2, 0, 0, 0),
                TestSuiteResults.MUST_NOT, new Scores(2, 2, 1, 2, 3),
                "MAY", new Scores(1, 1, 1, 1, 1)
        ));
        when(dataRepository.getScenarioScores()).thenReturn(Map.of(
                TestSuiteResults.MUST, new Scores(11, 12, 0, 0, 0),
                TestSuiteResults.MUST_NOT, new Scores(12, 12, 11, 12, 13),
                "MAY", new Scores(11, 11, 11, 11, 11)
        ));
        testSuiteResults.summarizeOutcomes(dataRepository);
    }
}
