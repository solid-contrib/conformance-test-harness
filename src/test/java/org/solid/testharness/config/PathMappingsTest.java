/*
 * MIT License
 *
 * Copyright (c) 2021 Solid
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
        final List<PathMappings.Mapping> mappings = pathMappings.getMappings();
        assertNotNull(mappings);
        assertEquals(3, mappings.size());
        assertEquals("https://example.org/dummy/group1", mappings.get(0).getPrefix());
        assertEquals(
                TestUtils.getPathUri("src/test/resources/dummy-features/group1").toString(), mappings.get(0).getPath()
        );
    }

    @Test
    void testToString() {
        assertEquals("[https://example.org/dummy/group1 => " +
                TestUtils.getPathUri("src/test/resources/dummy-features/group1") + ", " +
                "https://example.org/dummy/group2 => " +
                TestUtils.getPathUri("src/test/resources/dummy-features/otherExample") + ", " +
                "https://example.org/features => " + TestUtils.getPathUri("src/test/resources") + "]",
                pathMappings.toString()
        );
    }

    @Test
    void setAbsoluteMapping() {
        final List<PathMappings.Mapping> mappings = List.of(
                PathMappings.Mapping.create("https://example.org/dummy/group1", "/src/test/resources")
        );
        pathMappings.setMappings(mappings);
        assertEquals("[https://example.org/dummy/group1 => " + TestUtils.getPathUri("/src/test/resources") + "]",
                pathMappings.toString()
        );
    }

    @Test
    void setSingleMapping() {
        final List<PathMappings.Mapping> mappings = List.of(
                PathMappings.Mapping.create("https://example.org/dummy/group1", "src/test/resources")
        );
        pathMappings.setMappings(mappings);
        assertEquals("[https://example.org/dummy/group1 => " + TestUtils.getPathUri("src/test/resources") + "]",
                pathMappings.toString()
        );
    }

    @Test
    void setSingleMappingSlashes() {
        final List<PathMappings.Mapping> mappings = List.of(
                PathMappings.Mapping.create("https://example.org/dummy/group1/", "src/test/resources/")
        );
        pathMappings.setMappings(mappings);
        assertEquals("[https://example.org/dummy/group1 => " + TestUtils.getPathUri("src/test/resources") + "]",
                pathMappings.toString()
        );
    }

    @Test
    void setSingleFeatureMapping() {
        final List<PathMappings.Mapping> mappings = List.of(
                PathMappings.Mapping.create("https://example.org/dummy/group1/test.feature",
                        "src/test/resources/test.feature"
                )
        );
        pathMappings.setMappings(mappings);
        assertEquals("[https://example.org/dummy/group1/test.feature => " +
                TestUtils.getPathUri("src/test/resources/test.feature") + "]", pathMappings.toString());
    }

    @Test
    void setNullMappings() {
        assertThrows(NullPointerException.class, () -> pathMappings.setMappings(null));
    }

    @Test
    void setNullMappings2() {
        final List nullList = new ArrayList();
        nullList.add(null);
        pathMappings.setMappings(nullList);
        assertTrue(pathMappings.getMappings().isEmpty());
    }

    @Test
    void unmapFeaturePath() {
        final IRI iri = pathMappings.unmapFeaturePath("src/test/resources/dummy-features/group1/test.feature");
        assertEquals(iri("https://example.org/dummy/group1/test.feature"), iri);
    }

    @Test
    void unmapFeaturePathHttpsFeature() {
        final IRI iri = pathMappings.unmapFeaturePath("https://example.org/remote/test.feature");
        assertEquals(iri("https://example.org/remote/test.feature"), iri);
    }

    @Test
    void unmapFeaturePathHttpFeature() {
        final IRI iri = pathMappings.unmapFeaturePath("http://example.org/remote/test.feature");
        assertEquals(iri("http://example.org/remote/test.feature"), iri);
    }

    @Test
    void unmapFeaturePathFileFeature() {
        assertNull(pathMappings.unmapFeaturePath("file:/local/test.feature"));
    }

    @Test
    void unmapFeaturePathNoMapping() {
        assertNull(pathMappings.unmapFeaturePath("src/test/nomapping/test.feature"));
    }

    @Test
    void mapFeatureIri() {
        final URI path = pathMappings.mapFeatureIri(iri("https://example.org/dummy/group1/test.feature"));
        assertEquals(TestUtils.getPathUri("src/test/resources/dummy-features/group1/test.feature"), path);
    }

    @Test
    void mapFeatureIriRemoteFeature() {
        final URI path = pathMappings.mapFeatureIri(iri("https://example.org/remote/test.feature"));
        assertEquals(URI.create("https://example.org/remote/test.feature"), path);
    }
}
