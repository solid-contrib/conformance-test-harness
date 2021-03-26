package org.solid.testharness;

import com.intuit.karate.Results;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.config.TestHarnessConfig;

import javax.inject.Inject;
import java.util.List;

@QuarkusMain
public class Application implements QuarkusApplication {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.Application");

    @Inject
    TestHarnessConfig testHarnessConfig;
    @Inject
    TestRunner testRunner;

//    @Inject
//    @CommandLineArguments
//    String[] args;

    @Override
    public int run(String... args) throws Exception {
        logger.debug("START QUARKUS APPLICATION. Config {}", testHarnessConfig);
        List<String> featurePaths = testRunner.discoverTests();
        Results results = testRunner.runTests(featurePaths);
        return results != null && results.getFailCount() == 0 ? 0 : 1;
    }

    public static void main(String[] args) {
        Quarkus.run(Application.class);
    }
}
