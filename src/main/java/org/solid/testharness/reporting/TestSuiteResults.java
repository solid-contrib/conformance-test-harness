package org.solid.testharness.reporting;

import com.intuit.karate.Results;

public class TestSuiteResults {
    Results results;

    public TestSuiteResults(final Results results) {
        this.results = results;
    }

    public String getErrorMessages() {
        return this.results.getErrorMessages();
    }

    public int getFailCount() {
        return this.results.getFailCount();
    }

    @Override
    public String toString() {
        return String.format("Results:\n  Features  passed: %d, failed: %d, total: %d\n" +
                        "  Scenarios passed: %d, failed: %d, total: %d",
                results.getFeaturesPassed(), results.getFeaturesFailed(), results.getFeaturesTotal(),
                results.getScenariosPassed(), results.getScenariosFailed(), results.getScenariosTotal());
    }
}
