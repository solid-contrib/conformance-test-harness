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

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TestCaseTest extends AbstractDataModelTests  {
    @Override
    public String getTestFile() {
        return "src/test/resources/reporting/testcase-testing-feature.ttl";
    }

    @Test
    void getTitle() {
        final TestCase testCase = new TestCase(iri(NS, "test1"));
        assertEquals("Title", testCase.getTitle());
    }

    @Test
    void getTestScript() {
        final TestCase testCase = new TestCase(iri(NS, "test1"));
        assertEquals("https://example.org/test3.feature", testCase.getTestScript());
    }

    @Test
    void getRequirementReference() {
        final TestCase testCase = new TestCase(iri(NS, "test1"));
        assertEquals("https://example.org/specification1#requriement1", testCase.getRequirementReference());
    }

    @Test
    void getStatusUnreviewed() {
        final TestCase testCase = new TestCase(iri(NS, "test1"));
        assertEquals("Unreviewed", testCase.getStatus());
    }

    @Test
    void getStatusAccepted() {
        final TestCase testCase = new TestCase(iri(NS, "test2"));
        assertEquals("Accepted", testCase.getStatus());
    }

    @Test
    void isImplemented() {
        final TestCase testCase = new TestCase(iri(NS, "test1"));
        assertTrue(testCase.isImplemented());
    }

    @Test
    void isNotImplemented() {
        final TestCase testCase = new TestCase(iri(NS, "test2"));
        assertFalse(testCase.isImplemented());
    }

    @Test
    void getAssertion() {
        final TestCase testCase = new TestCase(iri(NS, "test1"));
        assertNotNull(testCase.getAssertion());
    }

    @Test
    void getAssertionNull() {
        final TestCase testCase = new TestCase(iri(NS, "test2"));
        assertNull(testCase.getAssertion());
    }

    @Test
    void getScenarios() {
        final TestCase testCase = new TestCase(iri(NS, "test1"));
        assertNotNull(testCase.getScenarios().get(0));
    }

    @Test
    void getScenariosEmpty() {
        final TestCase testCase = new TestCase(iri(NS, "test2"));
        assertNull(testCase.getScenarios());
    }
}
