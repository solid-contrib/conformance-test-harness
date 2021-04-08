package org.solid.testharness.reporting;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.utils.DataRepository;

import javax.inject.Inject;
import java.io.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(FullReportTestProfile.class)
@Disabled
class FullReportResultProcessorTest {
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
    void buildHtmlResultReportFromTarget() throws IOException {
        File reportFile = new File("target/test-result-report.html");
        dataRepository.loadTurtle(new FileReader(new File("target/report.ttl")));
        FileWriter wr = new FileWriter(reportFile);
        resultProcessor.buildHtmlResultReport(wr);
        wr.close();
        assertTrue(reportFile.exists());
    }

    @Test
    void buildHtmlCoverageReportFromTarget() throws IOException {
        File reportFile = new File("target/test-coverage-report.html");
        dataRepository.loadTurtle(new FileReader(new File("target/report.ttl")));
        FileWriter wr = new FileWriter(reportFile);
        resultProcessor.buildHtmlCoverageReport(wr);
        wr.close();
        assertTrue(reportFile.exists());
    }
}

