package org.solid.testharness.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class ResultProcessorTest {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.reporting.ResultProcessorTest");

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void processResult() throws Exception {
        String outputDir = "/Users/pete/work/solid/conformance-test-harness/build/karate-reports/";
        ResultProcessor resultProcessor = new ResultProcessor(new File(outputDir));
        resultProcessor.processResults();
        List<TestJsonResult> results = resultProcessor.getResults();
        logger.debug("Result {} passed of {}", resultProcessor.countPassedScenarios(), resultProcessor.countScenarios());
        assertNotNull(results);
    }

    @Test
    public void processResultLD() throws Exception {
        String outputDir = "/Users/pete/work/solid/conformance-test-harness/build/karate-reports/";
        ResultProcessor resultProcessor = new ResultProcessor(new File(outputDir));
        resultProcessor.processResultsLD();
        Repository rep =resultProcessor.getRepository();
        try (RepositoryConnection con = rep.getConnection()) {
            con.export(Rio.createWriter(RDFFormat.TURTLE, System.out));
        }
//        logger.debug("Result {} passed of {}", resultProcessor.countPassedScenarios(), resultProcessor.countScenarios());
//        assertNotNull(results);
    }
}

