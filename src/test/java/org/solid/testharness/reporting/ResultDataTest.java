package org.solid.testharness.reporting;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.AbstractDataModelTests;
import org.solid.testharness.utils.TestData;

import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ResultDataTest extends AbstractDataModelTests {
    @Override
    public String getTestFile() {
        return "src/test/resources/resultdata-testing-feature.ttl";
    }

    @Test
    void getHtmlPrefixes() {
        final ResultData resultData = new ResultData(iri(TestData.SAMPLE_NS));
        assertEquals("rdf: http://www.w3.org/1999/02/22-rdf-syntax-ns# " +
                "rdfs: http://www.w3.org/2000/01/rdf-schema# " +
                "xsd: http://www.w3.org/2001/XMLSchema# " +
                "dcterms: http://purl.org/dc/terms/ " +
                "doap: http://usefulinc.com/ns/doap# " +
                "solid: http://www.w3.org/ns/solid/terms# " +
                "solid-test: https://github.com/solid/conformance-test-harness/vocab# " +
                "earl: http://www.w3.org/ns/earl# " +
                "td: http://www.w3.org/2006/03/test-description#", resultData.getPrefixes());
    }

    @Test
    void getSpecificationTestCases() {
        final ResultData resultData = new ResultData(iri(TestData.SAMPLE_NS));
        final List<SpecificationTestCase> testCases = resultData.getSpecificationTestCases();
        assertNotNull(testCases);
        assertEquals(2, testCases.size());
    }

    @Test
    void getSpecification() {
        final ResultData resultData = new ResultData(iri(TestData.SAMPLE_NS));
        assertEquals("https://solidproject.org/TR/protocol#spec1", resultData.getSpecification());
    }

    @Test
    void getAssertor() {
        final ResultData resultData = new ResultData(iri(TestData.SAMPLE_NS));
        assertNotNull(resultData.getAssertor());
    }
}
