package org.solid.testharness.accesscontrol;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.solid.common.vocab.RDF;
import org.solid.common.vocab.VCARD;
import org.solid.testharness.http.Client;
import org.solid.testharness.utils.TestUtils;

import java.io.IOException;
import java.net.URI;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccessDatasetTest {
    Model testModel;

    @BeforeAll
    void setup() throws IOException {
        testModel = TestUtils.loadTurtleFromFile("src/test/resources/utils/vcard.ttl");
    }

    @Test
    void getMode() {
        final AccessDataset accessDataset = new TestAccessDataset();
        assertEquals("TEST", accessDataset.getMode());
    }

    @Test
    void getSetModel() {
        final AccessDataset accessDataset = new TestAccessDataset();
        assertNull(accessDataset.getModel());
        accessDataset.setModel(new LinkedHashModel());
        assertNotNull(accessDataset.getModel());
    }

    @Test
    void asTurtle() throws IOException {
        final AccessDataset accessDataset = new TestAccessDataset();
        accessDataset.setModel(testModel);
        final String turtle = accessDataset.asTurtle();
        final Model model = TestUtils.loadTurtleFromString(turtle);
        assertTrue(Models.isomorphic(testModel, model));
    }

    @Test
    void asTurtleNull() {
        final AccessDataset accessDataset = new TestAccessDataset();
        assertEquals("", accessDataset.asTurtle());
    }

    @Test
    void asSparqlInsert() throws IOException {
        final AccessDataset accessDataset = new TestAccessDataset();
        accessDataset.setModel(testModel);
        final String insert = accessDataset.asSparqlInsert();
        final String expected = TestUtils.loadStringFromFile("src/test/resources/utils/insert.rq");
        assertEquals(expected, insert);
    }

    @Test
    void asSparqlInsertNull() {
        final AccessDataset accessDataset = new TestAccessDataset();
        assertEquals("", accessDataset.asSparqlInsert());
    }

    @Test
    void parseTurtle() throws IOException {
        final AccessDataset accessDataset = new TestAccessDataset();
        accessDataset.parseTurtle(TestUtils.loadStringFromFile("src/test/resources/utils/vcard.ttl"),
                "https://example.org");
        assertTrue(Models.isomorphic(testModel, accessDataset.getModel()));
    }

    @Test
    void isSubsetOf() throws IOException {
        final AccessDataset accessDataset = new TestAccessDataset();
        accessDataset.setModel(testModel);
        final AccessDataset accessDataset2 = new TestAccessDataset();
        // empty model
        accessDataset2.setModel(new LinkedHashModel());
        assertFalse(accessDataset.isSubsetOf(accessDataset2));
        // same model
        accessDataset2.setModel(TestUtils.loadTurtleFromFile("src/test/resources/utils/vcard.ttl"));
        assertTrue(accessDataset.isSubsetOf(accessDataset2));
        assertTrue(accessDataset2.isSubsetOf(accessDataset));
        // larger model
        accessDataset2.getModel().add(iri("https://example.org/agent"), RDF.type, VCARD.Agent);
        assertTrue(accessDataset.isSubsetOf(accessDataset2));
        assertFalse(accessDataset2.isSubsetOf(accessDataset));
    }

    @Test
    void isSubsetOfFalse() throws IOException {
        final AccessDataset accessDataset = new TestAccessDataset();
        accessDataset.setModel(testModel);
        final AccessDataset accessDataset2 = new TestAccessDataset();
        accessDataset2.setModel(TestUtils.loadTurtleFromFile("src/test/resources/utils/wac.ttl"));
        assertFalse(accessDataset.isSubsetOf(accessDataset2));
        assertFalse(accessDataset2.isSubsetOf(accessDataset));
    }

    @Test
    void isSubsetOfNull() {
        final AccessDataset accessDataset = new TestAccessDataset();
        assertFalse(accessDataset.isSubsetOf(null));
        accessDataset.setModel(testModel);
        assertFalse(accessDataset.isSubsetOf(null));
        assertFalse(accessDataset.isSubsetOf(new TestAccessDataset()));
    }

    class TestAccessDataset implements AccessDataset {
        Model model;

        @Override
        public String getMode() {
            return "TEST";
        }

        @Override
        public Model getModel() {
            return model;
        }

        @Override
        public void setModel(final Model model) {
            this.model = model;
        }

        @Override
        public boolean apply(final Client client, final URI uri) {
            return false;
        }
    }
}