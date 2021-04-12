package org.solid.testharness.reporting;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.AbstractDataModelTests;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class TestCaseTest extends AbstractDataModelTests  {
    @Override
    public String getData() {
        return "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
                "@prefix dcterms: <http://purl.org/dc/terms/> .\n" +
                "@prefix earl: <http://www.w3.org/ns/earl#> .\n" +
                "@prefix td: <http://www.w3.org/2006/03/test-description#> .\n" +
                "@prefix ex: <http://example.org/> .\n" +
                "ex:test1\n" +
                "    a earl:TestCase ;\n" +
                "    dcterms:title \"Title\" ;\n" +
                "    dcterms:subject \"MUST\" ;\n" +
                "    td:reviewStatus td:unreviewed ;\n" +
                "    earl:hasOutcome \"TBD\" ." +
                "ex:test2\n" +
                "    a earl:TestCase ;\n" +
                "    td:reviewStatus td:accepted .";
    }

    @Test
    void getTitle() {
        TestCase testCase = new TestCase(iri(NS, "test1"));
        assertEquals("Title", testCase.getTitle());
    }

    @Test
    void getLevel() {
        TestCase testCase = new TestCase(iri(NS, "test1"));
        assertEquals("MUST", testCase.getLevel());
    }

    @Test
    void getStatusUnreviewed() {
        TestCase testCase = new TestCase(iri(NS, "test1"));
        assertEquals("Unreviewed", testCase.getStatus());
    }

    @Test
    void getStatusAccepted() {
        TestCase testCase = new TestCase(iri(NS, "test2"));
        assertEquals("Accepted", testCase.getStatus());
    }

}
