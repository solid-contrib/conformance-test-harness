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
    void getDate() {
        final TestResult testResult = new TestResult(iri(NS, "result1"));
        assertTrue(testResult.getDate().isEqual(ZonedDateTime.parse("2021-04-06T17:41:20.889+01:00")));
    }
}
