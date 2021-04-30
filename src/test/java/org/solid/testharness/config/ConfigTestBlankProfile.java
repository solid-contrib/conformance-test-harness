package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTestProfile;

public class ConfigTestBlankProfile implements QuarkusTestProfile {
    public String getConfigProfile() {
        return "blank";
    }
}
