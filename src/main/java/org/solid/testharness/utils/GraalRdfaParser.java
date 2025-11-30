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
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * RDFa parser using GraalJS and rdfa-streaming-parser.
 * This replaces the deprecated semargl-rdf4j parser for RDF4J v5 compatibility.
 *
 * <p>The parser uses a JavaScript bundle (rdfa-parser-bundle.js) that wraps the
 * rdfa-streaming-parser npm package. Quads are streamed back to Java via a callback.</p>
 */
@ApplicationScoped
public class GraalRdfaParser {
    private static final Logger logger = LoggerFactory.getLogger(GraalRdfaParser.class);
    private static final String BUNDLE_RESOURCE = "/rdfa-parser-bundle.js";
    private static final java.lang.String JS = "js";

    private Context context;
    private boolean initialized = false;

    /**
     * Initialize the GraalJS context and load the parser bundle.
     */
    @PostConstruct
    void initialize() {
        try {
            logger.debug("Initializing GraalJS RDFa parser");

            // Create GraalJS context with host access for callbacks
            context = Context.newBuilder(JS)
                    .allowHostAccess(HostAccess.ALL)
                    .allowHostClassLookup(className -> true)
                    .option("engine.WarnInterpreterOnly", "false")
                    .build();

            // Load the bundle from classpath
            final var bundleCode = loadBundle();
            context.eval(JS, bundleCode);

            // Verify the parser loaded correctly
            final var version = context.eval(JS, "RdfaParser.getVersion()");
            logger.info("GraalJS RDFa parser initialized: {}", version.asString());

            initialized = true;
        } catch (Exception e) {
            logger.error("Failed to initialize GraalJS RDFa parser", e);
            throw new IllegalStateException("Failed to initialize RDFa parser", e);
        }
    }

    /**
     * Clean up the GraalJS context.
     */
    @PreDestroy
    void cleanup() {
        if (context != null) {
            logger.debug("Closing GraalJS context");
            context.close();
            context = null;
            initialized = false;
        }
    }

    /**
     * Parse RDFa content and return an RDF4J Model.
     *
     * @param content the HTML/XHTML content containing RDFa
     * @param baseUri the base URI for resolving relative IRIs
     * @param contentType the content type ("text/html" or "application/xhtml+xml")
     * @return the parsed RDF model
     * @throws RdfaParseException if parsing fails
     */
    public Model parse(final String content, final String baseUri, final String contentType) {
        if (!initialized) {
            throw new IllegalStateException("GraalRdfaParser not initialized");
        }

        final Model model = new LinkedHashModel();
        final var handler = new QuadHandler(model);

        synchronized (context) {
            // Bind the handler so JS can call it
            context.getBindings(JS).putMember("quadHandler", handler);

            // Call the parser
            final var result = context.eval(JS, String.format(
                    "RdfaParser.parseRdfa(%s, %s, %s, quadHandler)",
                    escapeJsString(content),
                    escapeJsString(baseUri),
                    escapeJsString(contentType)
            ));

            // Check for errors
            final boolean success = result.getMember("success").asBoolean();
            if (!success) {
                final var error = result.getMember("error");
                final var errorMsg = error.isNull() ? "Unknown parse error" : error.asString();
                throw new RdfaParseException("Failed to parse RDFa: " + errorMsg);
            }
        }

        return model;
    }

    private String loadBundle() throws IOException {
        try (final InputStream is = getClass().getResourceAsStream(BUNDLE_RESOURCE)) {
            if (is == null) {
                throw new IOException("Bundle not found: " + BUNDLE_RESOURCE);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String escapeJsString(final String s) {
        if (s == null) {
            return "null";
        }
        return "'" + s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "'";
    }

    /**
     * Callback handler that receives quads from the JavaScript parser.
     */
    public static class QuadHandler {
        private final Model model;

        public QuadHandler(final Model model) {
            this.model = model;
        }

        @HostAccess.Export
        @SuppressWarnings("unused") // Called from JavaScript
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

        private Resource convertToResource(final Map<String, Object> term) {
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
                        // Handle rdf:langString without language tag - treat as plain literal
                        // This can occur when HTML has lang="" to reset language context
                        if (datatype.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString")) {
                            yield Values.literal(value);
                        } else {
                            yield Values.literal(value, Values.iri(datatype));
                        }
                    } else {
                        yield Values.literal(value);
                    }
                }
                default -> null;
            };
        }
    }

    /**
     * Exception thrown when RDFa parsing fails.
     */
    public static class RdfaParseException extends RuntimeException {
        public RdfaParseException(final String message) {
            super(message);
        }

        public RdfaParseException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
