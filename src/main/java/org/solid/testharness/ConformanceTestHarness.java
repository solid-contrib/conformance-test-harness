package org.solid.testharness;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.config.TestHarnessConfig;
import org.solid.testharness.discovery.TestSuiteDescription;
import org.solid.testharness.reporting.ReportGenerator;
import org.solid.testharness.reporting.TestSuiteResults;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@ApplicationScoped
public class ConformanceTestHarness {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.ConformanceTestHarness");

    @Inject
    TestHarnessConfig testHarnessConfig;
    @Inject
    TestSuiteDescription testSuiteDescription;
    @Inject
    TestRunner testRunner;
    @Inject
    ReportGenerator reportGenerator;

    public void initialize() {
        // setup repo and assertor
    }

    public boolean createCoverageReport() {
        logger.info("===================== DISCOVER TESTS ========================");
        try {
            testSuiteDescription.load(testHarnessConfig.getTestSuiteDescription());
            List<IRI> testCases = testSuiteDescription.getAllTestCases();
            List<String> featurePaths = testSuiteDescription.locateTestCases(testCases, testHarnessConfig.getPathMappings());
            if (featurePaths.isEmpty()) {
                logger.warn("There are no tests available");
                return true;
            }
        } catch (TestHarnessInitializationException e) {
            logger.error("Cannot build report", e);
            return false;
        }

        logger.info("===================== BUILD REPORT ========================");
        File outputDir = testHarnessConfig.getOutputDirectory();
        logger.info("Reports location: {}", outputDir.getPath());
        try {
            File coverageHtmlFile = new File(outputDir, "coverage.html");
            logger.info("Coverage report HTML/RDFa file: {}", coverageHtmlFile.getPath());
            reportGenerator.buildHtmlCoverageReport(new FileWriter(coverageHtmlFile));
            return true;
        } catch (IOException e) {
            logger.error("Failed to write coverage report", e);
            return false;
        }
    }

    public TestSuiteResults runTestSuites() {
        logger.info("===================== DISCOVER TESTS ========================");
        List<String> featurePaths;
        try {
            testSuiteDescription.load(testHarnessConfig.getTestSuiteDescription());
            testHarnessConfig.loadTestSubjectConfig(); // TODO:is this in right place?
            // TODO: Consider running some initial tests to discover the features provided by a server
            List<IRI> testCases = testSuiteDescription.getSupportedTestCases(testHarnessConfig.getTargetServer().getFeatures().keySet());
            logger.info("==== TEST CASES FOUND: {} - {}", testCases.size(), testCases);

            featurePaths = testSuiteDescription.locateTestCases(testCases, testHarnessConfig.getPathMappings());
            if (featurePaths.isEmpty()) {
                logger.warn("There are no tests available");
                return null;
            } else {
                logger.info("==== RUNNING {} TEST CASES: {}", featurePaths.size(), featurePaths);
            }

            testHarnessConfig.registerClients();
        } catch (TestHarnessInitializationException e) {
            logger.error("Cannot run test suites", e);
            return null;
        }

        logger.info("===================== RUN TESTS ========================");
        TestSuiteResults results = testRunner.runTests(featurePaths, testHarnessConfig.getTargetServer().getMaxThreads());

        logger.info("===================== BUILD REPORTS ========================");
        File outputDir = testHarnessConfig.getOutputDirectory();
        logger.info("Reports location: {}", outputDir.getPath());
        try {
            File reportTurtleFile = new File(outputDir, "report.ttl");
            logger.info("Report Turtle file: {}", reportTurtleFile.getPath());
            reportGenerator.buildTurtleReport(new FileWriter(reportTurtleFile));

            File reportHtmlFile = new File(outputDir, "report.html");
            logger.info("Report HTML/RDFa file: {}", reportHtmlFile.getPath());
            reportGenerator.buildHtmlResultReport(new FileWriter(reportHtmlFile));
//            resultProcessor.printReportToConsole();
        } catch (Exception e) {
            logger.error("Failed to write reports", e);
        }

        logger.info(results.toString());
        return results;
    }
}
