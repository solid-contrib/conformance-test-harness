package org.solid.testharness.utils;

import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RDFUtilsTest {
    @Test
    void turtleToTripleArray() throws Exception {
        List<String> triples = RDFUtils.turtleToTripleArray("</bob> a <http://xmlns.com/foaf/0.1/Person> .", "http://example.org");
        assertNotNull(triples);
        assertEquals(1, triples.size());
        assertEquals("<http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .", triples.get(0));
    }

    @Test
    void turtleToTripleArrayFails() {
        assertThrows(Exception.class, () -> RDFUtils.turtleToTripleArray("Not Turtle", "http://example.org"));
    }

    @Test
    void jsonLdToTripleArray() throws Exception {
        List<String> triples = RDFUtils.jsonLdToTripleArray("{\"@id\": \"http://example.org/bob\", \"@type\": \"http://xmlns.com/foaf/0.1/Person\"}", "http://example.org");
        assertNotNull(triples);
        assertEquals(1, triples.size());
        assertEquals("<http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .", triples.get(0));
    }

    @Test
    void jsonLdToTripleArrayFails() {
        assertThrows(Exception.class, () -> RDFUtils.jsonLdToTripleArray("Not JSON-LD", "http://example.org"));
    }

    @Test
    void meaninglessContruct() {
        assertNotNull(new RDFUtils());
    }
}
