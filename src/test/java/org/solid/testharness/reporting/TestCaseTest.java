package org.solid.testharness.reporting;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.solid.common.vocab.EARL;
import org.solid.testharness.utils.AbstractDataModelTests;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TestCaseTest extends AbstractDataModelTests  {
    @Override
    public String getTestFile() {
        return "src/test/resources/testcase-testing-feature.ttl";
    }

    @Test
    void getTitle() {
        final TestCase testCase = new TestCase(iri(NS, "test1"));
        assertEquals("Title", testCase.getTitle());
    }

    @Test
    void getLevel() {
        final TestCase testCase = new TestCase(iri(NS, "test1"));
        assertEquals("MUST", testCase.getLevel());
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
    void isImplementedPassed() {
        final TestCase testCase = new TestCase(iri(NS, "test3"));
        assertTrue(testCase.isImplemented());
    }

    @Test
    void getModeAsIri() {
        final TestCase testCase = new TestCase(iri(NS, "test3"));
        assertEquals(EARL.passed, testCase.getModeAsIri());
    }

    @Test
    void getAssertionNull() {
        final TestCase testCase = new TestCase(iri(NS, "test1"));
        assertNotNull(testCase.getAssertion());
    }

    @Test
    void getAssertion() {
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
