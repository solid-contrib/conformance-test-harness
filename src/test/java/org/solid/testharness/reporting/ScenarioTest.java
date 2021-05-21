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
    public String getTestFile() {
        return "src/test/resources/scenario-testing-feature.ttl";
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
