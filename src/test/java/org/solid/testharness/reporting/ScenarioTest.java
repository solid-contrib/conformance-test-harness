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

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.AbstractDataModelTests;

import java.time.ZonedDateTime;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ScenarioTest extends AbstractDataModelTests {
    @Override
    public String getTestFile() {
        return "src/test/resources/reporting/scenario-testing-feature.ttl";
    }

    @Test
    void getTitle() {
        final Scenario scenario = new Scenario(iri(NS, "scenario1"));
        assertEquals("TITLE", scenario.getTitle());
    }

    @Test
    void getScriptLocation() {
        final Scenario scenario = new Scenario(iri(NS, "scenario1"));
        assertEquals("https://example.org/test.feature#line=10,15", scenario.getUsed());
    }

    @Test
    void getStartTime() {
        final Scenario scenario = new Scenario(iri(NS, "scenario1"));
        assertTrue(scenario.getStartTime().isEqual(ZonedDateTime.parse("2021-04-15T13:00:00-04:00")));
    }

    @Test
    void getEndTime() {
        final Scenario scenario = new Scenario(iri(NS, "scenario1"));
        assertTrue(scenario.getEndTime().isEqual(ZonedDateTime.parse("2021-04-15T13:01:00-04:00")));
    }

    @Test
    void getGeneratedOutput() {
        final Scenario scenario = new Scenario(iri(NS, "scenario1"));
        assertNotNull(scenario.getGeneratedOutput());
    }

    @Test
    void getNoGeneratedOutput() {
        final Scenario scenario = new Scenario(iri(NS, "scenario2"));
        assertNull(scenario.getGeneratedOutput());
    }

    @Test
    void getSteps() {
        final Scenario scenario = new Scenario(iri(NS, "scenario1"));
        assertFalse(scenario.getSteps().isEmpty());
    }

    @Test
    void getNoSteps() {
        final Scenario scenario = new Scenario(iri(NS, "scenario2"));
        assertNull(scenario.getSteps());
    }

    @Test
    void getNoPassFail() {
        final Scenario scenario = new Scenario(iri(NS, "scenario1"));
        assertFalse(scenario.isFailed());
        assertFalse(scenario.isPassed());
    }

    @Test
    void isFailed() {
        final Scenario scenario = new Scenario(iri(NS, "scenarioFail"));
        assertTrue(scenario.isFailed());
    }

    @Test
    void isPassed() {
        final Scenario scenario = new Scenario(iri(NS, "scenarioPass"));
        assertTrue(scenario.isPassed());
    }

}
