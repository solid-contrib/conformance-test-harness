package org.solid.testharness.reporting;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.AbstractDataModelTests;

import java.time.LocalDate;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class AssertorTest extends AbstractDataModelTests {
    @Override
    public String getTestFile() {
        return "src/test/resources/assertor-testing-feature.ttl";
    }

    @Test
    void getSoftwareName() {
        final Assertor assertor = new Assertor(iri(NS, "tester1"));
        assertEquals("TESTER1", assertor.getSoftwareName());
    }

    @Test
    void getDescription() {
        final Assertor assertor = new Assertor(iri(NS, "tester1"));
        assertEquals("DESCRIPTION", assertor.getDescription());
    }

    @Test
    void getCreatedDate() {
        final Assertor assertor = new Assertor(iri(NS, "tester1"));
        assertEquals(LocalDate.parse("2021-01-01"), assertor.getCreatedDate());
    }

    @Test
    void getDeveloper() {
        final Assertor assertor = new Assertor(iri(NS, "tester1"));
        assertEquals("https://example.org/profile/card/#us", assertor.getDeveloper());
    }

    @Test
    void getHomepage() {
        final Assertor assertor = new Assertor(iri(NS, "tester1"));
        assertEquals("https://example.org/tester", assertor.getHomepage());
    }

    @Test
    void getRevision() {
        final Assertor assertor = new Assertor(iri(NS, "tester1"));
        assertEquals("0.0.1-SNAPSHOT", assertor.getRevision());
    }

    @Test
    void getRevisionNull() {
        final Assertor assertor = new Assertor(iri(NS, "tester2"));
        assertNull(assertor.getRevision());
    }
}
