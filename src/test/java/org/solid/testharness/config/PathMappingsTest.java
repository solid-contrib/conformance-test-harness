package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(ConfigTestNormalProfile.class)
class PathMappingsTest {
    @Inject
    PathMappings pathMappings;

    @Test
    void getMappings() {
        List<PathMappings.Mapping> mappings = pathMappings.getMappings();
        assertNotNull(mappings);
        assertEquals(3, mappings.size());
        assertEquals("https://example.org/dummy/group1", mappings.get(0).prefix);
        assertEquals("src/test/resources/dummy-features/group1", mappings.get(0).path);
    }

    @Test
    void testToString() {
        assertEquals("[https://example.org/dummy/group1 => src/test/resources/dummy-features/group1, " +
                "https://example.org/dummy/group2 => src/test/resources/dummy-features/otherExample, " +
                "https://example.org/features => src/test/resources]", pathMappings.toString());
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

    @Test
    void setSingleMapping() {
        List<PathMappings.Mapping> mappings = List.of(
                PathMappings.Mapping.create("prefix1", "path1")
        );
        pathMappings.setMappings(mappings);
        assertEquals("[prefix1 => path1]", pathMappings.toString());
    }

    @Test
    void setNullMappings() {
        pathMappings.setMappings(null);
        assertNull(pathMappings.getMappings());
    }

    @Test
    void setNullMappings2() {
        List nullList = new ArrayList();
        nullList.add(null);
        pathMappings.setMappings(nullList);
        assertNull(pathMappings.getMappings());
    }

    @Test
    void unmapFeaturePath() {
        IRI iri = pathMappings.unmapFeaturePath("src/test/resources/dummy-features/group1/test.feature");
        assertEquals(iri("https://example.org/dummy/group1/test.feature"), iri);
    }

    @Test
    void unmapFeaturePathRemoteFeature() {
        IRI iri = pathMappings.unmapFeaturePath("https://example.org/remote/test.feature");
        assertEquals(iri("https://example.org/remote/test.feature"), iri);
    }

    @Test
    void unmapFeaturePathNoMapping() {
        assertThrows(IllegalArgumentException.class, () -> pathMappings.unmapFeaturePath("src/test/nomapping/test.feature"));
    }

    @Test
    void mapFeatureIri() {
        String path = pathMappings.mapFeatureIri(iri("https://example.org/dummy/group1/test.feature"));
        assertEquals("src/test/resources/dummy-features/group1/test.feature", path);
    }

    @Test
    void mapFeatureIriRemoteFeature() {
        String path = pathMappings.mapFeatureIri(iri("https://example.org/remote/test.feature"));
        assertEquals("https://example.org/remote/test.feature", path);
    }
}
