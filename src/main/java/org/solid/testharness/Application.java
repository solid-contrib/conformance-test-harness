package org.solid.testharness;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.config.Bootstrap;

public class Application {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.Application");

    public static void main(String[] args) {
        Bootstrap bootstrap = Bootstrap.getInstance();
        TestRunner testRunner = bootstrap.getTestRunner();
        testRunner.runTests();
        bootstrap.shutdown();
    }
}
