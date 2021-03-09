package org.solid.testharness.reporting;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class TestResultTest {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.reporting.TestResultTest");

    @Test
    public void parseResultWithLocalContext() {
        try {
            String context = getResourceReader("test-result.jsonld").lines().collect(Collectors.joining("\n"));
            String baseUri = "http://solidcommunity.org/testsuite";
            Model model = Rio.parse(new JsonLdContextWrappingReader(getResourceReader("testresult.json"), context), baseUri, RDFFormat.JSONLD);
            StringWriter sw = new StringWriter();
            Rio.write(model, sw, RDFFormat.TURTLE);
            logger.debug(sw.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void parseResultWithRemoteContext() {
        try {
            String baseUri = "http://example.org";
            Model model = Rio.parse(new JsonLdContextWrappingReader(getResourceReader("testresult.json"), "\"http://schema.org/\""), baseUri, RDFFormat.JSONLD);
            StringWriter sw = new StringWriter();
            Rio.write(model, sw, RDFFormat.TURTLE);
            logger.debug(sw.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BufferedReader getResourceReader(String path) {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(path);
        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }
}


