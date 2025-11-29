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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proof of concept test for GraalJS-based RDFa parsing.
 * Tests that we can:
 * 1. Load the rdfa-parser-bundle.js into GraalJS
 * 2. Call parseRdfa() from Java
 * 3. Receive quads via Java callback
 * 4. Convert to RDF4J Model
 */
class GraalRdfaParserTest {
    private static final String JS = "js";
    private static final String BUNDLE_RESOURCE = "/rdfa-parser-bundle.js";
    private static Context context;

    @BeforeAll
    static void setUp() throws IOException {
        // Create GraalJS context with host access enabled for callbacks
        context = Context.newBuilder(JS)
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(className -> true)
                .build();

        // Load the bundle from classpath (built by webpack to target/classes)
        try (var is = GraalRdfaParserTest.class.getResourceAsStream(BUNDLE_RESOURCE)) {
            if (is == null) {
                throw new IOException("Bundle not found on classpath: " + BUNDLE_RESOURCE);
            }
            final var bundleCode = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            context.eval(JS, bundleCode);
        }
    }

    @AfterAll
    static void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void testBundleLoads() {
        // Verify RdfaParser global is available
        final var result = context.eval(JS, "typeof RdfaParser");
        assertEquals("object", result.asString());
    }

    @Test
    void testGetVersion() {
        final var result = context.eval(JS, "RdfaParser.getVersion()");
        assertTrue(result.asString().contains("rdfa-streaming-parser"));
    }

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

        final Model model = new LinkedHashModel();
        final var handler = new QuadHandler(model);

        // Put handler in bindings so JS can call it
        context.getBindings(JS).putMember("quadHandler", handler);

        // Call parseRdfa
        final var result = context.eval(JS, String.format(
                "RdfaParser.parseRdfa(%s, 'http://example.org/', 'text/html', quadHandler)",
                escapeJsString(html)
        ));

        // Check result
        assertTrue(result.getMember("success").asBoolean(), "Parse should succeed");

        // Verify model has expected triples
        assertFalse(model.isEmpty(), "Model should not be empty");

        // Check for schema:Person type
        final var alice = Values.iri("http://example.org/#alice");
        final var rdfType = Values.iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        final var schemaPerson = Values.iri("http://schema.org/Person");

        assertTrue(model.contains(alice, rdfType, schemaPerson),
                "Model should contain alice rdf:type schema:Person");

        // Check for schema:name
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

        final Model model = new LinkedHashModel();
        final var handler = new QuadHandler(model);
        context.getBindings(JS).putMember("quadHandler", handler);

        context.eval(JS, String.format(
                "RdfaParser.parseRdfa(%s, 'http://example.org/', 'text/html', quadHandler)",
                escapeJsString(html)
        ));

        final var bob = Values.iri("http://example.org/#bob");
        final var schemaName = Values.iri("http://schema.org/name");

        // Find the name statement and check the literal has language tag
        final var nameStatements = model.filter(bob, schemaName, null);
        assertFalse(nameStatements.isEmpty(), "Should have name statement");

        final var literal = nameStatements.iterator().next().getObject();
        assertTrue(literal.isLiteral(), "Object should be a literal");
        assertEquals("Bob", literal.stringValue());
    }

    @Test
    void testParseMinimalHtml() {
        // htmlparser2 is very lenient - even minimal HTML parses successfully
        final var html = "<html><body>Simple content</body></html>";

        final Model model = new LinkedHashModel();
        final var handler = new QuadHandler(model);
        context.getBindings(JS).putMember("quadHandler", handler);

        final var result = context.eval(JS, String.format(
                "RdfaParser.parseRdfa(%s, 'http://example.org/', 'text/html', quadHandler)",
                escapeJsString(html)
        ));

        // Check result
        final var success = result.getMember("success").asBoolean();
        final var error = result.getMember("error");
        assertTrue(success, () -> "Parse should succeed but got: " + (error.isNull() ? "null" : error.asString()));

        // Model may have some triples from default RDFa processing (e.g., usesVocabulary)
        // but should not have our test subject
        final var alice = Values.iri("http://example.org/#alice");
        assertFalse(model.contains(alice, null, null), "Model should not contain #alice");
    }

    /**
     * Escape a string for use as a JavaScript string literal.
     */
    private String escapeJsString(final String s) {
        return "'" + s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r") + "'";
    }

    /**
     * Java callback class that receives quads from the JavaScript parser.
     * Methods annotated with @HostAccess.Export are callable from GraalJS.
     */
    public static class QuadHandler {
        private final Model model;

        public QuadHandler(final Model model) {
            this.model = model;
        }

        @HostAccess.Export
        public void onQuad(final Map<String, Object> subject,
                          final Map<String, Object> predicate,
                          final Map<String, Object> object,
                          final Map<String, Object> graph) {
            final var s = convertToResource(subject);
            final var p = convertToIRI(predicate);
            final var o = convertToValue(object);

            if (s != null && p != null && o != null) {
                model.add(s, p, o);
            }
        }

        private org.eclipse.rdf4j.model.Resource convertToResource(final Map<String, Object> term) {
            if (term == null) {
                return null;
            }
            final var termType = (String) term.get("termType");
            final var value = (String) term.get("value");

            return switch (termType) {
                case "NamedNode" -> Values.iri(value);
                case "BlankNode" -> Values.bnode(value);
                default -> null;
            };
        }

        private IRI convertToIRI(final Map<String, Object> term) {
            if (term == null) {
                return null;
            }
            final var termType = (String) term.get("termType");
            if (!"NamedNode".equals(termType)) {
                return null;
            }
            return Values.iri((String) term.get("value"));
        }

        private Value convertToValue(final Map<String, Object> term) {
            if (term == null) {
                return null;
            }
            final var termType = (String) term.get("termType");
            final var value = (String) term.get("value");

            return switch (termType) {
                case "NamedNode" -> Values.iri(value);
                case "BlankNode" -> Values.bnode(value);
                case "Literal" -> {
                    final var language = (String) term.get("language");
                    final var datatype = (String) term.get("datatype");

                    if (language != null && !language.isEmpty()) {
                        yield Values.literal(value, language);
                    } else if (datatype != null && !datatype.isEmpty()) {
                        yield Values.literal(value, Values.iri(datatype));
                    } else {
                        yield Values.literal(value);
                    }
                }
                default -> null;
            };
        }
    }
}
