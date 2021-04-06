package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Set;

public class NoInitializationTestProfile implements QuarkusTestProfile {
    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(MockTestHarnessConfig.class);
    }
}
