package org.solid.testharness.reporting;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.AbstractDataModelTests;
import org.solid.testharness.utils.TestData;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ScenarioTest extends AbstractDataModelTests {
    @Override
    public String getData() {
        return TestData.PREFIXES + "ex:scenario1 a earl:TestCriterion, earl:TestCase;\n" +
                "    dcterms:title \"TITLE\";\n" +
                "    dcterms:isPartOf ex:parent;\n" +
                "    earl:assertions ex:assertion;\n" +
                "    earl:steps ex:steps .\n" +
                "ex:assertion a earl:Assertion .\n" +
                "ex:steps a rdf:List;\n" +
                "    rdf:first ex:step1;\n" +
                "    rdf:rest (ex:step2).\n" +
                "ex:step1 a earl:TestStep;\n" +
                "    dcterms:title \"STEP1\";\n" +
                "    earl:outcome earl:passed .\n" +
                "ex:step2 a earl:TestStep;\n" +
                "    dcterms:title \"STEP2\";\n" +
                "    earl:outcome earl:passed .\n" +
                "ex:scenario2 a earl:TestCriterion, earl:TestCase.\n";
    }

    @Test
    void getTitle() {
        final Scenario scenario = new Scenario(iri(NS, "scenario1"));
        assertEquals("TITLE", scenario.getTitle());
    }

    @Test
    void getParent() {
        final Scenario scenario = new Scenario(iri(NS, "scenario1"));
        assertEquals(TestData.SAMPLE_BASE + "/parent", scenario.getParent());
    }

    @Test
    void getAssertion() {
        final Scenario scenario = new Scenario(iri(NS, "scenario1"));
        assertNotNull(scenario.getAssertion());
    }

    @Test
    void getNoAssertion() {
        final Scenario scenario = new Scenario(iri(NS, "scenario2"));
        assertNull(scenario.getAssertion());
    }

    @Test
    void getSteps() {
        final Scenario scenario = new Scenario(iri(NS, "scenario1"));
        assertFalse(scenario.getSteps().isEmpty());
    }

    @Test
    void getNoSteps() {
        final Scenario scenario = new Scenario(iri(NS, "scenario2"));
        assertNull(scenario.getSteps());
    }
}
