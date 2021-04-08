package org.solid.testharness.reporting;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class FullReportTestProfile implements QuarkusTestProfile {
    public Map<String, String> getConfigOverrides() {
        return Map.of("testSuiteNamespace", "https://github.com/solid/conformance-test-harness/example/example.ttl#");
    }
}
