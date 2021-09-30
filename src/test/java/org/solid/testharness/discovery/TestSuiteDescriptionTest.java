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
package org.solid.testharness.discovery;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.common.vocab.DOAP;
import org.solid.common.vocab.RDF;
import org.solid.common.vocab.SPEC;
import org.solid.testharness.utils.*;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TestSuiteDescriptionTest {
    @Inject
    DataRepository repository;
    @Inject
    TestSuiteDescription testSuiteDescription;

    @BeforeEach
    void setUp() {
        try (RepositoryConnection conn = repository.getConnection()) {
            Namespaces.addToRepository(repository);
            conn.clear();
        }
    }

    @Test
    void loadOneManifest() throws Exception {
        testSuiteDescription.load(List.of(
                new URL("https://example.org/test-manifest-sample-1.ttl"),
                new URL("https://example.org/specification-sample-1.ttl")
        ));
        assertTrue(ask(iri("https://example.org/specification1"), RDF.type, DOAP.Specification));
        assertTrue(ask(iri("https://example.org/test-manifest-sample-1.ttl#group1-feature1"),
                SPEC.requirementReference, iri("https://example.org/specification1#spec1")));
        assertFalse(ask(iri("https://example.org/specification2"), RDF.type, DOAP.Specification));
        assertFalse(ask(iri("https://example.org/specification2"), RDF.type, SPEC.Specification));
        assertFalse(ask(iri("https://example.org/test-manifest-sample-2.ttl#group4-feature1"),
                SPEC.requirementReference, iri("https://example.org/specification2#spec1")));
    }

    @Test
    void loadTwoManifest() throws MalformedURLException {
        testSuiteDescription.load(List.of(
                new URL("https://example.org/test-manifest-sample-1.ttl"),
                new URL("https://example.org/test-manifest-sample-2.ttl"),
                new URL("https://example.org/specification-sample-1.ttl"),
                new URL("https://example.org/specification-sample-2.ttl")
        ));
        assertTrue(ask(iri("https://example.org/specification1"), RDF.type, DOAP.Specification));
        assertTrue(ask(iri("https://example.org/test-manifest-sample-1.ttl#group1-feature1"),
                SPEC.requirementReference, iri("https://example.org/specification1#spec1")));
        assertTrue(ask(iri("https://example.org/specification2"), RDF.type, DOAP.Specification));
        assertTrue(ask(iri("https://example.org/specification2"), RDF.type, SPEC.Specification));
        assertTrue(ask(iri("https://example.org/test-manifest-sample-2.ttl#group4-feature1"),
                SPEC.requirementReference, iri("https://example.org/specification2#spec1")));
    }

    @Test
    void loadRdfa() throws MalformedURLException {
        testSuiteDescription.load(List.of(new URL("https://example.org/specification-sample-1.html")));
        assertTrue(ask(iri("https://example.org/specification1"), RDF.type, DOAP.Specification));
        assertTrue(ask(iri("https://example.org/specification1#spec1"), SPEC.statement,
                literal("text of requirement 1")));
    }

    @Test
    void loadTwoManifestMixed() throws MalformedURLException {
        testSuiteDescription.load(List.of(
                new URL("https://example.org/test-manifest-sample-1.ttl"),
                new URL("https://example.org/test-manifest-sample-2.ttl"),
                new URL("https://example.org/specification-sample-1.html"),
                new URL("https://example.org/specification-sample-2.ttl")
        ));
        assertTrue(ask(iri("https://example.org/specification1"), RDF.type, DOAP.Specification));
        assertTrue(ask(iri("https://example.org/test-manifest-sample-1.ttl#group1-feature1"),
                SPEC.requirementReference, iri("https://example.org/specification1#spec1")));
        assertTrue(ask(iri("https://example.org/specification2"), RDF.type, DOAP.Specification));
        assertTrue(ask(iri("https://example.org/test-manifest-sample-2.ttl#group4-feature1"),
                SPEC.requirementReference, iri("https://example.org/specification2#spec1")));
    }

    @Test
    void getSupportedTestCasesNullFeatures() throws MalformedURLException {
        testSuiteDescription.load(List.of(
                new URL("https://example.org/test-manifest-sample-1.ttl"),
                new URL("https://example.org/test-manifest-sample-2.ttl"),
                new URL("https://example.org/specification-sample-1.ttl"),
                new URL("https://example.org/specification-sample-2.ttl")
        ));
        final List<IRI> testCases = testSuiteDescription.getSupportedTestCases(null);
        final IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3");
        assertThat("Group 1 matches", testCases, containsInAnyOrder(expected));
    }

    @Test
    void getSupportedTestCasesEmptyFeatures() throws MalformedURLException {
        testSuiteDescription.load(List.of(
                new URL("https://example.org/test-manifest-sample-1.ttl"),
                new URL("https://example.org/test-manifest-sample-2.ttl"),
                new URL("https://example.org/specification-sample-1.ttl"),
                new URL("https://example.org/specification-sample-2.ttl")
        ));
        final List<IRI> testCases = testSuiteDescription.getSupportedTestCases(Set.of());
        final IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3");
        assertThat("Groups 1 matches", testCases, containsInAnyOrder(expected));
    }

    @Test
    void getSupportedTestCasesOneFeature() throws MalformedURLException {
        testSuiteDescription.load(List.of(
                new URL("https://example.org/test-manifest-sample-1.ttl"),
                new URL("https://example.org/test-manifest-sample-2.ttl"),
                new URL("https://example.org/specification-sample-1.ttl"),
                new URL("https://example.org/specification-sample-2.ttl")
        ));
        final List<IRI> testCases = testSuiteDescription.getSupportedTestCases(Set.of("sf1"));
        final IRI[] expected = createIriList(
                "group1/feature1", "group1/feature2", "group1/feature3", "group2/feature1"
        );
        assertThat("Groups 1, 2 match", testCases, containsInAnyOrder(expected));
    }

    @Test
    void getSupportedTestCasesFeature1And2() throws MalformedURLException {
        testSuiteDescription.load(List.of(
                new URL("https://example.org/test-manifest-sample-1.ttl"),
                new URL("https://example.org/test-manifest-sample-2.ttl"),
                new URL("https://example.org/specification-sample-1.ttl"),
                new URL("https://example.org/specification-sample-2.ttl")
        ));
        final List<IRI> testCases = testSuiteDescription.getSupportedTestCases(Set.of("sf1", "sf2"));
        final IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3",
                "group2/feature1", "group3/feature1", "group3/feature2");
        assertThat("Groups 1, 2, 3 match", testCases, containsInAnyOrder(expected));
    }

    @Test
    void getSupportedTestCasesFeature1And3() throws MalformedURLException {
        testSuiteDescription.load(List.of(
                new URL("https://example.org/test-manifest-sample-1.ttl"),
                new URL("https://example.org/test-manifest-sample-2.ttl"),
                new URL("https://example.org/specification-sample-1.ttl"),
                new URL("https://example.org/specification-sample-2.ttl")
        ));
        final List<IRI> testCases = testSuiteDescription.getSupportedTestCases(Set.of("sf1", "sf3"));
        final IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3",
                "group2/feature1", "group4/feature1", "group4/feature2", "group4/feature3");
        assertThat("Groups 1, 2, 4 match", testCases, containsInAnyOrder(expected));
    }

    @Test
    void getSupportedTestCasesAllFeatures() throws MalformedURLException {
        testSuiteDescription.load(List.of(
                new URL("https://example.org/test-manifest-sample-1.ttl"),
                new URL("https://example.org/test-manifest-sample-2.ttl"),
                new URL("https://example.org/specification-sample-1.ttl"),
                new URL("https://example.org/specification-sample-2.ttl")
        ));
        final List<IRI> testCases = testSuiteDescription.getSupportedTestCases(Set.of("sf1", "sf2", "sf3"));
        final IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3",
                "group2/feature1", "group3/feature1", "group3/feature2",
                "group4/feature1", "group4/feature2", "group4/feature3"
        );
        assertThat("All groups match", testCases, containsInAnyOrder(expected));
    }

    @Test
    void getSupportedTestCasesFeature3() throws MalformedURLException {
        testSuiteDescription.load(List.of(
                new URL("https://example.org/test-manifest-sample-1.ttl"),
                new URL("https://example.org/test-manifest-sample-2.ttl"),
                new URL("https://example.org/specification-sample-1.ttl"),
                new URL("https://example.org/specification-sample-2.ttl")
        ));
        final List<IRI> testCases = testSuiteDescription.getSupportedTestCases(Set.of("sf3"));
        final IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3");
        assertThat("Group 1 matches", testCases, containsInAnyOrder(expected));
    }

    @Test
    void getAllTestCases() throws MalformedURLException {
        testSuiteDescription.load(List.of(
                new URL("https://example.org/test-manifest-sample-1.ttl"),
                new URL("https://example.org/test-manifest-sample-2.ttl"),
                new URL("https://example.org/specification-sample-1.ttl"),
                new URL("https://example.org/specification-sample-2.ttl")
        ));
        final List<IRI> testCases = testSuiteDescription.getAllTestCases();
        final IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3",
                "group2/feature1", "group3/feature1", "group3/feature2",
                "group4/feature1", "group4/feature2", "group4/feature3");
        assertThat("Group 1 matches", testCases, containsInAnyOrder(expected));
    }

    @Test
    void mapEmptyList() {
        final List<IRI> testCases = Collections.emptyList();
        assertTrue(testSuiteDescription.locateTestCases(testCases).isEmpty());
    }

    @Test
    void mapRemoteFeaturePaths() {
        final List<IRI> testCases = List.of(iri("https://example.org/test/group3/feature1"));
        assertThrows(TestHarnessInitializationException.class, () -> testSuiteDescription.locateTestCases(testCases));
    }

    @Test
    void mapFeaturePaths() {
        final List<IRI> testCases = List.of(iri("https://example.org/features/test.feature"));
        final List<String> paths = testSuiteDescription.locateTestCases(testCases);
        final String[] expected = new String[]{TestUtils.getPathUri("src/test/resources/test.feature").toString()};
        assertThat("Locations match", paths, containsInAnyOrder(expected));
    }

    @Test
    void mapMissingFeaturePaths() {
        final List<IRI> testCases = List.of(
                iri("https://example.org/test/group1/feature1"), iri("https://example.org/test/group1/feature2"),
                iri("https://example.org/test/group1/feature3"), iri("https://example.org/test/group2/feature1")
        );
        final List<String> paths = testSuiteDescription.locateTestCases(testCases);
        final String[] expected = new String[]{TestUtils.getPathUri("src/test/resources/test-features/group1/feature1")
                .toString(),
                TestUtils.getPathUri("src/test/resources/test-features/group1/feature2").toString(),
                TestUtils.getPathUri("src/test/resources/test-features/otherExample/feature1").toString()
        };
        assertThat("Locations match", paths, containsInAnyOrder(expected));
        // TODO: Test for titles being inserted once this is implemented
//        try (RepositoryConnection conn = repository.getConnection()) {
//            assertTrue(conn.hasStatement(null, EARL.mode, EARL.untested, false));
//        }
    }

    private IRI[] createIriList(final String... testCases) {
        return Stream.of(testCases).map(s -> iri("https://example.org/test/" + s)).toArray(IRI[]::new);
    }

    private boolean ask(final Resource subject, final IRI predicate, final Value object) {
        try (RepositoryConnection conn = repository.getConnection()) {
            return conn.hasStatement(subject, predicate, object, false);
        }
    }
}
