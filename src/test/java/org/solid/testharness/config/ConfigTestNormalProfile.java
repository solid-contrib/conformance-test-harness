package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTestProfile;

public class ConfigTestNormalProfile implements QuarkusTestProfile {
    public String getConfigProfile() {
        return "test";
    }
}
