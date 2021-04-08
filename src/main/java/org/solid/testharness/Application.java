package org.solid.testharness;

import com.intuit.karate.Results;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.config.TestHarnessConfig;
import org.solid.testharness.reporting.ResultProcessor;

import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

@QuarkusMain
public class Application implements QuarkusApplication {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.Application");

    @Inject
    TestHarnessConfig testHarnessConfig;
    @Inject
    TestRunner testRunner;
    @Inject
    ResultProcessor resultProcessor;

//    @Inject
//    @CommandLineArguments
//    String[] args;

    @Override
    public int run(String... args) throws Exception {
        logger.debug("START QUARKUS APPLICATION. Config {}", testHarnessConfig);
        testRunner.loadTestSuite();
        testRunner.filterSupportedTests();
        List<String> featurePaths = testRunner.mapTestLocations();
        Results results = testRunner.runTests(featurePaths);

        logger.info("===================== START REPORT ========================");
        try {
            File outputDir = new File("target/reports");
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

        return results != null && results.getFailCount() == 0 ? 0 : 1;
    }

    public static void main(String[] args) {
        Quarkus.run(Application.class);
    }
}
