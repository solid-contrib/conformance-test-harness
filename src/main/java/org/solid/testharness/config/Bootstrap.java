package org.solid.testharness.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.CDI;

/**
 * This provides the TestHarnessConfig into Karate features by accessing the CDI container.
 */
public class Bootstrap {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.config.Bootstrap");

    private static Bootstrap INSTANCE;
    public synchronized static Bootstrap getInstance() {
        if (INSTANCE == null) {
            try {
                INSTANCE = new Bootstrap();
            } catch (RuntimeException e) {
                logger.error(e.getMessage());
            }
        }
        return INSTANCE;
    }

    private Bootstrap() {
        io.quarkus.runtime.Application app = io.quarkus.runtime.Application.currentApplication();
        if (app == null) {
            throw new RuntimeException("Features cannot be tested directly from the IDE. Use the TestScenarioRunner instead.");
        }
        logger.debug("BOOTSTRAP - APP INSTANCE {}", app);
    }

    public TestHarnessConfig getTestHarnessConfig() {
        return CDI.current().select(TestHarnessConfig.class).get();
    }
}
