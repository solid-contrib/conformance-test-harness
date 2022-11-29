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
import com.intuit.karate.core.FeatureCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.utils.DataRepository;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.quarkiverse.loggingjson.providers.KeyValueStructuredArgument.*;

public class TestSuiteResults {
    private static final Logger resultLogger = LoggerFactory.getLogger("ResultLogger");
    public static final String MUST_NOT = "MUST-NOT";
    public static final String MUST = "MUST";
    final Results results;
    long startTime;
    Map<String, Scores> featureScores = new HashMap<>();
    Map<String, Scores> scenarioScores = new HashMap<>();
    int featuresTotal;
    int mustFeaturesPassed;
    int mustFeaturesFailed;
    int scenariosTotal;
    int mustScenariosPassed;
    int mustScenariosFailed;
    int toleratedScenariosFailing;

    public static TestSuiteResults emptyResults() {
        return new TestSuiteResults(null);
    }

    public TestSuiteResults(final Results results) {
        this.results = results;
    }

    public Results getResults() {
        return results;
    }

    public Map<String, Scores> getFeatureScores() {
        return featureScores;
    }

    public Map<String, Scores> getScenarioScores() {
        return scenarioScores;
    }

    public List<FeatureCall> getFeatures() {
        return this.results.getSuite().features;
    }

    public String getErrorMessages() {
        return results != null ? results.getErrorMessages() : "";
    }

    public boolean hasFailures() {
        return mustScenariosFailed - toleratedScenariosFailing != 0;
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
        featureScores = dataRepository.getFeatureScores();
        featuresTotal = Scores.calcScore(featureScores, null, null);
        mustFeaturesPassed = Scores.calcScore(featureScores, MUST, Scores.PASSED) +
                Scores.calcScore(featureScores, MUST_NOT, Scores.PASSED);
        mustFeaturesFailed = Scores.calcScore(featureScores, MUST, Scores.FAILED) +
                Scores.calcScore(featureScores, MUST_NOT, Scores.FAILED);
        scenarioScores = dataRepository.getScenarioScores();
        scenariosTotal = Scores.calcScore(scenarioScores, null, null);
        toleratedScenariosFailing = dataRepository.countToleratedFailures();
        mustScenariosPassed = Scores.calcScore(scenarioScores, MUST, Scores.PASSED) +
                Scores.calcScore(scenarioScores, MUST_NOT, Scores.PASSED);
        mustScenariosFailed = Scores.calcScore(scenarioScores, MUST, Scores.FAILED) +
                Scores.calcScore(scenarioScores, MUST_NOT, Scores.FAILED);
    }

    public void log() {
        final Scores mustFeatures = new Scores();
        mustFeatures.setScore(Scores.PASSED, mustFeaturesPassed);
        mustFeatures.setScore(Scores.FAILED, mustFeaturesFailed);
        final Scores mustScenarios = new Scores();
        mustScenarios.setScore(Scores.PASSED, mustScenariosPassed);
        mustScenarios.setScore(Scores.FAILED, mustScenariosFailed);
        resultLogger.info(this.toString(),
                kv("mustFeatures", mustFeatures),
                kv("features", featureScores),
                kv("mustScenarios", mustScenarios),
                kv("scenarios", scenarioScores),
                kv("toleratedScenariosFailing", toleratedScenariosFailing),
                kv("elapsedTime", getElapsedTime()),
                kv("totalTime", getTimeTakenMillis()),
                kv("resultDate", DateTimeFormatter.ISO_DATE_TIME.format(getResultDate()))
        );
    }

    @Override
    public String toString() {
        if (getFeatureTotal() > 0) {
            return String.format("Results:\n" +
                            "  MustFeatures  passed: %d, failed: %d\n  Total features:  %d\n" +
                            "  MustScenarios passed: %d, failed: %d\n  Total scenarios: %d",
                    mustFeaturesPassed, mustFeaturesFailed, featuresTotal,
                    mustScenariosPassed, mustScenariosFailed, scenariosTotal
            );
        } else {
            return "Results: No features were run";
        }
    }
}
