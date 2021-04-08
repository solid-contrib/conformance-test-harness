package org.solid.testharness.reporting;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.utils.DataRepository;

import javax.inject.Inject;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ResultProcessorTest {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.reporting.ResultProcessorTest");

    @Inject
    DataRepository dataRepository;

    @Inject
    ResultProcessor resultProcessor;

    @Test
    void buildHtmlResultReport() throws IOException {
        // load assertor description
        // load config for target server
        dataRepository.loadTurtle(new FileReader(new File("src/test/resources/testsuite-sample.ttl")));
        // load results
        StringWriter sw = new StringWriter();
        resultProcessor.buildHtmlResultReport(sw);
        logger.debug("OUTPUT:\n{}", sw.toString());
        assertTrue(sw.toString().length() > 1);
    }

    @Test
    void buildHtmlCoverageReport() throws IOException {
        // load assertor description
        dataRepository.loadTurtle(new FileReader(new File("src/test/resources/testsuite-sample.ttl")));
        StringWriter sw = new StringWriter();
        resultProcessor.buildHtmlCoverageReport(sw);
        logger.debug("OUTPUT:\n{}", sw.toString());
        assertTrue(sw.toString().length() > 1);
    }
}

