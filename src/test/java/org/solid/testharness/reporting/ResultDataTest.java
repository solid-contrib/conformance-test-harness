package org.solid.testharness.reporting;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.AbstractDataModelTests;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class ResultDataTest extends AbstractDataModelTests {
    @Override
    public String getData() {
        return "@prefix td: <http://www.w3.org/2006/03/test-description#> .\n" +
                "@prefix ex: <http://example.org/> .\n" +
                "ex:test1 a td:SpecificationTestCase .\n" +
                "ex:test2 a td:SpecificationTestCase .";
    }

    @Test
    void getPrefixes() {
        ResultData resultData = new ResultData();
        assertEquals("xsd: http://www.w3.org/2001/XMLSchema# dcterms: http://purl.org/dc/terms/ doap: http://usefulinc.com/ns/doap# solid: http://www.w3.org/ns/solid/terms# solid-test: https://github.com/solid/conformance-test-harness/vocab# earl: http://www.w3.org/ns/earl# td: http://www.w3.org/2006/03/test-description#", resultData.getPrefixes());
    }

    @Test
    void getSpecificationTestCases() {
        ResultData resultData = new ResultData();
        List<SpecificationTestCase> testCases = resultData.getSpecificationTestCases();
        assertNotNull(testCases);
        assertEquals(2, testCases.size());
    }

    @Test
    void getSpecification() {
        ResultData resultData = new ResultData();
        assertEquals("Solid Spec", resultData.getSpecification());
    }
}
