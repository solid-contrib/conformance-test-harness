package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Collections;
import java.util.Set;

public class NoInitializationTestProfile implements QuarkusTestProfile {
    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Collections.singleton(MockTestHarnessConfig.class);
    }
}
