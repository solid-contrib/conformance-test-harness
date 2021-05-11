package org.solid.testharness2.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import org.solid.testharness.config.PathMappings;

import javax.inject.Inject;
import java.net.URI;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(ConfigTestBlankProfile.class)
class PathMappingsMissingTest {
    @Inject
    PathMappings pathMappings;

    @Test
    void getMappings() {
        assertTrue(pathMappings.getMappings().isEmpty());
    }

    @Test
    void testToString() {
        assertEquals("[]", pathMappings.toString());
    }

    @Test
    void unmapFeaturePath() {
        assertNull(pathMappings.unmapFeaturePath("/path"));
    }

    @Test
    void mapFeatureIri() {
        assertEquals(URI.create("file:/path"), pathMappings.mapFeatureIri(iri("file:/path")));
    }
}
