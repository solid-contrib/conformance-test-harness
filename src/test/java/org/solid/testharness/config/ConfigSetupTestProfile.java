package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class ConfigSetupTestProfile implements QuarkusTestProfile {
    public Map<String, String> getConfigOverrides() {
        return Map.of("target", "MISSING");
    }
}
