/*
 * MIT License
 *
 * Copyright (c) 2021 Solid
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
    private static final Logger logger = LoggerFactory.getLogger(RDFUtilsTest.class);

    @Test
    void turtleToTripleArray() throws Exception {
        final List<String> triples = RDFUtils.turtleToTripleArray(TestData.SAMPLE_TURTLE, TestData.SAMPLE_BASE);
        assertNotNull(triples);
        assertEquals(1, triples.size());
        assertEquals(TestData.SAMPLE_TRIPLE, triples.get(0));
    }

    @Test
    void turtleToTripleArrayFails() {
        assertThrows(Exception.class, () -> RDFUtils.turtleToTripleArray("Not Turtle", TestData.SAMPLE_BASE));
    }

    @Test
    void jsonLdToTripleArray() throws Exception {
        final List<String> triples = RDFUtils.jsonLdToTripleArray(TestData.SAMPLE_JSONLD, TestData.SAMPLE_BASE);
        assertNotNull(triples);
        assertEquals(1, triples.size());
        assertEquals(TestData.SAMPLE_TRIPLE, triples.get(0));
    }

    @Test
    void jsonLdToTripleArrayFails() {
        assertThrows(Exception.class, () -> RDFUtils.jsonLdToTripleArray("Not JSON-LD", TestData.SAMPLE_BASE));
    }

    @Test
    void rdfaToTripleArray() throws Exception {
        final List<String> triples = RDFUtils.rdfaToTripleArray(TestData.SAMPLE_HTML, TestData.SAMPLE_BASE);
        logger.error(Arrays.toString(triples.toArray()));
        assertNotNull(triples);
        assertEquals(1, triples.size());
        assertEquals(TestData.SAMPLE_HTML_TRIPLE, triples.get(0));
    }

    @Test
    void rdfaToTripleArrayFails() {
        assertThrows(Exception.class, () -> RDFUtils.rdfaToTripleArray("Not RDFa", TestData.SAMPLE_BASE));
    }
}
