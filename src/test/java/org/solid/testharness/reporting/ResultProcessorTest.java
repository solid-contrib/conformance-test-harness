package org.solid.testharness.reporting;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;


class ResultProcessorTest {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.reporting.ResultProcessorTest");

    @Test
    @Disabled
    public void processResult() {
        String outputDir = "/Users/pete/work/solid/conformance-test-harness/build/karate-reports/";
        ResultProcessor resultProcessor = new ResultProcessor(new File(outputDir));
        resultProcessor.processResults();
        List<TestJsonResult> results = resultProcessor.getResults();
        logger.debug("Result {} passed of {}", resultProcessor.countPassedScenarios(), resultProcessor.countScenarios());
        assertNotNull(results);
    }

    @Test
    @Disabled
    public void processResultLD() throws Exception {
        String outputDir = "/Users/pete/work/solid/conformance-test-harness/build/karate-reports/";
        ResultProcessor resultProcessor = new ResultProcessor(new File(outputDir));
        resultProcessor.processResultsLD();
        Repository rep = resultProcessor.getRepository();
        String outputFile = "/Users/pete/work/solid/conformance-test-harness/src/test/resources/testsuite-results-earl.ttl";
        FileWriter wr = new FileWriter(outputFile);

        try (RepositoryConnection conn = rep.getConnection()) {
            conn.export(Rio.createWriter(RDFFormat.TURTLE, wr));
            conn.export(Rio.createWriter(RDFFormat.TURTLE, System.out));
        }
        wr.close();
        logger.debug("Features {}", resultProcessor.countFeatures());
//        logger.debug("Result {} passed of {}", resultProcessor.countPassedScenarios(), resultProcessor.countScenarios());
//        assertNotNull(results);
    }

    @Test
    @Disabled
    public void transformOutput() {
        Repository repository = loadTestData("testsuite-results-json.ttl");
        try (RepositoryConnection conn = repository.getConnection()) {
            String queryString = "SELECT ?feature WHERE { ?feature <http://solidcommunity.org/testsuite#keyword> \"Feature\" } ";
            TupleQuery tupleQuery = conn.prepareTupleQuery(queryString);
            int count = 0;
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                while (result.hasNext()) {  // iterate over the result
                    BindingSet bindingSet = result.next();
                    Value feature = bindingSet.getValue("feature");
                    logger.debug("Feature: {}", feature);
                    count++;
                }
            }
            logger.debug("Count {}", count);
            conn.export(Rio.createWriter(RDFFormat.TURTLE, System.out));
        } catch (RDF4JException e) {
            logger.error("Failed to parse test result", e);
        }
    }

    private Repository loadTestData(String filename) {
        ClassLoader classLoader = ResultProcessorTest.class.getClassLoader();
        URL url = classLoader.getResource(filename);
        Repository repository = new SailRepository(new MemoryStore());
        try (RepositoryConnection conn = repository.getConnection()) {
            try {
                conn.add(url, RDFFormat.TURTLE);
            } catch (IOException e) {
                logger.error("Failed to read " + url, e);
            }
        } catch (RDF4JException e) {
            logger.error("Failed to parse test result", e);
        }
        return repository;
    }
}

