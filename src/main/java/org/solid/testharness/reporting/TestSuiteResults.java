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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.karate.Results;
import com.intuit.karate.core.Feature;
import org.apache.commons.lang3.StringUtils;
import org.solid.testharness.utils.DataRepository;

import javax.enterprise.inject.spi.CDI;
import java.text.SimpleDateFormat;
import java.util.*;

public class TestSuiteResults {
    final Results results;
    long startTime;
    Map<String, Integer> counts = new HashMap<>();

    public static TestSuiteResults emptyResults() {
        return new TestSuiteResults(null);
    }

    public TestSuiteResults(final Results results) {
        this.results = results;
    }

    public List<Feature> getFeatures() {
        return this.results.getSuite().features;
    }

    public String getErrorMessages() {
        return results != null ? results.getErrorMessages() : "";
    }

    public int getFailCount() {
        return results != null ? results.getFailCount() : 0;
    }

    public int getFeatureFailCount() {
        return results != null ? results.getFeaturesFailed() : 0;
    }

    public int getFeaturePassCount() {
        return results != null ? results.getFeaturesPassed() : 0;
    }

    public int getFeatureSkipCount() {
        return results != null ? (int) results.toKarateJson().get("featuresSkipped") : 0;
    }

    public int getFeatureTotal() {
        return results != null ? results.getFeaturesTotal() : 0;
    }

    public int getScenarioFailCount() {
        return results != null ? results.getScenariosFailed() : 0;
    }

    public int getScenarioPassCount() {
        return results != null ? results.getScenariosPassed() : 0;
    }

    public int getScenarioTotal() {
        return results != null ? results.getScenariosTotal() : 0;
    }

    public void setStartTime(final long startTime) {
        this.startTime = startTime;
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    public double getTimeTakenMillis() {
        return results != null ? results.getTimeTakenMillis() : 0;
    }

    public Date getResultDate() {
        return results != null ? new Date(results.getEndTime()) : new Date();
    }

    public void summarizeOutcomes(final DataRepository dataRepository) {
        counts = dataRepository.getOutcomeCounts();
    }

    public int getCount(final String level, final String outcome) {
        final String keyPattern = StringUtils.defaultString(level) + ":" + StringUtils.defaultString(outcome);
        if (counts.containsKey(keyPattern)) {
            return counts.get(keyPattern);
        } else {
            return counts.entrySet().stream()
                    .filter((entry) -> entry.getKey().contains(keyPattern))
                    .mapToInt(Map.Entry::getValue)
                    .sum();
        }
    }

    public String toJson() {
        try {
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault());
            final ObjectMapper objectMapper = CDI.current().select(ObjectMapper.class).get();
            final Map<String, Object> resultData = new HashMap<>();
            resultData.put("mustFeaturesPassed", getCount("MUST", "passed") + getCount("MUST-NOT", "passed"));
            resultData.put("mustFeaturesFailed", getCount("MUST", "failed") + getCount("MUST-NOT", "failed"));
            resultData.put("featuresPassed", getFeaturePassCount());
            resultData.put("featuresFailed", getFeatureFailCount());
            resultData.put("featuresSkipped", getFeatureSkipCount());
            resultData.put("scenariosPassed", getScenarioPassCount());
            resultData.put("scenariosFailed", getScenarioFailCount());
            resultData.put("scenariosTotal", getScenarioTotal());
            resultData.put("elapsedTime", getElapsedTime());
            resultData.put("totalTime", getTimeTakenMillis());
            resultData.put("resultDate", sdf.format(getResultDate()));
            resultData.putAll(counts);
            return objectMapper.writeValueAsString(resultData);
        } catch (Exception e) {
            return "{}";
        }
    }

    @Override
    public String toString() {
        if (getFeatureTotal() > 0) {
            return String.format("Results:\n  Features  passed: %d, failed: %d, total: %d\n" +
                            "  Scenarios passed: %d, failed: %d, total: %d",
                    results.getFeaturesPassed(), results.getFeaturesFailed(), results.getFeaturesTotal(),
                    results.getScenariosPassed(), results.getScenariosFailed(), results.getScenariosTotal());
        } else {
            return "Results: No features were run";
        }
    }
}
