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
import org.solid.common.vocab.EARL;
import org.solid.testharness.utils.AbstractDataModelTests;

import java.time.ZonedDateTime;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TestResultTest extends AbstractDataModelTests {
    @Override
    public String getTestFile() {
        return "src/test/resources/reporting/testresult-testing-feature.ttl";
    }

    @Test
    void getOutcome() {
        final TestResult testResult = new TestResult(iri(NS, "result1"));
        assertEquals(EARL.passed.stringValue(), testResult.getOutcome());
    }

    @Test
    void isPassed() {
        final TestResult testResult = new TestResult(iri(NS, "result1"));
        assertTrue(testResult.isPassed());
    }

    @Test
    void isFailed() {
        final TestResult testResult = new TestResult(iri(NS, "result2"));
        assertTrue(testResult.isFailed());
    }

    @Test
    void getOutcomeLocalName() {
        final TestResult testResult = new TestResult(iri(NS, "result1"));
        assertEquals(EARL.passed.getLocalName(), testResult.getOutcomeLocalName());
    }

    @Test
    void getDate() {
        final TestResult testResult = new TestResult(iri(NS, "result1"));
        assertTrue(testResult.getDate().isEqual(ZonedDateTime.parse("2021-04-06T17:41:20.889001Z")));
    }
}
