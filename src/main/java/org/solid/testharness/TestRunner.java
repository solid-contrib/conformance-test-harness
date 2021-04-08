package org.solid.testharness;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.config.TestHarnessConfig;
import org.solid.testharness.discovery.TestSuiteDescription;
import org.solid.testharness.reporting.ReportGenerator;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

@Dependent
public class TestRunner {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.TestRunner");

    @Inject
    TestHarnessConfig testHarnessConfig;
    @Inject
    ReportGenerator reportGenerator;

    private TestSuiteDescription suite;

    public void loadTestSuite() throws FileNotFoundException {
        suite = new TestSuiteDescription();
        suite.load(new FileReader(testHarnessConfig.getTestSuiteDescription()));
    }

    public void filterSupportedTests() {
        // TODO: Consider running some initial tests to discover the features provided by a server
        List<IRI> testCases = suite.filterSupportedTestCases(testHarnessConfig.getTargetServer().getFeatures().keySet());
        logger.info("==== TEST CASES FOUND: {} - {}", testCases.size(), testCases);
    }

    public List<String> mapTestLocations() {
        logger.info("===================== DISCOVER TESTS ========================");
        List<String> featurePaths = suite.locateTestCases(testHarnessConfig.getPathMappings());
        if (featurePaths.isEmpty()) {
            logger.warn("There are no tests available");
        } else {
            logger.info("==== RUNNING {} TEST CASES: {}", featurePaths.size(), featurePaths);
        }
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

        List<String> tags = List.of("~@ignore");

        Results results = Runner.builder()
                .path(featurePaths)
                .tags(tags)
                .outputHtmlReport(true)
                .suiteReports(reportGenerator)
                .parallel(testHarnessConfig.getTargetServer().getMaxThreads());
        return results;
    }
}
