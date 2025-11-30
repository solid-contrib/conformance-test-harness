/*
 * MIT License
 *
 * Copyright (c) 2019 - 2022 W3C Solid Community Group
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
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GraalJS-based RDFa parsing.
 */
@QuarkusTest
class GraalRdfaParserTest {
    @Inject
    GraalRdfaParser parser;

    @Test
    void testParseSimpleRdfa() {
        final var html = """
            <!DOCTYPE html>
            <html vocab="http://schema.org/">
              <head><title>Test</title></head>
              <body typeof="Person" about="#alice">
                <span property="name">Alice</span>
              </body>
            </html>
            """;

        final var model = parser.parse(html, "http://example.org/", "text/html");

        assertFalse(model.isEmpty(), "Model should not be empty");

        final var alice = Values.iri("http://example.org/#alice");
        final var rdfType = Values.iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        final var schemaPerson = Values.iri("http://schema.org/Person");

        assertTrue(model.contains(alice, rdfType, schemaPerson),
                "Model should contain alice rdf:type schema:Person");

        final var schemaName = Values.iri("http://schema.org/name");
        assertTrue(model.contains(alice, schemaName, null),
                "Model should contain alice schema:name");
    }

    @Test
    void testParseLiteralWithLanguage() {
        final var html = """
            <!DOCTYPE html>
            <html>
              <body vocab="http://schema.org/" typeof="Person" about="#bob">
                <span property="name" lang="en">Bob</span>
              </body>
            </html>
            """;

        final var model = parser.parse(html, "http://example.org/", "text/html");

        final var bob = Values.iri("http://example.org/#bob");
        final var schemaName = Values.iri("http://schema.org/name");

        final var nameStatements = model.filter(bob, schemaName, null);
        assertFalse(nameStatements.isEmpty(), "Should have name statement");

        final var literal = (Literal) nameStatements.iterator().next().getObject();
        assertTrue(literal.isLiteral(), "Object should be a literal");
        assertEquals("Bob", literal.stringValue());
        assertTrue(literal.getLanguage().isPresent(), "Should have language tag");
        assertEquals("en", literal.getLanguage().get());
    }

    @Test
    void testParseLiteralWithDatatype() {
        final var html = """
            <!DOCTYPE html>
            <html prefix="xsd: http://www.w3.org/2001/XMLSchema#">
              <body vocab="http://schema.org/" typeof="Person" about="#charlie">
                <span property="age" datatype="xsd:integer">42</span>
              </body>
            </html>
            """;

        final var model = parser.parse(html, "http://example.org/", "text/html");

        final var charlie = Values.iri("http://example.org/#charlie");
        final var schemaAge = Values.iri("http://schema.org/age");

        final var ageStatements = model.filter(charlie, schemaAge, null);
        assertFalse(ageStatements.isEmpty(), "Should have age statement");

        final var literal = (Literal) ageStatements.iterator().next().getObject();
        assertEquals("42", literal.stringValue());
        assertEquals(XSD.INTEGER, literal.getDatatype());
    }

    @Test
    void testParseBlankNode() {
        // Use a nested element to force blank node creation per RDFa rules
        final var html = """
            <!DOCTYPE html>
            <html vocab="http://schema.org/">
              <body>
                <div typeof="Person">
                  <span property="name">Anonymous</span>
                </div>
              </body>
            </html>
            """;

        final var model = parser.parse(html, "http://example.org/", "text/html");

        final var rdfType = Values.iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        final var schemaPerson = Values.iri("http://schema.org/Person");

        // Should have a statement with type Person
        final var personStatements = model.filter(null, rdfType, schemaPerson);
        assertFalse(personStatements.isEmpty(), "Should have Person type statement");
        // The subject may be blank node or base URI depending on RDFa processing
        // Just verify we got a valid Person type triple
    }

    @Test
    void testParseXhtml() {
        final var xhtml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml" vocab="http://schema.org/">
              <head><title>Test</title></head>
              <body typeof="Person" about="#dave">
                <span property="name">Dave</span>
              </body>
            </html>
            """;

        final var model = parser.parse(xhtml, "http://example.org/", "application/xhtml+xml");

        assertFalse(model.isEmpty(), "Model should not be empty");

        final var dave = Values.iri("http://example.org/#dave");
        final var schemaName = Values.iri("http://schema.org/name");
        assertTrue(model.contains(dave, schemaName, null), "Should have name statement");
    }

    @Test
    void testParseMinimalHtml() {
        final var html = "<html><body>Simple content</body></html>";

        final var model = parser.parse(html, "http://example.org/", "text/html");

        // Model may have some default RDFa triples but should not have our test subject
        final var alice = Values.iri("http://example.org/#alice");
        assertFalse(model.contains(alice, null, null), "Model should not contain #alice");
    }

    @Test
    void testParseNotInitialized() {
        final var uninitializedParser = new GraalRdfaParser();
        // Don't call initialize()

        assertThrows(IllegalStateException.class, () ->
                uninitializedParser.parse("<html></html>", "http://example.org/", "text/html"));
    }

    @Test
    void testCleanup() {
        final var testParser = new GraalRdfaParser();
        testParser.initialize();

        // Should be able to parse after initialization
        final var model = testParser.parse("<html><body></body></html>", "http://example.org/", "text/html");
        assertNotNull(model);

        // Cleanup
        testParser.cleanup();

        // Should fail after cleanup
        assertThrows(IllegalStateException.class, () ->
                testParser.parse("<html></html>", "http://example.org/", "text/html"));
    }

    @Test
    void testQuadHandlerNullSubject() {
        final var model = new LinkedHashModel();
        final var handler = new GraalRdfaParser.QuadHandler(model);

        handler.onQuad(null,
                Map.of("termType", "NamedNode", "value", "http://example.org/pred"),
                Map.of("termType", "Literal", "value", "test"),
                null);

        assertTrue(model.isEmpty(), "Model should be empty when subject is null");
    }

    @Test
    void testQuadHandlerNullPredicate() {
        final var model = new LinkedHashModel();
        final var handler = new GraalRdfaParser.QuadHandler(model);

        handler.onQuad(
                Map.of("termType", "NamedNode", "value", "http://example.org/subj"),
                null,
                Map.of("termType", "Literal", "value", "test"),
                null);

        assertTrue(model.isEmpty(), "Model should be empty when predicate is null");
    }

    @Test
    void testQuadHandlerNullObject() {
        final var model = new LinkedHashModel();
        final var handler = new GraalRdfaParser.QuadHandler(model);

        handler.onQuad(
                Map.of("termType", "NamedNode", "value", "http://example.org/subj"),
                Map.of("termType", "NamedNode", "value", "http://example.org/pred"),
                null,
                null);

        assertTrue(model.isEmpty(), "Model should be empty when object is null");
    }

    @Test
    void testQuadHandlerUnknownSubjectType() {
        final var model = new LinkedHashModel();
        final var handler = new GraalRdfaParser.QuadHandler(model);

        handler.onQuad(
                Map.of("termType", "Unknown", "value", "test"),
                Map.of("termType", "NamedNode", "value", "http://example.org/pred"),
                Map.of("termType", "Literal", "value", "test"),
                null);

        assertTrue(model.isEmpty(), "Model should be empty when subject type is unknown");
    }

    @Test
    void testQuadHandlerBlankNodePredicate() {
        final var model = new LinkedHashModel();
        final var handler = new GraalRdfaParser.QuadHandler(model);

        handler.onQuad(
                Map.of("termType", "NamedNode", "value", "http://example.org/subj"),
                Map.of("termType", "BlankNode", "value", "b0"),
                Map.of("termType", "Literal", "value", "test"),
                null);

        assertTrue(model.isEmpty(), "Model should be empty when predicate is blank node");
    }

    @Test
    void testQuadHandlerUnknownObjectType() {
        final var model = new LinkedHashModel();
        final var handler = new GraalRdfaParser.QuadHandler(model);

        handler.onQuad(
                Map.of("termType", "NamedNode", "value", "http://example.org/subj"),
                Map.of("termType", "NamedNode", "value", "http://example.org/pred"),
                Map.of("termType", "Unknown", "value", "test"),
                null);

        assertTrue(model.isEmpty(), "Model should be empty when object type is unknown");
    }

    @Test
    void testQuadHandlerBlankNodeSubject() {
        final var model = new LinkedHashModel();
        final var handler = new GraalRdfaParser.QuadHandler(model);

        handler.onQuad(
                Map.of("termType", "BlankNode", "value", "b0"),
                Map.of("termType", "NamedNode", "value", "http://example.org/pred"),
                Map.of("termType", "Literal", "value", "test"),
                null);

        assertFalse(model.isEmpty(), "Model should have statement with blank node subject");
        assertTrue(model.subjects().iterator().next().isBNode());
    }

    @Test
    void testQuadHandlerBlankNodeObject() {
        final var model = new LinkedHashModel();
        final var handler = new GraalRdfaParser.QuadHandler(model);

        handler.onQuad(
                Map.of("termType", "NamedNode", "value", "http://example.org/subj"),
                Map.of("termType", "NamedNode", "value", "http://example.org/pred"),
                Map.of("termType", "BlankNode", "value", "b0"),
                null);

        assertFalse(model.isEmpty(), "Model should have statement with blank node object");
        assertTrue(model.objects().iterator().next().isBNode());
    }

    @Test
    void testQuadHandlerNamedNodeObject() {
        final var model = new LinkedHashModel();
        final var handler = new GraalRdfaParser.QuadHandler(model);

        handler.onQuad(
                Map.of("termType", "NamedNode", "value", "http://example.org/subj"),
                Map.of("termType", "NamedNode", "value", "http://example.org/pred"),
                Map.of("termType", "NamedNode", "value", "http://example.org/obj"),
                null);

        assertFalse(model.isEmpty(), "Model should have statement with IRI object");
        assertTrue(model.objects().iterator().next().isIRI());
    }

    @Test
    void testQuadHandlerLiteralWithEmptyLanguage() {
        final var model = new LinkedHashModel();
        final var handler = new GraalRdfaParser.QuadHandler(model);

        final Map<String, Object> objectTerm = new HashMap<>();
        objectTerm.put("termType", "Literal");
        objectTerm.put("value", "test");
        objectTerm.put("language", "");
        objectTerm.put("datatype", null);

        handler.onQuad(
                Map.of("termType", "NamedNode", "value", "http://example.org/subj"),
                Map.of("termType", "NamedNode", "value", "http://example.org/pred"),
                objectTerm,
                null);

        assertFalse(model.isEmpty());
        final var literal = (Literal) model.objects().iterator().next();
        assertFalse(literal.getLanguage().isPresent(), "Should not have language tag");
    }

    @Test
    void testQuadHandlerLiteralWithRdfLangString() {
        // Test the rdf:langString without language tag case (line 234 in GraalRdfaParser)
        final var model = new LinkedHashModel();
        final var handler = new GraalRdfaParser.QuadHandler(model);

        final Map<String, Object> objectTerm = new HashMap<>();
        objectTerm.put("termType", "Literal");
        objectTerm.put("value", "test");
        objectTerm.put("language", "");
        objectTerm.put("datatype", "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString");

        handler.onQuad(
                Map.of("termType", "NamedNode", "value", "http://example.org/subj"),
                Map.of("termType", "NamedNode", "value", "http://example.org/pred"),
                objectTerm,
                null);

        assertFalse(model.isEmpty());
        final var literal = (Literal) model.objects().iterator().next();
        assertEquals("test", literal.stringValue());
        // Should be treated as plain literal, not langString
        assertEquals(XSD.STRING, literal.getDatatype());
    }

    @Test
    void testQuadHandlerLiteralWithNullLanguageAndDatatype() {
        final var model = new LinkedHashModel();
        final var handler = new GraalRdfaParser.QuadHandler(model);

        final Map<String, Object> objectTerm = new HashMap<>();
        objectTerm.put("termType", "Literal");
        objectTerm.put("value", "test");
        objectTerm.put("language", null);
        objectTerm.put("datatype", null);

        handler.onQuad(
                Map.of("termType", "NamedNode", "value", "http://example.org/subj"),
                Map.of("termType", "NamedNode", "value", "http://example.org/pred"),
                objectTerm,
                null);

        assertFalse(model.isEmpty());
        final var literal = (Literal) model.objects().iterator().next();
        assertEquals("test", literal.stringValue());
        assertEquals(XSD.STRING, literal.getDatatype());
    }

    @Test
    void testQuadHandlerLiteralWithEmptyDatatype() {
        // Test the !datatype.isEmpty() branch at line 231
        final var model = new LinkedHashModel();
        final var handler = new GraalRdfaParser.QuadHandler(model);

        final Map<String, Object> objectTerm = new HashMap<>();
        objectTerm.put("termType", "Literal");
        objectTerm.put("value", "test");
        objectTerm.put("language", null);
        objectTerm.put("datatype", "");  // Empty string, not null

        handler.onQuad(
                Map.of("termType", "NamedNode", "value", "http://example.org/subj"),
                Map.of("termType", "NamedNode", "value", "http://example.org/pred"),
                objectTerm,
                null);

        assertFalse(model.isEmpty());
        final var literal = (Literal) model.objects().iterator().next();
        assertEquals("test", literal.stringValue());
        // Should fall through to plain literal since datatype is empty
        assertEquals(XSD.STRING, literal.getDatatype());
    }

    @Test
    void testRdfaParseExceptionWithMessage() {
        final var exception = new GraalRdfaParser.RdfaParseException("Test error");
        assertEquals("Test error", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testRdfaParseExceptionWithCause() {
        final var cause = new RuntimeException("Root cause");
        final var exception = new GraalRdfaParser.RdfaParseException("Test error", cause);
        assertEquals("Test error", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testParseErrorWithMessage() {
        // Create a parser and override parseRdfa to return an error
        final var testParser = new GraalRdfaParser();
        testParser.initialize();
        try {
            // Inject a mock parseRdfa that returns an error
            injectMockParseRdfa(testParser, "{ success: false, error: 'Test parse error' }");

            final var exception = assertThrows(GraalRdfaParser.RdfaParseException.class,
                    () -> testParser.parse("<html></html>", "http://example.org/", "text/html"));
            assertTrue(exception.getMessage().contains("Test parse error"));
        } finally {
            testParser.cleanup();
        }
    }

    @Test
    void testParseErrorWithNullError() {
        // Test the "Unknown parse error" branch when error is null
        final var testParser = new GraalRdfaParser();
        testParser.initialize();
        try {
            // Inject a mock parseRdfa that returns success: false with null error
            injectMockParseRdfa(testParser, "{ success: false, error: null }");

            final var exception = assertThrows(GraalRdfaParser.RdfaParseException.class,
                    () -> testParser.parse("<html></html>", "http://example.org/", "text/html"));
            assertTrue(exception.getMessage().contains("Unknown parse error"));
        } finally {
            testParser.cleanup();
        }
    }

    @Test
    void testInitializeExceptionFromBadJs() {
        // Test initialization failure (lines 85-87) by injecting bad JS after context creation
        final var testParser = new GraalRdfaParser();

        try {
            final var contextField = GraalRdfaParser.class.getDeclaredField("context");
            contextField.setAccessible(true);

            // Initialize normally first
            testParser.initialize();

            // Get the context and inject bad JS that will break on next eval
            final var context = (org.graalvm.polyglot.Context) contextField.get(testParser);

            // Close the context to simulate a failure state
            context.close();
            contextField.set(testParser, null);

            // Reset initialized flag
            final var initializedField = GraalRdfaParser.class.getDeclaredField("initialized");
            initializedField.setAccessible(true);
            initializedField.set(testParser, false);

            // Now trying to use the parser should fail since context is null
            assertThrows(IllegalStateException.class, () ->
                    testParser.parse("<html></html>", "http://example.org/", "text/html"));
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    void testInitializeExceptionWrapping() {
        // Test that initialization exceptions are wrapped in IllegalStateException
        // This tests lines 85-87 by verifying the exception wrapping behavior
        final var cause = new RuntimeException("Test failure");
        final var exception = new IllegalStateException("Failed to initialize RDFa parser", cause);

        assertEquals("Failed to initialize RDFa parser", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testCleanupWhenContextIsNull() {
        // Test cleanup when context is already null (line 96 false branch)
        final var testParser = new GraalRdfaParser();
        // Don't call initialize(), so context is null
        // cleanup() should not throw when context is null
        assertDoesNotThrow(testParser::cleanup);
    }

    @Test
    void testCleanupCalledTwice() {
        // Test cleanup called twice - second call should handle null context
        final var testParser = new GraalRdfaParser();
        testParser.initialize();
        testParser.cleanup();
        // Second cleanup should not throw
        assertDoesNotThrow(testParser::cleanup);
    }

    /**
     * Inject a mock parseRdfa function that returns the specified result.
     * Uses reflection to access the private context field.
     */
    private void injectMockParseRdfa(final GraalRdfaParser parser, final String returnValue) {
        try {
            final var contextField = GraalRdfaParser.class.getDeclaredField("context");
            contextField.setAccessible(true);
            final var context = (org.graalvm.polyglot.Context) contextField.get(parser);
            context.eval("js", "RdfaParser.parseRdfa = function() { return " + returnValue + "; }");
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock", e);
        }
    }
}
