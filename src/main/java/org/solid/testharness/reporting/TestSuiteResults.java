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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestSuiteResults {
    public static final String MUST_NOT = "MUST-NOT";
    public static final String MUST = "MUST";
    final Results results;
    long startTime;
    Map<String, Scores> counts = new HashMap<>();
    Scores scenarioScores = new Scores();
    int featuresTotal;

    public static TestSuiteResults emptyResults() {
        return new TestSuiteResults(null);
    }

    public TestSuiteResults(final Results results) {
        this.results = results;
    }

    public Results getResults() {
        return results;
    }

    public List<Feature> getFeatures() {
        return this.results.getSuite().features;
    }

    public String getErrorMessages() {
        return results != null ? results.getErrorMessages() : "";
    }

    public boolean hasFailures() {
        return getScenarioCount(Scores.FAILED) != 0;
    }

    public int getFeatureTotal() {
        return featuresTotal;
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

    public ZonedDateTime getResultDate() {
        return results != null
                ? Instant.ofEpochMilli(results.getEndTime()).atZone(ZoneId.of("Z"))
                : ZonedDateTime.now();
    }

    public void summarizeOutcomes(final DataRepository dataRepository) {
        counts = dataRepository.getOutcomeCounts();
        scenarioScores = dataRepository.getScenarioCounts();
        featuresTotal = getCount(null, null);
    }

    public int getCount(final String level, final String outcome) {
        if (!StringUtils.isEmpty(level)) {
            if (counts.containsKey(level)) {
                if (StringUtils.isEmpty(outcome)) {
                    return counts.get(level).getTotal();
                } else {
                    return counts.get(level).getScore(outcome);
                }
            } else {
                return 0;
            }
        } else {
            return counts.values().stream()
                    .mapToInt(s -> StringUtils.isEmpty(outcome) ? s.getTotal() : s.getScore(outcome))
                    .sum();
        }
    }

    public int getScenarioCount(final String outcome) {
        if (StringUtils.isEmpty(outcome)) {
            return scenarioScores.getTotal();
        } else {
            return scenarioScores.getScore(outcome);
        }
    }

    public String toJson() {
        try {
            final ObjectMapper objectMapper = CDI.current().select(ObjectMapper.class).get();
            final Map<String, Object> resultData = new HashMap<>();
            final Scores mustFeatures = new Scores();
            mustFeatures.setScore(Scores.PASSED, getCount(MUST, Scores.PASSED) + getCount(MUST_NOT, Scores.PASSED));
            mustFeatures.setScore(Scores.FAILED, getCount(MUST, Scores.FAILED) + getCount(MUST_NOT, Scores.FAILED));
            resultData.put("mustFeatures", mustFeatures);
            resultData.put("scenarios", scenarioScores);
            resultData.put("elapsedTime", getElapsedTime());
            resultData.put("totalTime", getTimeTakenMillis());
            resultData.put("resultDate", DateTimeFormatter.ISO_DATE_TIME.format(getResultDate()));
            resultData.putAll(counts);
            return objectMapper.writeValueAsString(resultData);
        } catch (Exception e) {
            return "{}";
        }
    }

    @Override
    public String toString() {
        if (getFeatureTotal() > 0) {
            return String.format("Results:\n  MustFeatures passed: %d, failed: %d\n  Total features: %d\n" +
                            "  Scenarios passed: %d, failed: %d, total: %d",
                    getCount(MUST, Scores.PASSED) + getCount(MUST_NOT, Scores.PASSED),
                    getCount(MUST, Scores.FAILED) + getCount(MUST_NOT, Scores.FAILED),
                    featuresTotal,
                    scenarioScores.getScore(Scores.PASSED), scenarioScores.getScore(Scores.FAILED),
                    scenarioScores.getTotal());
        } else {
            return "Results: No features were run";
        }
    }
}
