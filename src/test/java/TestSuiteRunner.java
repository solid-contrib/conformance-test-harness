import com.intuit.karate.Results;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.TestRunner;
import org.solid.testharness.reporting.ResultProcessor;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("solid")
@QuarkusTest
public class TestSuiteRunner {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.TestSuiteRunner");

    @Inject
    TestRunner testRunner;

    @Inject
    ResultProcessor resultProcessor;

    @Test
    void testSuite() throws FileNotFoundException {
        testRunner.loadTestSuite();
        testRunner.filterSupportedTests();
        List<String> featurePaths = testRunner.mapTestLocations();
        Results results = testRunner.runTests(featurePaths);
        assertNotNull(results);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());

        logger.info("===================== START REPORT ========================");
        try {
            File outputDir = new File("target");
            logger.info("Reports location: {}", outputDir.getCanonicalPath());

            File reportTurtleFile = new File(outputDir, "report.ttl");
            logger.info("Report Turtle file: {}", reportTurtleFile.getPath());
            resultProcessor.buildTurtleReport(new FileWriter(reportTurtleFile));

            File reportHtmlFile = new File(outputDir, "report.html");
            logger.info("Report HTML/RDFa file: {}", reportHtmlFile.getPath());
            resultProcessor.buildHtmlResultReport(new FileWriter(reportHtmlFile));
//            resultProcessor.printReportToConsole();
        } catch (Exception e) {
            logger.error("Failed to write reports", e);
        }

        logger.info("Results:\n  Features  passed: {}, failed: {}, total: {}\n  Scenarios passed: {}, failed: {}, total: {}",
                results.getFeaturesPassed(), results.getFeaturesFailed(), results.getFeaturesTotal(),
                results.getScenariosPassed(), results.getScenariosFailed(), results.getScenariosTotal()
        );
    }

    @Test
    void testSuiteCoverage() throws FileNotFoundException {
        testRunner.loadTestSuite();
        testRunner.mapTestLocations();
        try {
            File outputDir = new File("target").getCanonicalFile();
            logger.info("Reports location: {}", outputDir.getCanonicalPath());
            File coverageHtmlFile = new File(outputDir, "coverage.html");
            logger.info("Coverage report HTML/RDFa file: {}", coverageHtmlFile.getPath());
            resultProcessor.buildHtmlCoverageReport(new FileWriter(coverageHtmlFile));
        } catch (Exception e) {
            logger.error("Failed to write reports", e);
        }
    }
}
