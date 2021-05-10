package org.solid.testharness.reporting;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.TestUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@Disabled
class FullReportReportGeneratorTest {
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
    void buildHtmlResultReportFromTarget() throws IOException {
        final File reportFile = new File("target/test-result-report.html");
        dataRepository.loadTurtle(TestUtils.getFileUrl("target/report.ttl"));
        final FileWriter wr = new FileWriter(reportFile);
        reportGenerator.buildHtmlResultReport(wr);
        wr.close();
        assertTrue(reportFile.exists());
    }

    @Test
    void buildHtmlCoverageReportFromTarget() throws IOException {
        final File reportFile = new File("target/test-coverage-report.html");
        dataRepository.loadTurtle(TestUtils.getFileUrl("target/report.ttl"));
        final FileWriter wr = new FileWriter(reportFile);
        reportGenerator.buildHtmlCoverageReport(wr);
        wr.close();
        assertTrue(reportFile.exists());
    }
}

