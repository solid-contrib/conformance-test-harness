package org.solid.testharness2.config;

import io.quarkus.test.junit.QuarkusTestProfile;

public class ConfigTestBlankProfile implements QuarkusTestProfile {
    public String getConfigProfile() {
        return "blank";
    }
}
