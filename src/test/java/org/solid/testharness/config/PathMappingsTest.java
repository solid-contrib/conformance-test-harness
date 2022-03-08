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
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.TestHarnessInitializationException;
import org.solid.testharness.utils.TestUtils;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
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
        final List<PathMappings.PathMapping> mappings = pathMappings.mappings();
        assertNotNull(mappings);
        assertEquals(7, mappings.size());
        assertEquals("https://example.org/test/group1", mappings.get(0).prefix());
        assertEquals(
                TestUtils.getPathUri("src/test/resources/test-features/group1").toString(), mappings.get(0).path()
        );
    }

    @Test
    void testToString() {
        assertEquals("[" + String.join(", ", List.of(
                pathMappingString("https://example.org/test/group1", "src/test/resources/test-features/group1"),
                pathMappingString("https://example.org/test/group2", "src/test/resources/test-features/otherExample"),
                pathMappingString("https://example.org/features", "src/test/resources"),
                pathMappingString("https://example.org/specification", "src/test/resources/discovery/specification"),
                pathMappingString("https://example.org/test-manifest", "src/test/resources/discovery/test-manifest"),
                pathMappingString("https://example.org/additional", "src/test/resources/discovery/additional"),
                pathMappingString("https://example.org/badmapping", "https://example.org:-1")
                )) + "]", pathMappings.stringValue());
    }

    private String pathMappingString(final String prefix, final String path) {
        return prefix + " => " + (path.startsWith("http") ? path : TestUtils.getPathUri(path));
    }

    @Test
    void createMapping() {
        final TestMapping mapping = new TestMapping("https://example.org/test/group1", "src/test/resources");
        assertEquals("https://example.org/test/group1 => " + TestUtils.getPathUri("src/test/resources"),
                mapping.stringValue()
        );
    }

    @Test
    void setSingleMappingSlashes() {
        final TestMapping mapping = new TestMapping("https://example.org/test/group1/", "src/test/resources/");
        assertEquals("https://example.org/test/group1 => " + TestUtils.getPathUri("src/test/resources"),
                mapping.stringValue()
        );
    }

    @Test
    void setSingleFeatureMapping() {
        final TestMapping mapping = new TestMapping("https://example.org/test/group1/test.feature",
                "src/test/resources/test.feature"
        );
        assertEquals("https://example.org/test/group1/test.feature => " +
                TestUtils.getPathUri("src/test/resources/test.feature"), mapping.stringValue());
    }

    @Test
    void unmapFeaturePath() {
        final String iri = pathMappings.unmapFeaturePath("src/test/resources/test-features/group1/test.feature");
        assertEquals("https://example.org/test/group1/test.feature", iri);
    }

    @Test
    void unmapFeaturePathHttpsFeature() {
        final String iri = pathMappings.unmapFeaturePath("https://example.org/remote/test.feature");
        assertEquals("https://example.org/remote/test.feature", iri);
    }

    @Test
    void unmapFeaturePathHttpFeature() {
        final String iri = pathMappings.unmapFeaturePath("http://example.org/remote/test.feature");
        assertEquals("http://example.org/remote/test.feature", iri);
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
    void mapIri() {
        final URI uri = pathMappings.mapIri(iri("https://example.org/test-manifest-sample-1.ttl"));
        assertEquals(TestUtils.getPathUri("src/test/resources/discovery/test-manifest-sample-1.ttl"), uri);
    }

    @Test
    void mapIriNoMapping() {
        final URI uri = pathMappings.mapIri(iri("https://example.org/unknown/test.ttl"));
        assertEquals(URI.create("https://example.org/unknown/test.ttl"), uri);
    }

    @Test
    void mapUrl() throws MalformedURLException {
        final URL url = pathMappings.mapUrl(new URL("https://example.org/specification-sample-1.ttl"));
        assertEquals(TestUtils.getFileUrl("src/test/resources/discovery/specification-sample-1.ttl"), url);
    }

    @Test
    void mapUrlFail() {
        assertThrows(TestHarnessInitializationException.class,
                () -> pathMappings.mapUrl(new URL("https://example.org/badmapping-sample-1.ttl")));
    }

    @Test
    void mapIriRemoteFeature() {
        final URI path = pathMappings.mapIri(iri("https://example.org/remote/test.feature"));
        assertEquals(URI.create("https://example.org/remote/test.feature"), path);
    }

    public static class TestMapping implements PathMappings.PathMapping {
        public String prefix;
        public String path;

        public TestMapping (final String prefix, final String path) {
            this.prefix = new PathMappings.PrefixConverter().convert(prefix);
            this.path = new PathMappings.PathConverter().convert(path);
        }

        @Override
        public String prefix() {
            return prefix;
        }

        @Override
        public String path() {
            return path;
        }
    }
}
