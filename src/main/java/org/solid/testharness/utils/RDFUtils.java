package org.solid.testharness.utils;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RDFUtils {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.utils.RDFUtils");

    public static final List<String> turtleToTripleArray(String data, String baseUri) throws Exception {
        Model model = Rio.parse(new StringReader(data), baseUri, RDFFormat.TURTLE);
        StringWriter sw = new StringWriter();
        Rio.write(model, sw, RDFFormat.NTRIPLES);
        return Arrays.asList(sw.toString().split("\n"));
    }

    public static final List<String> jsonLdToTripleArray(String data, String baseUri) throws Exception {
        Model model = Rio.parse(new StringReader(data), baseUri, RDFFormat.JSONLD);
        StringWriter sw = new StringWriter();
        Rio.write(model, sw, RDFFormat.NTRIPLES);
        return Arrays.asList(sw.toString().split("\n"));
    }

/*
    public static final Model parse(String data, String contentType, String baseUri) throws IOException {
        RDFFormat dataFormat = null;
        switch (contentType) {
            case "text/turtle":
                dataFormat = RDFFormat.TURTLE;
                break;
            case "text/n-triples":
                dataFormat = RDFFormat.NTRIPLES;
                break;
            case "text/plain":
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
            logger.error("RDF Parse Error: {} in {}", e.getMessage(), data);
            return null;
        }
        Set<Value> resources = model.filter(null, LDP.CONTAINS, null).objects();
        return resources.stream().map(resource -> resource.toString()).collect(Collectors.toList());
    }
 */
}
