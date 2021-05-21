package org.solid.testharness.reporting;

import com.intuit.karate.Results;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TestSuiteResultsTest {
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
        when(results.getFailCount()).thenReturn(1);
        final TestSuiteResults testSuiteResults = new TestSuiteResults(results);
        assertEquals(1, testSuiteResults.getFailCount());
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
}
