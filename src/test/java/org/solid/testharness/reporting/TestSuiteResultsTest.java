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
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.TestUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
    void getErrorMessages() {
        final Results results = mock(Results.class);
        when(results.getErrorMessages()).thenReturn("ERRORS");
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        assertEquals("ERRORS", testSuiteResults.getErrorMessages());
    }

    @Test
    void hasFailures() {
        final TestSuiteResults testSuiteResults = TestSuiteResults.emptyResults();
        testSuiteResults.scenarioScores.setScore(Scores.FAILED, 10);
        assertTrue(testSuiteResults.hasFailures());
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
    void getCounts() {
        final TestSuiteResults testSuiteResults = TestSuiteResults.emptyResults();
        addOutcomes(testSuiteResults);
        assertEquals(1, testSuiteResults.getCount(TestSuiteResults.MUST, Scores.PASSED));
        assertEquals(2, testSuiteResults.getCount(TestSuiteResults.MUST, Scores.FAILED));
        assertEquals(2, testSuiteResults.getCount(TestSuiteResults.MUST_NOT, Scores.PASSED));
        assertEquals(2, testSuiteResults.getCount(TestSuiteResults.MUST_NOT, Scores.UNTESTED));
        assertEquals(3, testSuiteResults.getCount(TestSuiteResults.MUST_NOT, Scores.INAPPLICABLE));
        assertEquals(3, testSuiteResults.getCount(TestSuiteResults.MUST, null));
        assertEquals(10, testSuiteResults.getCount(TestSuiteResults.MUST_NOT, null));
        assertEquals(5, testSuiteResults.getCount("MAY", null));
        assertEquals(4, testSuiteResults.getCount(null, Scores.PASSED));
        assertEquals(5, testSuiteResults.getCount(null, Scores.FAILED));
        assertEquals(2, testSuiteResults.getCount(null, Scores.CANTTELL));
        assertEquals(18, testSuiteResults.getCount(null, null));
    }

    @Test
    void getScenarioCounts() {
        final TestSuiteResults testSuiteResults = TestSuiteResults.emptyResults();
        addOutcomes(testSuiteResults);
        assertEquals(1, testSuiteResults.getScenarioCount(Scores.PASSED));
        assertEquals(2, testSuiteResults.getScenarioCount(Scores.FAILED));
        assertEquals(3, testSuiteResults.getScenarioCount(Scores.CANTTELL));
        assertEquals(4, testSuiteResults.getScenarioCount(Scores.UNTESTED));
        assertEquals(5, testSuiteResults.getScenarioCount(Scores.INAPPLICABLE));
        assertEquals(15, testSuiteResults.getScenarioCount(null));
    }

    @Test
    void toJson() {
        final Results results = mock(Results.class);
        final ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Z"));
        when(results.getEndTime()).thenReturn(now.toInstant().toEpochMilli());
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        addOutcomes(testSuiteResults);
        final String timestamp = DateTimeFormatter.ISO_DATE_TIME.format(
                Instant.ofEpochMilli(now.toInstant().toEpochMilli()).atZone(ZoneId.of("Z"))
        );

        final String json = testSuiteResults.toJson();
        assertTrue(json.contains("\"resultDate\":\"" + timestamp + "\""));
        assertTrue(json.contains("\"mustFeatures\":{\"passed\":3,\"failed\":4,\"total\":7}"));
        assertTrue(json.contains("\"MAY\":{\"passed\":1,\"failed\":1,\"cantTell\":1,\"untested\":1," +
                "\"inapplicable\":1,\"total\":5}"));
        assertTrue(json.contains("\"scenarios\":{\"passed\":1,\"failed\":2,\"cantTell\":3,\"untested\":4," +
                "\"inapplicable\":5,\"total\":15}"));
    }

    @Test
    void toJsonEmpty() {
        final TestSuiteResults testSuiteResults = TestSuiteResults.emptyResults();
        final String json = testSuiteResults.toJson();
        assertTrue(json.contains("\"mustFeatures\":{\"passed\":0,\"failed\":0,\"total\":0}"));
        assertTrue(json.contains("\"scenarios\":{\"total\":0}"));
    }

    @Test
    void toJsonFails() {
        final Results results = mock(Results.class);
        when(results.getTimeTakenMillis()).thenThrow(new RuntimeException("FAIL"));
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        assertEquals("{}", testSuiteResults.toJson());
    }

    @Test
    void testToString() {
        final TestSuiteResults testSuiteResults = TestSuiteResults.emptyResults();
        addOutcomes(testSuiteResults);
        assertEquals("Results:\n  MustFeatures passed: 3, failed: 4\n  Total features: 18\n" +
                "  Scenarios passed: 1, failed: 2, total: 15", testSuiteResults.toString());
    }

    @Test
    void testToStringEmpty() {
        final TestSuiteResults testSuiteResults = TestSuiteResults.emptyResults();
        assertEquals("Results: No features were run", testSuiteResults.toString());
    }

    private void addOutcomes(final TestSuiteResults testSuiteResults) {
        when(dataRepository.getOutcomeCounts()).thenReturn(Map.of(
                TestSuiteResults.MUST, new Scores(1, 2, 0, 0, 0),
                TestSuiteResults.MUST_NOT, new Scores(2, 2, 1, 2, 3),
                "MAY", new Scores(1, 1, 1, 1, 1)
        ));
        when(dataRepository.getScenarioCounts()).thenReturn(new Scores(1, 2, 3, 4, 5));
        testSuiteResults.summarizeOutcomes(dataRepository);
    }
}
