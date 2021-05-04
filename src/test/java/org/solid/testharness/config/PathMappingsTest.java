package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.TestUtils;

import javax.inject.Inject;
import java.net.URI;
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
        assertEquals("https://example.org/dummy/group1", mappings.get(0).getPrefix());
        assertEquals(TestUtils.getPathUri("src/test/resources/dummy-features/group1").toString(), mappings.get(0).getPath());
    }

    @Test
    void testToString() {
        assertEquals("[https://example.org/dummy/group1 => " + TestUtils.getPathUri("src/test/resources/dummy-features/group1") + ", " +
                "https://example.org/dummy/group2 => " + TestUtils.getPathUri("src/test/resources/dummy-features/otherExample") + ", " +
                "https://example.org/features => " + TestUtils.getPathUri("src/test/resources") + "]", pathMappings.toString());
    }

    @Test
    void setAbsoluteMapping() {
        List<PathMappings.Mapping> mappings = List.of(
                PathMappings.Mapping.create("https://example.org/dummy/group1", "/src/test/resources")
        );
        pathMappings.setMappings(mappings);
        assertEquals("[https://example.org/dummy/group1 => " + TestUtils.getPathUri("/src/test/resources") + "]", pathMappings.toString());
    }

    @Test
    void setSingleMapping() {
        List<PathMappings.Mapping> mappings = List.of(
                PathMappings.Mapping.create("https://example.org/dummy/group1", "src/test/resources")
        );
        pathMappings.setMappings(mappings);
        assertEquals("[https://example.org/dummy/group1 => " + TestUtils.getPathUri("src/test/resources") + "]", pathMappings.toString());
    }

    @Test
    void setSingleMappingSlashes() {
        List<PathMappings.Mapping> mappings = List.of(
                PathMappings.Mapping.create("https://example.org/dummy/group1/", "src/test/resources/")
        );
        pathMappings.setMappings(mappings);
        assertEquals("[https://example.org/dummy/group1 => " + TestUtils.getPathUri("src/test/resources") + "]", pathMappings.toString());
    }

    @Test
    void setSingleFeatureMapping() {
        List<PathMappings.Mapping> mappings = List.of(
                PathMappings.Mapping.create("https://example.org/dummy/group1/test.feature", "src/test/resources/test.feature")
        );
        pathMappings.setMappings(mappings);
        assertEquals("[https://example.org/dummy/group1/test.feature => " + TestUtils.getPathUri("src/test/resources/test.feature") + "]", pathMappings.toString());
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
    void unmapFeaturePathHttpsFeature() {
        IRI iri = pathMappings.unmapFeaturePath("https://example.org/remote/test.feature");
        assertEquals(iri("https://example.org/remote/test.feature"), iri);
    }

    @Test
    void unmapFeaturePathHttpFeature() {
        IRI iri = pathMappings.unmapFeaturePath("http://example.org/remote/test.feature");
        assertEquals(iri("http://example.org/remote/test.feature"), iri);
    }

    @Test
    void unmapFeaturePathFileFeature() {
        IRI iri = pathMappings.unmapFeaturePath("file:/local/test.feature");
        assertEquals(iri("file:/local/test.feature"), iri);
    }

    @Test
    void unmapFeaturePathNoMapping() {
        assertThrows(IllegalArgumentException.class, () -> pathMappings.unmapFeaturePath("src/test/nomapping/test.feature"));
    }

    @Test
    void mapFeatureIri() {
        URI path = pathMappings.mapFeatureIri(iri("https://example.org/dummy/group1/test.feature"));
        assertEquals(TestUtils.getPathUri("src/test/resources/dummy-features/group1/test.feature"), path);
    }

    @Test
    void mapFeatureIriRemoteFeature() {
        URI path = pathMappings.mapFeatureIri(iri("https://example.org/remote/test.feature"));
        assertEquals(URI.create("https://example.org/remote/test.feature"), path);
    }
}
