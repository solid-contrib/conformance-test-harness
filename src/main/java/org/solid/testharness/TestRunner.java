package org.solid.testharness;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.apache.commons.lang3.StringUtils;
import org.solid.testharness.reporting.TestSuiteResults;
import org.solid.testharness.utils.FeatureResultHandler;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.List;

@Dependent
public class TestRunner {
    @Inject
    FeatureResultHandler featureResultHandler;

    @SuppressWarnings("unchecked")
    public TestSuiteResults runTests(List<String> featurePaths, int threads) {
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

        Results results = Runner.builder()
                .path(featurePaths)
                .tags("~@ignore")
                .outputHtmlReport(true)
                .suiteReports(featureResultHandler)
                .parallel(threads);
        return new TestSuiteResults(results);
    }

    public TestSuiteResults runTest(String featurePath) {
        if (StringUtils.isBlank(featurePath)) {
            throw new IllegalArgumentException("featurePath is a required parameter");
        }
        Results results = Runner.builder()
                .path(featurePath)
                .tags("~@ignore")
                .parallel(1);
        return new TestSuiteResults(results);
    }
}
