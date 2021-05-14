package org.solid.testharness.reporting;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.solid.common.vocab.EARL;
import org.solid.testharness.utils.AbstractDataModelTests;
import org.solid.testharness.utils.Namespaces;
import org.solid.testharness.utils.TestData;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class AssertionTest extends AbstractDataModelTests {
    @Override
    public String getData() {
        return TestData.PREFIXES + "ex:assertion1 a earl:Assertion;\n" +
                "    earl:assertedBy " + Namespaces.TEST_HARNESS_PREFIX + ":;\n" +
                "    earl:test ex:test;\n" +
                "    earl:subject " + Namespaces.TEST_HARNESS_PREFIX + ":testserver;\n" +
                "    earl:mode earl:automatic;\n" +
                "    earl:result ex:result1 .\n" +
                "ex:result1 a earl:TestResult.\n" +
                "ex:assertion2 a earl:Assertion.";
    }

    @Test
    void getAssertedBy() {
        final Assertion assertion = new Assertion(iri(NS, "assertion1"));
        assertEquals(Namespaces.TEST_HARNESS_URI, assertion.getAssertedBy());
    }

    @Test
    void getTest() {
        final Assertion assertion = new Assertion(iri(NS, "assertion1"));
        assertEquals(TestData.SAMPLE_BASE + "/test", assertion.getTest());
    }

    @Test
    void getTestSubject() {
        final Assertion assertion = new Assertion(iri(NS, "assertion1"));
        assertEquals(Namespaces.TEST_HARNESS_URI + "testserver", assertion.getTestSubject());
    }

    @Test
    void getMode() {
        final Assertion assertion = new Assertion(iri(NS, "assertion1"));
        assertEquals(EARL.automatic.stringValue(), assertion.getMode());
    }

    @Test
    void getResult() {
        final Assertion assertion = new Assertion(iri(NS, "assertion1"));
        assertNotNull(assertion.getResult());
    }

    @Test
    void getNoResult() {
        final Assertion assertion = new Assertion(iri(NS, "assertion2"));
        assertNull(assertion.getResult());
    }
}
