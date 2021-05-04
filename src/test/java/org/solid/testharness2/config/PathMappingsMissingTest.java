package org.solid.testharness2.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import org.solid.testharness.config.PathMappings;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
@TestProfile(ConfigTestBlankProfile.class)
class PathMappingsMissingTest {
    @Inject
    PathMappings pathMappings;

    @Test
    void getMappings() {
        assertNull(pathMappings.getMappings());
    }

    @Test
    void testToString() {
        assertNull(pathMappings.toString());
    }
}
