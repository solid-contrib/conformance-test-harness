package org.solid.testharness2.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import org.solid.testharness.config.Config;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(ConfigTestBlankProfile.class)
public class ConfigMissingTest {
    @Inject
    Config config;

    @Test
    void getTestSubject() {
        assertNull(config.getTestSubject());
    }

    @Test
    void getConfigUrl() {
        assertThrows(TestHarnessInitializationException.class, () -> config.getConfigUrl());
    }

    @Test
    void getTestSuiteDescription() {
        assertThrows(TestHarnessInitializationException.class, () -> config.getTestSuiteDescription());
    }

    @Test
    void getLoginEndpoint() {
        assertNull(config.getLoginEndpoint());
    }

    @Test
    void logConfigSettings() {
        assertDoesNotThrow(() -> config.logConfigSettings());
    }
}
