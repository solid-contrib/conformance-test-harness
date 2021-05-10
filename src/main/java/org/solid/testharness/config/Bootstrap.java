package org.solid.testharness.config;

import io.quarkus.runtime.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.CDI;

/**
 * This provides the TestSubject into Karate features by accessing the CDI container.
 */
public final class Bootstrap {
    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    private static Bootstrap INSTANCE;
    public static synchronized Bootstrap getInstance() {
        if (INSTANCE == null) {
            try {
                INSTANCE = new Bootstrap();
            } catch (RuntimeException e) {
                logger.error(e.toString());
            }
        }
        return INSTANCE;
    }

    private Bootstrap() {
        final Application app = Application.currentApplication();
        if (app == null) {
            logger.error("BOOTSTRAP - APP INSTANCE {}", app);
            throw new RuntimeException("Features cannot be tested directly from the IDE. " +
                    "Use the TestScenarioRunner instead.");
        }
        logger.debug("BOOTSTRAP - APP INSTANCE {}", app);
    }

    public TestSubject getTestSubject() {
        return CDI.current().select(TestSubject.class).get();
    }
}
