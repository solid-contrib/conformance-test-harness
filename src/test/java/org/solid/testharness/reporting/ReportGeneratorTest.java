package org.solid.testharness.reporting;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.TestUtils;

import javax.inject.Inject;
import java.io.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ReportGeneratorTest {
    private static final Logger logger = LoggerFactory.getLogger(ReportGeneratorTest.class);

    @Inject
    DataRepository dataRepository;

    @Inject
    ReportGenerator reportGenerator;

    @BeforeEach
    void setUp() {
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            conn.clear();
        }
    }

    @Test
    void buildHtmlResultReport() throws IOException {
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/harness-sample.ttl"));
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/config-sample.ttl"));
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/testsuite-sample.ttl"));
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/testsuite-results-sample.ttl"));
        StringWriter sw = new StringWriter();
        reportGenerator.buildHtmlResultReport(sw);
        logger.debug("OUTPUT:\n{}", sw);
        assertTrue(sw.toString().length() > 1);
    }

    @Test
    void buildHtmlCoverageReport() throws IOException {
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/harness-sample.ttl"));
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/testsuite-sample.ttl"));
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/coverage-sample.ttl"));
        StringWriter sw = new StringWriter();
        reportGenerator.buildHtmlCoverageReport(sw);
        logger.debug("OUTPUT:\n{}", sw);
        assertTrue(sw.toString().length() > 1);
    }

    @Test
    void buildHtmlResultReportFile() throws IOException {
        File reportFile = new File("target/example-result-report.html");
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/harness-sample.ttl"));
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/config-sample.ttl"));
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/testsuite-sample.ttl"));
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/testsuite-results-sample.ttl"));
        FileWriter wr = new FileWriter(reportFile);
        reportGenerator.buildHtmlResultReport(wr);
        wr.close();
        assertTrue(reportFile.exists());
    }

    @Test
    void buildHtmlCoverageReportFile() throws IOException {
        File reportFile = new File("target/example-coverage-report.html");
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/harness-sample.ttl"));
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/testsuite-sample.ttl"));
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/coverage-sample.ttl"));
        FileWriter wr = new FileWriter(reportFile);
        reportGenerator.buildHtmlCoverageReport(wr);
        wr.close();
        assertTrue(reportFile.exists());
    }}

