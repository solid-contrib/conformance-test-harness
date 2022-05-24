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

import com.intuit.karate.core.ScenarioEngine;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;
import org.solid.common.vocab.DCTERMS;
import org.solid.common.vocab.FOAF;
import org.solid.common.vocab.RDF;
import org.solid.testharness.utils.TestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class RDFModelTest {
    private static final String TEST_URL = "https://example.org/";
    private static final String CHLID = "https://example.org/test/";
    private static final String NO_MEMBERS = String.format("<%s> a <%s>.", TEST_URL, LDP.RDF_SOURCE);
    private static final String MEMBERS = String.format("<%s> <%s> <%s>.", TEST_URL, LDP.CONTAINS, CHLID);
    private static final IRI BOB_IRI = iri(TestUtils.SAMPLE_NS, TestUtils.BOB);

    private static final Model SAMPLE_HTML_MODEL;
    private static final Model SAMPLE_MODEL;
    private static final Model SAMPLE_MODEL2;
    private static String SAMPLE_TURTLE;
    private static String SAMPLE_JSONLD;
    private static String SAMPLE_HTML;

    static {
        try {
            SAMPLE_TURTLE = TestUtils.loadStringFromFile("src/test/resources/turtle-sample.ttl");
            SAMPLE_JSONLD = TestUtils.loadStringFromFile("src/test/resources/jsonld-sample.json");
            SAMPLE_HTML = TestUtils.loadStringFromFile("src/test/resources/rdfa-sample.html");
        } catch (IOException e) {
            e.printStackTrace();
        }
        SAMPLE_HTML_MODEL = TestUtils.createModel(iri(TestUtils.SAMPLE_NS, "doc"), DCTERMS.title,
                literal("TITLE"));
        SAMPLE_MODEL = TestUtils.createModel(BOB_IRI, RDF.type, FOAF.Person);
        SAMPLE_MODEL2 = TestUtils.createModel(BOB_IRI, RDF.type, FOAF.Person);
        SAMPLE_MODEL2.addAll(SAMPLE_HTML_MODEL);
    }

    @Test
    void parseMissingType() {
        final Exception exception = assertThrows(Exception.class,
                () -> RDFModel.parse(null, "application/json", null)
        );
        assertTrue(exception.getMessage().contains("TestHarnessApiException: Failed to parse data"));
        assertTrue(exception.getMessage().contains("contentType 'application/json' is not supported"));
    }

    @Test
    void parseBadData() {
        final Exception exception = assertThrows(Exception.class,
                () -> RDFModel.parse("BAD", "text/turtle", null)
        );
        assertTrue(exception.getMessage().contains("TestHarnessApiException: Failed to parse data"));
        assertTrue(exception.getMessage().contains("RDFParseException: Unexpected end of file"));
    }

    @Test
    void parseTurtle() {
        final RDFModel model = RDFModel.parse(SAMPLE_TURTLE, "text/turtle", TestUtils.SAMPLE_BASE);
        assertNotNull(model);
        assertEquals(1, model.subjects(null, null).size());
    }

    @Test
    void parseJsonLd() {
        final RDFModel model = RDFModel.parse(SAMPLE_JSONLD, "application/ld+json",
                TestUtils.SAMPLE_BASE);
        assertNotNull(model);
        assertEquals(1, model.subjects(null, null).size());
    }

    @Test
    void parseRdfa() {
        final RDFModel model = RDFModel.parse(SAMPLE_HTML, "text/html", TestUtils.SAMPLE_BASE);
        assertNotNull(model);
        assertEquals(2, model.subjects(null, null).size());
    }

    @Test
    void containsModelNull() {
        final RDFModel model = RDFModel.parse(SAMPLE_TURTLE, "text/turtle", TestUtils.SAMPLE_BASE);
        final Exception exception = assertThrows(Exception.class,
                () -> model.contains(null)
        );
        assertTrue(exception.getMessage().contains("TestHarnessApiException: Contains test failed"));
        assertTrue(exception.getMessage().contains("IllegalArgumentException: The subset model"));
    }

    @Test
    void containsModelEmpty() {
        final RDFModel model = RDFModel.parse(SAMPLE_TURTLE, "text/turtle", TestUtils.SAMPLE_BASE);
        final Exception exception = assertThrows(Exception.class,
                () -> model.contains(new RDFModel(new ModelBuilder().build()))
        );
        assertTrue(exception.getMessage().contains("TestHarnessApiException: Contains test failed"));
        assertTrue(exception.getMessage().contains("IllegalArgumentException: The subset model"));
    }

    @Test
    void containsModel() {
        final RDFModel model = RDFModel.parse(SAMPLE_TURTLE, "text/turtle", TestUtils.SAMPLE_BASE);
        assertTrue(model.contains(new RDFModel(SAMPLE_MODEL)));
    }

    @Test
    void containsModelSizeDiff() {
        ScenarioEngine.set(null);
        final RDFModel model = RDFModel.parse(SAMPLE_TURTLE, "text/turtle", TestUtils.SAMPLE_BASE);
        assertFalse(model.contains(new RDFModel(SAMPLE_MODEL2)));
    }

    @Test
    void containsModelContentDiff() {
        ScenarioEngine.set(TestUtils.createEmptyScenarioEngine());
        final RDFModel model = RDFModel.parse(SAMPLE_TURTLE, "text/turtle", TestUtils.SAMPLE_BASE);
        assertFalse(model.contains(new RDFModel(SAMPLE_HTML_MODEL)));
    }


    @Test
    void getMembers() {
        final List<String> members = RDFModel.parse(MEMBERS, "text/turtle", TEST_URL).getMembers();
        assertFalse(members.isEmpty());
        assertEquals(CHLID, members.get(0));
    }

    @Test
    void getMembersEmpty() {
        final List<String> members = RDFModel.parse(NO_MEMBERS, "text/turtle", TEST_URL).getMembers();
        assertTrue(members.isEmpty());
    }

    @Test
    void subjects() {
        final RDFModel model = new RDFModel(SAMPLE_MODEL);
        final List<String> subjects = model.subjects(RDF.type, FOAF.Person);
        assertFalse(subjects.isEmpty());
        assertEquals(BOB_IRI.stringValue(), subjects.get(0));
    }

    @Test
    void subjectsEmpty() {
        final RDFModel model = new RDFModel(SAMPLE_MODEL);
        final List<String> subjects = model.subjects(RDF.type, FOAF.Agent);
        assertTrue(subjects.isEmpty());
    }

    @Test
    void subjectsFails() {
        final RDFModel model = new RDFModel(null);
        final Exception exception = assertThrows(Exception.class,
                () -> model.subjects(RDF.type, FOAF.Person)
        );
        assertTrue(exception.getMessage().contains("TestHarnessApiException: Failed to get list of subjects"));
        assertTrue(exception.getMessage().contains("NullPointerException"));
    }

    @Test
    void predicates() {
        final RDFModel model = new RDFModel(SAMPLE_MODEL);
        final List<String> predicates = model.predicates(BOB_IRI, FOAF.Person);
        assertFalse(predicates.isEmpty());
        assertEquals(RDF.type.stringValue(), predicates.get(0));
    }

    @Test
    void predicatesEmpty() {
        final RDFModel model = new RDFModel(SAMPLE_MODEL);
        final List<String> predicates = model.predicates(BOB_IRI, FOAF.Agent);
        assertTrue(predicates.isEmpty());
    }

    @Test
    void predicatesFails() {
        final RDFModel model = new RDFModel(null);
        final Exception exception = assertThrows(Exception.class,
                () -> model.predicates(BOB_IRI, FOAF.Person)
        );
        assertTrue(exception.getMessage().contains("TestHarnessApiException: Failed to get list of predicates"));
        assertTrue(exception.getMessage().contains("NullPointerException"));
    }

    @Test
    void objects() {
        final RDFModel model = new RDFModel(SAMPLE_MODEL);
        final List<String> objects = model.objects(BOB_IRI, RDF.type);
        assertFalse(objects.isEmpty());
        assertEquals(FOAF.Person.stringValue(), objects.get(0));
    }

    @Test
    void objectsEmpty() {
        final RDFModel model = new RDFModel(SAMPLE_MODEL);
        final List<String> objects = model.objects(BOB_IRI, RDF.value);
        assertTrue(objects.isEmpty());
    }

    @Test
    void objectsFails() {
        final RDFModel model = new RDFModel(null);
        final Exception exception = assertThrows(Exception.class,
                () -> model.objects(BOB_IRI, RDF.type)
        );
        assertTrue(exception.getMessage().contains("TestHarnessApiException: Failed to get list of objects"));
        assertTrue(exception.getMessage().contains("NullPointerException"));
    }

    @Test
    void contains() {
        final RDFModel model = new RDFModel(SAMPLE_MODEL);
        assertTrue(model.contains(BOB_IRI, RDF.type, FOAF.Person));
    }

    @Test
    void containsFalse() {
        final RDFModel model = new RDFModel(SAMPLE_MODEL);
        assertFalse(model.contains(BOB_IRI, RDF.type, FOAF.Agent));
    }

    @Test
    void containsFails() {
        final RDFModel model = new RDFModel(null);
        final Exception exception = assertThrows(Exception.class,
                () -> model.contains(BOB_IRI, RDF.type, FOAF.Person)
        );
        assertTrue(exception.getMessage()
                .contains("TestHarnessApiException: Failed testing if model contains the statement"));
        assertTrue(exception.getMessage().contains("NullPointerException"));
    }

    @Test
    void iri1() {
        assertEquals(BOB_IRI, RDFModel.iri(BOB_IRI.stringValue()));
    }

    @Test
    void iri1Fails() {
        final Exception exception = assertThrows(Exception.class,
                () -> RDFModel.iri(null)
        );
        assertTrue(exception.getMessage()
                .contains("TestHarnessApiException: Failed to create an IRI"));
        assertTrue(exception.getMessage().contains("NullPointerException"));
    }

    @Test
    void iri2() {
        assertEquals(BOB_IRI, RDFModel.iri(TestUtils.SAMPLE_NS, TestUtils.BOB));
    }

    @Test
    void iri2Fails() {
        final Exception exception = assertThrows(Exception.class,
                () -> RDFModel.iri(null, null)
        );
        assertTrue(exception.getMessage()
                .contains("TestHarnessApiException: Failed to create an IRI"));
        assertTrue(exception.getMessage().contains("NullPointerException"));
    }

    @Test
    void literalObject() {
        final Literal literal = RDFModel.literal(5);
        assertEquals("5", literal.stringValue());
        assertEquals(XSD.INT, literal.getDatatype());
    }

    @Test
    void literalObjectFails() {
        final Exception exception = assertThrows(Exception.class,
                () -> RDFModel.literal(SAMPLE_MODEL)
        );
        assertTrue(exception.getMessage().contains("TestHarnessApiException: Failed to create a literal"));
        assertTrue(exception.getMessage().contains("IllegalArgumentException"));
    }

    @Test
    void literalBigDecimal() {
        final Literal literal = RDFModel.literal(BigDecimal.ONE);
        assertEquals("1", literal.stringValue());
        assertEquals(XSD.DECIMAL, literal.getDatatype());
    }

    @Test
    void literalBigDecimalFails() {
        final Exception exception = assertThrows(Exception.class,
                () -> RDFModel.literal((BigDecimal) null)
        );
        assertTrue(exception.getMessage().contains("TestHarnessApiException: Failed to create a decimal literal"));
        assertTrue(exception.getMessage().contains("NullPointerException"));
    }

    @Test
    void literalBigInteger() {
        final Literal literal = RDFModel.literal(BigInteger.TEN);
        assertEquals("10", literal.stringValue());
        assertEquals(XSD.INTEGER, literal.getDatatype());
    }

    @Test
    void literalBigIntegerFails() {
        final Exception exception = assertThrows(Exception.class,
                () -> RDFModel.literal((BigInteger) null)
        );
        assertTrue(exception.getMessage().contains("TestHarnessApiException: Failed to create an integer literal"));
        assertTrue(exception.getMessage().contains("NullPointerException"));
    }

    @Test
    void literalDataType() {
        final Literal literal = RDFModel.literal("true", XSD.BOOLEAN);
        assertEquals("true", literal.stringValue());
        assertEquals(XSD.BOOLEAN, literal.getDatatype());
    }

    @Test
    void literalDataTypeFails() {
        final Exception exception = assertThrows(Exception.class,
                () -> RDFModel.literal(null, XSD.BOOLEAN)
        );
        assertTrue(exception.getMessage().contains("TestHarnessApiException: Failed to create an " +
                "http://www.w3.org/2001/XMLSchema#boolean literal"));
        assertTrue(exception.getMessage().contains("NullPointerException"));
    }

    @Test
    void literalString() {
        final Literal literal = RDFModel.literal("hello", "en");
        assertEquals("hello", literal.stringValue());
        assertEquals("en", literal.getLanguage().orElse(""));
    }

    @Test
    void literalStringFails() {
        final Exception exception = assertThrows(Exception.class,
                () -> RDFModel.literal(null, "en")
        );
        assertTrue(exception.getMessage().contains("TestHarnessApiException: Failed to create a string with encoding"));
        assertTrue(exception.getMessage().contains("NullPointerException"));
    }
}
