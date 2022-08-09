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
package org.solid.testharness.api;

import com.intuit.karate.Logger;
import com.intuit.karate.core.ScenarioEngine;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.repository.util.RepositoryUtil;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.ParseErrorLogger;
import org.eclipse.rdf4j.rio.helpers.RDFaParserSettings;
import org.eclipse.rdf4j.rio.helpers.RDFaVersion;
import org.solid.testharness.http.HttpConstants;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility methods for testing RDF responses.
 */
public final class RDFModel {
    private final Model model;
    private final Logger logger;
    private static final Map<String, RDFFormat> CONTENT_TYPES = Map.of(
            HttpConstants.MEDIA_TYPE_TEXT_TURTLE, RDFFormat.TURTLE,
            HttpConstants.MEDIA_TYPE_APPLICATION_JSON_LD, RDFFormat.JSONLD,
            HttpConstants.MEDIA_TYPE_TEXT_HTML, RDFFormat.RDFA,
            HttpConstants.MEDIA_TYPE_APPLICATION_XHTML_XML, RDFFormat.RDFA
    );

    RDFModel(final Model model) {
        this.model = model;
        logger = Optional.ofNullable(ScenarioEngine.get()) .map(se -> se.logger).orElse(null);
    }

    /**
     * Returns a <code>RDFModel</code> instance containing the model for this input.
     * @param data the RDF data
     * @param contentType the content type for this data
     * @param baseUri the base URI for this data
     * @return the model
     */
    public static RDFModel parse(final String data, final String contentType,  final String baseUri) {
        try {
            if (!CONTENT_TYPES.containsKey(contentType)) {
                throw new IllegalArgumentException("contentType '" + contentType + "' is not supported");
            }
            final RDFFormat format = CONTENT_TYPES.get(contentType);
            if (format.equals(RDFFormat.RDFA)) {
                final ParserConfig parserConfig = new ParserConfig();
                parserConfig.set(RDFaParserSettings.RDFA_COMPATIBILITY, RDFaVersion.RDFA_1_1);
                return new RDFModel(Rio.parse(new StringReader(data), baseUri, RDFFormat.RDFA,
                        parserConfig, SimpleValueFactory.getInstance(), new ParseErrorLogger()));
            } else {
                return new RDFModel(Rio.parse(new StringReader(data), baseUri, format));
            }
        } catch (Exception e) {
            throw new TestHarnessApiException("Failed to parse data", e);
        }
    }

    /**
     * Returns true if the subset model passed in is a subset of the model in this instance. If it is not a subset
     * the method logs information highlighting the differences.
     * @param subset the model to compare to this one
     * @return <code>true</code> if it is a subset
     */
    public boolean contains(final RDFModel subset) {
        try {
            if (subset == null || subset.model.isEmpty()) {
                throw new IllegalArgumentException("The subset model must exist and have at least one statement");
            }
            if (Models.isSubset(subset.model, model)) {
                return true;
            } else {
                final StringBuilder sb = new StringBuilder();
                final int sizeDiff = subset.model.size() - model.size();
                if (sizeDiff > 0) {
                    sb.append("This model has ").append(sizeDiff).append(" fewer statements than the other.\n");
                }
                sb.append("The statements missing from this model are:\n");
                final StringWriter sw = new StringWriter();
                final RDFWriter writer = Rio.createWriter(RDFFormat.NTRIPLES, sw);
                writer.startRDF();
                RepositoryUtil.difference(subset.model, model).forEach(writer::handleStatement);
                writer.endRDF();
                sb.append(sw);
                log(sb.toString());
                return false;
            }
        } catch (Exception e) {
            throw new TestHarnessApiException("Contains test failed", e);
        }
    }

    /**
     * Returns a list of URIs representing the members of a container.
     * @return the list of member urls
     */
    public List<String> getMembers() {
        return objects(null, LDP.CONTAINS);
    }

    /**
     * Returns a list of subjects matching the predicate and object.
     * @param predicate the iri of the predicate or null
     * @param object the object value (iri/literal) or null
     * @return the list of subjects as strings
     */
    public List<String> subjects(final IRI predicate, final Value object) {
        try {
            final Set<Resource> subjects = model.filter(null, predicate, object).subjects();
            return subjects.stream().map(Resource::stringValue).collect(Collectors.toList());
        } catch (Exception e) {
            throw new TestHarnessApiException("Failed to get list of subjects", e);
        }
    }

    /**
     * Returns a list of predicates matching the subject and object.
     * @param subject the iri of the subject or null
     * @param object the object value (iri/literal) or null
     * @return the list of predicates as strings
     */
    public List<String> predicates(final IRI subject, final Value object) {
        try {
            final Set<IRI> predicates = model.filter(subject, null, object).predicates();
            return predicates.stream().map(IRI::stringValue).collect(Collectors.toList());
        } catch (Exception e) {
            throw new TestHarnessApiException("Failed to get list of predicates", e);
        }
    }

    /**
     * Returns a list of objects matching the subject and predicate.
     * @param subject the iri of the subject
     * @param predicate the iri of the predicate
     * @return the list of objects as strings
     */
    public List<String> objects(final IRI subject, final IRI predicate) {
        try {
            final Set<Value> objects = model.filter(subject, predicate, null).objects();
            return objects.stream().map(Value::stringValue).collect(Collectors.toList());
        } catch (Exception e) {
            throw new TestHarnessApiException("Failed to get list of objects", e);
        }
    }

    /**
     * Returns true if the statement is found in the model where the object value is an IRI.
     * @param subject the iri of the subject
     * @param predicate the iri of the predicate
     * @param object the iri of the object
     * @return <code>true</code> if the statement is found
     */
    public boolean contains(final IRI subject, final IRI predicate, final Value object) {
        try {
            return model.contains(subject, predicate, object);
        } catch (Exception e) {
            throw new TestHarnessApiException("Failed testing if model contains the statement", e);
        }
    }

    /**
     * Convert the model to triples.
     * @return the triples
     */
    public String asTriples() {
        final StringWriter sw = new StringWriter();
        Rio.write(model, sw, RDFFormat.NTRIPLES);
        return sw.toString();
    }

    /**
     * Returns an <code>IRI</code> instance.
     * @param iri the iri as a string
     * @return the IRI
     */
    public static IRI iri(final String iri) {
        try {
            return Values.iri(iri);
        } catch (Exception e) {
            throw new TestHarnessApiException("Failed to create an IRI", e);
        }
    }

    /**
     * Returns an <code>IRI</code> instance.
     * @param namespace the namespace as a string
     * @param localName the localName as a string
     * @return the IRI
     */
    public static IRI iri(final String namespace, final String localName) {
        try {
            return Values.iri(namespace, localName);
        } catch (Exception e) {
            throw new TestHarnessApiException("Failed to create an IRI", e);
        }
    }

    /**
     * Returns a <code>Literal</code> instance encoding a literal of any type in the list:
     * Boolean, Byte, Double, Float, Integer, Long, Short, XMLGregorianCalendar, TemporalAccessor and Date.
     * @param value the value to convert to a Literal
     * @return the Literal
     */
    public static Literal literal(final Object value) {
        try {
            return Values.literal(value, true);
        } catch (Exception e) {
            throw new TestHarnessApiException("Failed to create a literal", e);
        }
    }

    /**
     * Returns a <code>Literal</code> instance encoding a literal from a BigDecimal.
     * @param value the value to convert to a Literal
     * @return the Literal
     */
    public static Literal literal(final BigDecimal value) {
        try {
            return Values.literal(value);
        } catch (Exception e) {
            throw new TestHarnessApiException("Failed to create a decimal literal", e);
        }
    }

    /**
     * Returns a <code>Literal</code> instance encoding a literal from a BigInteger.
     * @param value the value to convert to a Literal
     * @return the Literal
     */
    public static Literal literal(final BigInteger value) {
        try {
            return Values.literal(value);
        } catch (Exception e) {
            throw new TestHarnessApiException("Failed to create an integer literal", e);
        }
    }

    /**
     * Returns a <code>Literal</code> instance encoding a literal of the given type.
     * @param value the value to convert to a Literal
     * @param datatype the IRI of the XSD datatype
     * @return the Literal
     */
    public static Literal literal(final String value, final IRI datatype) {
        try {
            return Values.literal(value, datatype);
        } catch (Exception e) {
            throw new TestHarnessApiException("Failed to create an " + datatype + " literal", e);
        }
    }
    /**
     * Returns a <code>Literal</code> instance encoding a string literal with an optional language tag.
     * @param value the string value to convert to a Literal
     * @param language the language code to associate with the Literal
     * @return the Literal
     */
    public static Literal literal(final String value, final String language) {
        try {
            return Values.literal(value, language);
        } catch (Exception e) {
            throw new TestHarnessApiException("Failed to create a string with encoding", e);
        }
    }

    private void log(final String msg) {
        if (logger != null) {
            logger.debug(msg);
        }
    }
}
