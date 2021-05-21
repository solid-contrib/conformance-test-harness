package org.solid.testharness.reporting;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.AbstractDataModelTests;

import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SpecificationTestCaseTest extends AbstractDataModelTests {
    @Override
    public String getTestFile() {
        return "src/test/resources/specificationtestcase-testing-feature.ttl";
    }

    @Test
    void getTitle() {
        final SpecificationTestCase specificationTestCase = new SpecificationTestCase(iri(NS, "test1"));
        assertEquals("Title", specificationTestCase.getTitle());
    }

    @Test
    void getDescription() {
        final SpecificationTestCase specificationTestCase = new SpecificationTestCase(iri(NS, "test1"));
        assertEquals("Description", specificationTestCase.getDescription());
    }

    @Test
    void getSpecificationReference() {
        final SpecificationTestCase specificationTestCase = new SpecificationTestCase(iri(NS, "test1"));
        assertEquals(NS + "spec", specificationTestCase.getSpecificationReference());
    }

    @Test
    void getTestCases() {
        final SpecificationTestCase specificationTestCase = new SpecificationTestCase(iri(NS, "test1"));
        final List<TestCase> testCases = specificationTestCase.getTestCases();
        assertNotNull(testCases);
        assertEquals(1, testCases.size());
    }

    @Test
    void getEmptyTestCases() {
        final SpecificationTestCase specificationTestCase = new SpecificationTestCase(iri(NS, "test2"));
        final List<TestCase> testCases = specificationTestCase.getTestCases();
        assertNull(testCases);
    }
}
