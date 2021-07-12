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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.karate.Results;

import javax.enterprise.inject.spi.CDI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class TestSuiteResults {
    Results results;
    long startTime;

    public TestSuiteResults(final Results results) {
        this.results = results;
    }

    public String getErrorMessages() {
        return this.results.getErrorMessages();
    }

    public int getFailCount() {
        return this.results.getFailCount();
    }

    public int getFeatureFailCount() {
        return this.results.getFeaturesFailed();
    }

    public int getFeaturePassCount() {
        return this.results.getFeaturesPassed();
    }

    public int getFeatureTotal() {
        return this.results.getFeaturesTotal();
    }

    public int getScenarioFailCount() {
        return this.results.getScenariosFailed();
    }

    public int getScenarioPassCount() {
        return this.results.getScenariosPassed();
    }

    public int getScenarioTotal() {
        return this.results.getScenariosTotal();
    }

    public void setStartTime(final long startTime) {
        this.startTime = startTime;
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    public Date getResultDate() {
        return new Date(this.results.getEndTime());
    }

    public String toJson() throws JsonProcessingException {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault());
        final ObjectMapper objectMapper = CDI.current().select(ObjectMapper.class).get();
        return objectMapper.writeValueAsString(Map.of(
        "featuresPassed", this.results.getFeaturesPassed(),
        "featuresFailed", this.results.getFeaturesFailed(),
        "featuresSkipped", (int) this.results.toKarateJson().get("featuresSkipped"),
        "scenariosPassed", this.results.getScenariosPassed(),
        "scenariosFailed", this.results.getScenariosFailed(),
        "elapsedTime", this.results.getElapsedTime(),
        "totalTime", this.results.getTimeTakenMillis(),
        "resultDate", sdf.format(getResultDate())
        ));
    }

    @Override
    public String toString() {
        return String.format("Results:\n  Features  passed: %d, failed: %d, total: %d\n" +
                        "  Scenarios passed: %d, failed: %d, total: %d",
                results.getFeaturesPassed(), results.getFeaturesFailed(), results.getFeaturesTotal(),
                results.getScenariosPassed(), results.getScenariosFailed(), results.getScenariosTotal());
    }
}
