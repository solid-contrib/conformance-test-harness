package org.solid.testharness.config;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.TestRunner;

/**
 * Bootstrap the IoC container allowing the top level managed beans to be passed to the test runner which could either be
 * the CLI application running the tests, JUnit tests or Karate features running within the IDE.
 */
public class Bootstrap {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.config.Bootstrap");

    private WeldContainer container;

    private static Bootstrap INSTANCE;
    public synchronized static Bootstrap getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Bootstrap();
        }
        return INSTANCE;
    }

    private Bootstrap() {
        Weld weld = new Weld();
        container = weld.initialize();
        logger.debug("WELD CONTAINER INITIALIZED");
        TestHarnessConfig config = container.select(TestHarnessConfig.class).get();
        config.registerClients();
        logger.debug("SOLID CLIENTS REGISTERED");
    }

    public TestHarnessConfig getTestHarnessConfig() {
        return container.select(TestHarnessConfig.class).get();
    }

    public TestRunner getTestRunner() {
        return container.select(TestRunner.class).get();
    }

    public void shutdown() {
        container.shutdown();
    }
}
