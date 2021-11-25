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

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.ParseErrorLogger;
import org.eclipse.rdf4j.rio.helpers.RDFaParserSettings;
import org.eclipse.rdf4j.rio.helpers.RDFaVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.rdf4j.model.util.Values.iri;

public final class RDFUtils {
    private static final Logger logger = LoggerFactory.getLogger(RDFUtils.class);

    public static List<String> turtleToTripleArray(final String data, final String baseUri) throws Exception {
        final Model model = Rio.parse(new StringReader(data), baseUri, RDFFormat.TURTLE);
        final StringWriter sw = new StringWriter();
        Rio.write(model, sw, RDFFormat.NTRIPLES);
        return Arrays.asList(sw.toString().split("\n"));
    }

    public static List<String> jsonLdToTripleArray(final String data, final String baseUri) throws Exception {
        final Model model = Rio.parse(new StringReader(data), baseUri, RDFFormat.JSONLD);
        final StringWriter sw = new StringWriter();
        Rio.write(model, sw, RDFFormat.NTRIPLES);
        return Arrays.asList(sw.toString().split("\n"));
    }

    public static List<String> rdfaToTripleArray(final String data, final String baseUri) throws Exception {
        final Model model = parseRdfa(data, baseUri);
        final StringWriter sw = new StringWriter();
        Rio.write(model, sw, RDFFormat.NTRIPLES);
        return Arrays.asList(sw.toString().split("\n"));
    }

    private static Model parseRdfa(final String data, final String baseUri) throws IOException {
        final ParserConfig parserConfig = new ParserConfig();
        parserConfig.set(RDFaParserSettings.RDFA_COMPATIBILITY, RDFaVersion.RDFA_1_1);
        return Rio.parse(new StringReader(data), baseUri, RDFFormat.RDFA,
                parserConfig, SimpleValueFactory.getInstance(), new ParseErrorLogger());
    }

    public static List<String> parseContainerContents(final String data, final String url) throws Exception {
        final Model model;
        try {
            model = Rio.parse(new StringReader(data), url, RDFFormat.TURTLE);
        } catch (Exception e) {
            logger.error("RDF Parse Error: {} in {}", e, data);
            throw (Exception) new Exception("Bad container listing").initCause(e);
        }
        final Set<Value> resources = model.filter(iri(url), LDP.CONTAINS, null).objects();
        return resources.stream().map(Object::toString).collect(Collectors.toList());
    }

    private RDFUtils() { }
/*
    public static final Model parse(String data, String contentType, String baseUri) throws IOException {
        RDFFormat dataFormat = null;
        switch (contentType) {
            case HttpConstants.MEDIA_TYPE_TEXT_TURTLE:
                dataFormat = RDFFormat.TURTLE;
                break;
            case "text/n-triples":
                dataFormat = RDFFormat.NTRIPLES;
                break;
            case HttpConstants.MEDIA_TYPE_TEXT_PLAIN:
                dataFormat = RDFFormat.NTRIPLES;
                break;
        }
        return Rio.parse(new StringReader(data), baseUri, dataFormat);
    }

    public static final List<String> triplesToTripleArray(String data, String baseUri) throws IOException {
        Model model = Rio.parse(new StringReader(data), baseUri, RDFFormat.NTRIPLES);
        StringWriter sw = new StringWriter();
        Rio.write(model, sw, RDFFormat.NTRIPLES);
        return Arrays.asList(sw.toString().split("\n"));
    }

    public static final String turtleToJsonLd(String data, String baseUri) throws IOException {
        Model model = Rio.parse(new StringReader(data), baseUri, RDFFormat.TURTLE);
        StringWriter sw = new StringWriter();
        Rio.write(model, sw, RDFFormat.JSONLD);
        return sw.toString();
    }

    public static final Boolean isTurtle(String data, String baseUri) {
        try {
            Model model = Rio.parse(new StringReader(data), baseUri, RDFFormat.TURTLE);
            return model != null && model.size() != 0;
        } catch (IOException | RDFParseException | RDFHandlerException e) {
            logger.debug("Input is not in Turtle format", e);
            return false;
        }
    }

    public static final Boolean isJsonLD(String data, String baseUri) {
        try {
            Model model = Rio.parse(new StringReader(data), baseUri, RDFFormat.JSONLD);
            return model != null && model.size() != 0;
        } catch (IOException | RDFParseException | RDFHandlerException e) {
            logger.debug("Input is not in JSON-LD format", e);
            return false;
        }
    }

    public static final Boolean isNTriples(String data, String baseUri) {
        try {
            Model model = Rio.parse(new StringReader(data), baseUri, RDFFormat.NTRIPLES);
            return model != null && model.size() != 0;
        } catch (IOException | RDFParseException | RDFHandlerException e) {
            logger.debug("Input is not in N-Triples format", e);
            return false;
        }
    }

    public static final List<String> parseContainer(String data, String baseUri) {
        Model model = null;
        try {
            model = Rio.parse(new StringReader(data), baseUri, RDFFormat.TURTLE);
        } catch (Exception e) {
            logger.error("RDF Parse Error: {} in {}", e.toString(), data);
            return null;
        }
        Set<Value> resources = model.filter(null, LDP.CONTAINS, null).objects();
        return resources.stream().map(resource -> resource.toString()).collect(Collectors.toList());
    }
 */
}
