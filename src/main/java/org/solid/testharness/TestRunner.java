package org.solid.testharness;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.config.TestHarnessConfig;
import org.solid.testharness.discovery.TestSuiteDescription;
import org.solid.testharness.reporting.ReportGenerator;
import org.solid.testharness.reporting.ResultProcessor;
import org.solid.testharness.utils.DataRepository;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Dependent
public class TestRunner {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.TestRunner");

    @Inject
    TestHarnessConfig testHarnessConfig;
    @Inject
    ResultProcessor resultProcessor;
    @Inject
    ReportGenerator reportGenerator;
    @Inject
    DataRepository dataRepository;

    public List<String> discoverTests() throws FileNotFoundException {
        logger.info("===================== DISCOVER TESTS ========================");
        TestSuiteDescription suite = new TestSuiteDescription(dataRepository);
        suite.load(new FileReader(testHarnessConfig.getTestSuiteDescription()));

        // TODO: Consider running some initial tests to discover the features provided by a server
        List<IRI> testCases = suite.getSuitableTestCases(testHarnessConfig.getTargetServer().getFeatures().keySet());
        logger.info("==== TEST CASES FOUND: {} - {}", testCases.size(), testCases);

        List<String> featurePaths = suite.locateTestCases(testHarnessConfig.getPathMappings()).stream().filter(f -> {
            File file = new File(f);
            if (!file.exists()) {
                logger.warn("FEATURE NOT IMPLEMENTED: {}", f);
                return false;
            }
            return true;
        }).collect(Collectors.toList());
        if (featurePaths.isEmpty()) {
            logger.warn("There are no tests available");
        }

        logger.info("==== RUNNING {} TEST CASES: {}", featurePaths.size(), featurePaths);
        return featurePaths;
    }

    public Results runTests(List<String> featurePaths) {
        testHarnessConfig.registerClients();
        logger.info("===================== START TESTS ========================");

        // we can also create Features which may be useful when fetching from remote resource although this may cause problems with
        // loading other features files due to classpath issues - Karate may need a RemoteResource type that knows how to fetch related
        // resources from the same URL the feature came from
//        List<Feature> featureList = featureFiles.stream().map(f -> Feature.read(new FileResource(f.toFile()))).collect(Collectors.toList());
//        logger.info("==== FEATURES {}", featureList);
//        Results results = Runner.builder()
//                .features(featureList)
//                .tags(tags)
//                .outputHtmlReport(true)
//                .parallel(8);

        List<String> tags = Collections.singletonList("~@ignore");

        Results results = Runner.builder()
                .path(featurePaths)
                .tags(tags)
                .outputHtmlReport(true)
                .suiteReports(reportGenerator)
                .parallel(testHarnessConfig.getTargetServer().getMaxThreads());

        logger.info("===================== START REPORT ========================");
        try {
            File outputDir = new File(results.getReportDir());
            logger.info("Reports location: {}", outputDir.getCanonicalPath());
            File reportTurtleFile = new File(outputDir, "report.ttl");
            logger.info("Report Turtle file: {}", reportTurtleFile.getPath());
            resultProcessor.buildTurtleReport(new FileWriter(reportTurtleFile));
            File reportHtmlFile = new File(outputDir, "report.html");
            logger.info("Report HTML/RDFa file: {}", reportHtmlFile.getPath());
            resultProcessor.buildHtmlReport(new FileWriter(reportHtmlFile));
//            resultProcessor.printReportToConsole();
        } catch (IOException e) {
            logger.error("Failed to write reports", e);
        }

        logger.info("Results:\n  Features  passed: {}, failed: {}, total: {}\n  Scenarios passed: {}, failed: {}, total: {}",
                results.getFeaturesPassed(), results.getFeaturesFailed(), results.getFeaturesTotal(),
                results.getScenariosPassed(), results.getScenariosFailed(), results.getScenariosTotal()
        );

        return results;
    }
}
