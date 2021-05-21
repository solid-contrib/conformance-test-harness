package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ConfigTestNormalProfile implements QuarkusTestProfile {
    @Override
    public String getConfigProfile() {
        return "test";
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "SOLID_IDENTITY_PROVIDER", "https://idp.example.org",
                "LOGIN_ENDPOINT", "https://example.org/login/password",
                "SERVER_ROOT", "https://target.example.org",
                "TEST_CONTAINER", "test",
                "ALICE_WEBID", "https://alice.target.example.org/profile/card#me",
                "BOB_WEBID", "https://bob.target.example.org/profile/card#me"
        );
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return Collections.emptyList();
    }
}
