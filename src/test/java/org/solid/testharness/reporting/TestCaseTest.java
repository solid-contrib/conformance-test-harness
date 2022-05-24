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

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.solid.common.vocab.TD;
import org.solid.testharness.utils.AbstractDataModelTests;
import org.solid.testharness.utils.Namespaces;

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
        assertEquals("https://example.org/specification1#requirement1", testCase.getRequirementReference());
    }

    @Test
    void getRequirementAnchor() {
        Namespaces.clearSpecificationNamespaces();
        final TestCase testCase = new TestCase(iri(NS, "test1"));
        assertEquals("null_requirement1", testCase.getRequirementAnchor());
    }

    @Test
    void getRequirementAnchorNull() {
        final TestCase testCase = new TestCase(iri(NS, "test2"));
        assertNull(testCase.getRequirementAnchor());
    }

    @Test
    void getStatusUnreviewed() {
        final TestCase testCase = new TestCase(iri(NS, "test1"));
        assertEquals(TD.unreviewed.stringValue(), testCase.getStatus());
    }

    @Test
    void getStatusAccepted() {
        final TestCase testCase = new TestCase(iri(NS, "test2"));
        assertEquals(TD.accepted.stringValue(), testCase.getStatus());
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
        assertEquals(0, testCase.countScenarios());
        assertEquals(0, testCase.countPassed());
        assertEquals(0, testCase.countFailed());
        assertFalse(testCase.isPassed());
        assertFalse(testCase.isFailed());
    }

    @Test
    void countScenarios() {
        final TestCase testCase = new TestCase(iri(NS, "test1"));
        assertEquals(2, testCase.countScenarios());
    }

    @Test
    void countFailed() {
        final TestCase testCase = new TestCase(iri(NS, "test1"));
        assertEquals(1, testCase.countFailed());
    }

    @Test
    void countPassed() {
        final TestCase testCase = new TestCase(iri(NS, "test1"));
        assertEquals(1, testCase.countPassed());
    }

    @Test
    void failedFalse() {
        final TestCase testCase = new TestCase(iri(NS, "testPass"));
        assertFalse(testCase.isFailed());
    }

    @Test
    void failedTrue() {
        final TestCase testCase = new TestCase(iri(NS, "testFail"));
        assertTrue(testCase.isFailed());
    }

    @Test
    void passedFalse() {
        final TestCase testCase = new TestCase(iri(NS, "testFail"));
        assertFalse(testCase.isPassed());
    }

    @Test
    void passedTrue() {
        final TestCase testCase = new TestCase(iri(NS, "testPass"));
        assertTrue(testCase.isPassed());
    }
}
