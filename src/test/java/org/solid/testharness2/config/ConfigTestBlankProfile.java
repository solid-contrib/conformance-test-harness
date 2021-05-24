package org.solid.testharness2.config;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class ConfigTestBlankProfile implements QuarkusTestProfile {
    public String getConfigProfile() {
        return "blank";
    }
    public Map<String, String> getConfigOverrides() {
        return Map.of("LOGIN_ENDPOINT", "");
    }
}
