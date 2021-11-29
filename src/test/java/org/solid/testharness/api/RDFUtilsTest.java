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
package org.solid.testharness.api;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.utils.TestData;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class RDFUtilsTest {
    private static final Logger logger = LoggerFactory.getLogger(RDFUtilsTest.class);
    private static final String TEST_URL = "https://example.org/";
    private static final String CHLID = "https://example.org/test/";
    private static final String NO_MEMBERS = String.format("<%s> a <%s>.", TEST_URL, LDP.CONTAINER);
    private static final String MEMBERS = String.format("<%s> <%s> <%s>.", TEST_URL, LDP.CONTAINS, CHLID);

    @Test
    void turtleToTripleArray() {
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
    void jsonLdToTripleArray() {
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
    void rdfaToTripleArray() {
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

    @Test
    void parseMembers() {
        final List<String> members = RDFUtils.parseContainerContents(MEMBERS, TEST_URL);
        assertFalse(members.isEmpty());
        assertEquals(CHLID, members.get(0));
    }

    @Test
    void parseMembersEmpty() {
        final List<String> members = RDFUtils.parseContainerContents(NO_MEMBERS, TEST_URL);
        assertTrue(members.isEmpty());
    }

    @Test
    void parseMembersFails() {
        final Exception exception = assertThrows(Exception.class,
                () -> RDFUtils.parseContainerContents("BAD", TEST_URL)
        );
        assertTrue(exception.getMessage().contains("TestHarnessException: Bad container listing"));
        assertTrue(exception.getMessage().contains("RDFParseException: Unexpected end of file"));
    }
}
