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
    public String getData() {
        return "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
                "@prefix dcterms: <http://purl.org/dc/terms/> .\n" +
                "@prefix earl: <http://www.w3.org/ns/earl#> .\n" +
                "@prefix td: <http://www.w3.org/2006/03/test-description#> .\n" +
                "@prefix ex: <http://example.org/> .\n" +
                "ex:test1\n" +
                "    a td:SpecificationTestCase ;\n" +
                "    dcterms:title \"Title\" ;\n" +
                "    dcterms:description \"Description\" ;\n" +
                "    td:specificationReference ex:spec ;\n" +
                "    dcterms:hasPart ex:testcase ." +
                "ex:testcase\n" +
                "    a earl:TestCase ;\n" +
                "    td:reviewStatus td:accepted .\n" +
                "ex:test2\n" +
                "    a td:SpecificationTestCase .";
    }

    @Test
    void getTitle() {
        SpecificationTestCase specificationTestCase = new SpecificationTestCase(iri(NS, "test1"));
        assertEquals("Title", specificationTestCase.getTitle());
    }

    @Test
    void getDescription() {
        SpecificationTestCase specificationTestCase = new SpecificationTestCase(iri(NS, "test1"));
        assertEquals("Description", specificationTestCase.getDescription());
    }

    @Test
    void getSpecificationReference() {
        SpecificationTestCase specificationTestCase = new SpecificationTestCase(iri(NS, "test1"));
        assertEquals(NS + "spec", specificationTestCase.getSpecificationReference());
    }

    @Test
    void getTestCases() {
        SpecificationTestCase specificationTestCase = new SpecificationTestCase(iri(NS, "test1"));
        List<TestCase> testCases = specificationTestCase.getTestCases();
        assertNotNull(testCases);
        assertEquals(1, testCases.size());
    }

    @Test
    void getEmptyTestCases() {
        SpecificationTestCase specificationTestCase = new SpecificationTestCase(iri(NS, "test2"));
        List<TestCase> testCases = specificationTestCase.getTestCases();
        assertNull(testCases);
    }
}
