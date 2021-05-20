package org.solid.testharness.reporting;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.solid.common.vocab.EARL;
import org.solid.testharness.utils.AbstractDataModelTests;
import org.solid.testharness.utils.TestData;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TestCaseTest extends AbstractDataModelTests  {
    @Override
    public String getData() {
        return TestData.PREFIXES +
                "ex:test1\n" +
                "    a earl:TestCase, earl:TestCriterion, earl:TestFeature ;\n" +
                "    dcterms:title \"Title\" ;\n" +
                "    dcterms:subject \"MUST\" ;\n" +
                "    td:reviewStatus td:unreviewed ;\n" +
                "    earl:hasOutcome \"TBD\" ;\n" +
                "    earl:assertions ex:assertion ;\n" +
                "    dcterms:hasPart ex:test3 .\n" +
                "ex:assertion a earl:Assertion .\n" +
                "ex:test2\n" +
                "    a earl:TestCase ;\n" +
                "    td:reviewStatus td:accepted ;\n" +
                "    earl:mode earl:untested .\n" +
                "ex:test3\n" +
                "    a earl:TestCase ;\n" +
                "    earl:mode earl:passed .";

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
