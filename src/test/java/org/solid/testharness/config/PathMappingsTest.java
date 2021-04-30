package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@TestProfile(ConfigTestNormalProfile.class)
class PathMappingsTest {
    @Inject
    PathMappings pathMappings;

    @Test
    void getMappings() {
        List<PathMappings.Mapping> mappings = pathMappings.getMappings();
        assertNotNull(mappings);
        assertEquals(1, mappings.size());
        assertEquals("https://example.org", mappings.get(0).prefix);
        assertEquals("src/test/resources/dummy-features", mappings.get(0).path);
    }

    @Test
    void testToString() {
        assertEquals("[https://example.org => src/test/resources/dummy-features]", pathMappings.toString());
    }

    @Test
    void testToString2() {
        List<PathMappings.Mapping> mappings = List.of(
            PathMappings.Mapping.create("prefix1", "path1"),
            PathMappings.Mapping.create("prefix2", "path2")
        );
        pathMappings.setMappings(mappings);
        assertEquals("[prefix1 => path1, prefix2 => path2]", pathMappings.toString());
    }
}
