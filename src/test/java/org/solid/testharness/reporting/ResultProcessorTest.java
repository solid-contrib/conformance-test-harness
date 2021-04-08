package org.solid.testharness.reporting;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.utils.DataRepository;

import javax.inject.Inject;
import java.io.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ResultProcessorTest {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.reporting.ResultProcessorTest");

    @Inject
    DataRepository dataRepository;

    @Inject
    ResultProcessor resultProcessor;

    @BeforeEach
    void setUp() {
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            conn.clear();
        }
    }

    @Test
    void buildHtmlResultReport() throws IOException {
        dataRepository.loadTurtle(new FileReader(new File("src/test/resources/harness-sample.ttl")));
        dataRepository.loadTurtle(new FileReader(new File("src/test/resources/config-sample.ttl")));
        dataRepository.loadTurtle(new FileReader(new File("src/test/resources/testsuite-sample.ttl")));
        dataRepository.loadTurtle(new FileReader(new File("src/test/resources/testsuite-results-sample.ttl")));
        StringWriter sw = new StringWriter();
        resultProcessor.buildHtmlResultReport(sw);
        logger.debug("OUTPUT:\n{}", sw.toString());
        assertTrue(sw.toString().length() > 1);
    }

    @Test
    void buildHtmlCoverageReport() throws IOException {
        dataRepository.loadTurtle(new FileReader(new File("src/test/resources/harness-sample.ttl")));
        dataRepository.loadTurtle(new FileReader(new File("src/test/resources/testsuite-sample.ttl")));
        dataRepository.loadTurtle(new FileReader(new File("src/test/resources/coverage-sample.ttl")));
        StringWriter sw = new StringWriter();
        resultProcessor.buildHtmlCoverageReport(sw);
        logger.debug("OUTPUT:\n{}", sw.toString());
        assertTrue(sw.toString().length() > 1);
    }
}

