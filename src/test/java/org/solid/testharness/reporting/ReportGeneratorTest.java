package org.solid.testharness.reporting;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.TestData;
import org.solid.testharness.utils.TestUtils;

import javax.inject.Inject;
import java.io.*;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

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
    void buildTurtleReport() {
        final StringWriter sw = new StringWriter();
        assertDoesNotThrow(() -> reportGenerator.buildTurtleReport(sw));
        assertTrue(sw.toString().length() > 1);
    }

    @Test
    void buildHtmlResultReport() throws IOException {
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/harness-sample.ttl"));
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/config-sample.ttl"));
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/testsuite-sample.ttl"));
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/testsuite-results-sample.ttl"));
        final StringWriter sw = new StringWriter();
        reportGenerator.buildHtmlResultReport(sw);
//        logger.debug("OUTPUT:\n{}", sw);
        assertTrue(sw.toString().length() > 1);
    }

    @Test
    void buildHtmlCoverageReport() throws IOException {
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/harness-sample.ttl"));
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/testsuite-sample.ttl"));
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/coverage-sample.ttl"));
        final StringWriter sw = new StringWriter();
        reportGenerator.buildHtmlCoverageReport(sw);
//        logger.debug("OUTPUT:\n{}", sw);
        assertTrue(sw.toString().length() > 1);
    }

    @Test
    void buildHtmlResultReportFile() throws IOException {
        final File reportFile = new File("target/example-result-report.html");
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/harness-sample.ttl"));
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/config-sample.ttl"));
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/testsuite-sample.ttl"));
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/testsuite-results-sample.ttl"));
        final Writer wr = Files.newBufferedWriter(reportFile.toPath());
        reportGenerator.buildHtmlResultReport(wr);
        wr.close();
        assertTrue(reportFile.exists());
    }

    @Test
    void buildHtmlCoverageReportFile() throws IOException {
        final File reportFile = new File("target/example-coverage-report.html");
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/harness-sample.ttl"));
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/testsuite-sample.ttl"));
        dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/coverage-sample.ttl"));
        final Writer wr = Files.newBufferedWriter(reportFile.toPath());
        reportGenerator.buildHtmlCoverageReport(wr);
        wr.close();
        assertTrue(reportFile.exists());
    }

    @Test
    void printReportToConsole() {
        assertDoesNotThrow(() -> reportGenerator.printReportToConsole());
    }

    @Test
    void buildHtmlCoverageReportEmpty() {
        final StringWriter sw = new StringWriter();
        assertThrows(NullPointerException.class, () -> reportGenerator.buildHtmlCoverageReport(sw));
    }

    @Test
    void buildHtmlCoverageReportBadSubject() throws IOException {
        TestData.insertData(dataRepository, TestData.PREFIXES + "_:b0 doap:implements ex:spec .");
        final StringWriter sw = new StringWriter();
        assertThrows(NullPointerException.class, () -> reportGenerator.buildHtmlCoverageReport(sw));
    }
}

