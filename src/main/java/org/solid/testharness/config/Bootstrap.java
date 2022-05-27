/*
 * MIT License
 *
 * Copyright (c) 2019 - 2022 W3C Solid Community Group
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.solid.testharness.config;

import io.quarkus.runtime.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.ConformanceTestHarness;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.enterprise.inject.spi.CDI;

/**
 * This provides the TestSubject into Karate features by accessing the CDI container.
 */
public final class Bootstrap {
    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    private static Bootstrap instance;
    public static Bootstrap getInstance() {
        synchronized (Bootstrap.class) {
            if (instance == null) {
                try {
                    instance = new Bootstrap();
                } catch (RuntimeException e) {
                    logger.error("Bootstrap failed", e);
                }
            }
            return instance;
        }
    }

    @SuppressWarnings("PMD.CloseResource")
    // The application is closed by Quarkus
    private Bootstrap() {
        final Application app = Application.currentApplication();
        if (app == null) {
            logger.error("BOOTSTRAP - APP INSTANCE {}", app);
            throw new TestHarnessInitializationException("Features cannot be tested directly from the IDE. " +
                    "Use the TestScenarioRunner instead.");
        }
        logger.debug("BOOTSTRAP - APP INSTANCE {}", app);
    }

    public TestSubject getTestSubject() {
        return CDI.current().select(TestSubject.class).get();
    }

    public ConformanceTestHarness getTestHarness() {
        return CDI.current().select(ConformanceTestHarness.class).get();
    }

    public Config getConfig() {
        return CDI.current().select(Config.class).get();
    }
}
