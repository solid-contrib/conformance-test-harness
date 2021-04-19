package org.solid.testharness.utils;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class RDFUtilsTest {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.utils.RDFUtilsTest");

    @Test
    void turtleToTripleArray() throws Exception {
        List<String> triples = RDFUtils.turtleToTripleArray(TestData.SAMPLE_TURTLE, TestData.SAMPLE_BASE);
        assertNotNull(triples);
        assertEquals(1, triples.size());
        assertEquals(TestData.SAMPLE_TURTLE_TRIPLE, triples.get(0));
    }

    @Test
    void turtleToTripleArrayFails() {
        assertThrows(Exception.class, () -> RDFUtils.turtleToTripleArray("Not Turtle", TestData.SAMPLE_BASE));
    }

    @Test
    void jsonLdToTripleArray() throws Exception {
        List<String> triples = RDFUtils.jsonLdToTripleArray(TestData.SAMPLE_JSONLD, TestData.SAMPLE_BASE);
        assertNotNull(triples);
        assertEquals(1, triples.size());
        assertEquals(TestData.SAMPLE_JSONLD_TRIPLE, triples.get(0));
    }

    @Test
    void jsonLdToTripleArrayFails() {
        assertThrows(Exception.class, () -> RDFUtils.jsonLdToTripleArray("Not JSON-LD", TestData.SAMPLE_BASE));
    }

    @Test
    void rdfaToTripleArray() throws Exception {
        List<String> triples = RDFUtils.rdfaToTripleArray(TestData.SAMPLE_HTML, TestData.SAMPLE_BASE);
        logger.error(Arrays.toString(triples.toArray()));
        assertNotNull(triples);
        assertEquals(1, triples.size());
        assertEquals(TestData.SAMPLE_HTML_TRIPLE, triples.get(0));
    }

    @Test
    void rdfaToTripleArrayFails() {
        assertThrows(Exception.class, () -> RDFUtils.rdfaToTripleArray("Not RDFa", TestData.SAMPLE_BASE));
    }

    @Test
    void meaninglessContruct() {
        assertNotNull(new RDFUtils());
    }
}
