package org.solid.testharness.reporting;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.AbstractDataModelTests;

import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class ResultDataTest extends AbstractDataModelTests {
    @Override
    public String getData() {
        return "@prefix td: <http://www.w3.org/2006/03/test-description#> .\n" +
                "@prefix ex: <http://example.org/> .\n" +
                "@prefix doap: <http://usefulinc.com/ns/doap#> .\n" +
                "@prefix dcterms: <http://purl.org/dc/terms/> .\n" +
                "<http://example.org/> doap:implements <https://solidproject.org/TR/protocol#spec1> ;\n" +
                "  dcterms:hasPart ex:test1, ex:test2 .\n" +
                "ex:test1 a td:SpecificationTestCase .\n" +
                "ex:test2 a td:SpecificationTestCase .";
    }

    @Test
    void getPrefixes() {
        final ResultData resultData = new ResultData(iri("http://example.org/"));
        assertEquals("xsd: http://www.w3.org/2001/XMLSchema# " +
                "dcterms: http://purl.org/dc/terms/ " +
                "doap: http://usefulinc.com/ns/doap# " +
                "solid: http://www.w3.org/ns/solid/terms# " +
                "solid-test: https://github.com/solid/conformance-test-harness/vocab# " +
                "earl: http://www.w3.org/ns/earl# " +
                "td: http://www.w3.org/2006/03/test-description#", resultData.getPrefixes());
    }

    @Test
    void getSpecificationTestCases() {
        final ResultData resultData = new ResultData(iri("http://example.org/"));
        final List<SpecificationTestCase> testCases = resultData.getSpecificationTestCases();
        assertNotNull(testCases);
        assertEquals(2, testCases.size());
    }

    @Test
    void getSpecification() {
        final ResultData resultData = new ResultData(iri("http://example.org/"));
        assertEquals("https://solidproject.org/TR/protocol#spec1", resultData.getSpecification());
    }
}
