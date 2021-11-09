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
import org.solid.common.vocab.*;
import org.solid.testharness.utils.*;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;

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
        repository.setAssertor(iri(TestData.SAMPLE_NS, "testharness"));
        repository.setTestSubject(iri(TestData.SAMPLE_NS, "test"));
    }

    @Test
    void loadOneManifest() throws MalformedURLException {
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
    void setNonRunningTestAssertionsNull() throws MalformedURLException {
        testSuiteDescription.load(List.of( new URL("https://example.org/test-manifest-sample-1.ttl")));
        testSuiteDescription.setNonRunningTestAssertions(null, null);
        assertEquals(6, count(null, RDF.type, TD.TestCase));
        assertEquals(3, count(null, RDF.type, EARL.Assertion));
        assertEquals(3, count(null, EARL.outcome, EARL.inapplicable));
        assertFalse(ask(null, EARL.test, createTestIri("group1/feature1")));
        assertFalse(ask(null, EARL.test, createTestIri("group1/feature2")));
        assertFalse(ask(null, EARL.test, createTestIri("group1/feature3")));
    }

    @Test
    void setNonRunningTestAssertionsEmptyFeatures() throws MalformedURLException {
        testSuiteDescription.load(List.of( new URL("https://example.org/test-manifest-sample-1.ttl")));
        testSuiteDescription.setNonRunningTestAssertions(Set.of(), null);
        assertEquals(6, count(null, RDF.type, TD.TestCase));
        assertEquals(3, count(null, RDF.type, EARL.Assertion));
        assertEquals(3, count(null, EARL.outcome, EARL.inapplicable));
        assertFalse(ask(null, EARL.test, createTestIri("group1/feature1")));
        assertFalse(ask(null, EARL.test, createTestIri("group1/feature2")));
        assertFalse(ask(null, EARL.test, createTestIri("group1/feature3")));
    }

    @Test
    void setNonRunningTestAssertionsEmptyFilters() throws MalformedURLException {
        testSuiteDescription.load(List.of( new URL("https://example.org/test-manifest-sample-1.ttl")));
        testSuiteDescription.setNonRunningTestAssertions(null, List.of());
        assertEquals(6, count(null, RDF.type, TD.TestCase));
        assertEquals(3, count(null, RDF.type, EARL.Assertion));
        assertEquals(3, count(null, EARL.outcome, EARL.inapplicable));
        assertFalse(ask(null, EARL.test, createTestIri("group1/feature1")));
        assertFalse(ask(null, EARL.test, createTestIri("group1/feature2")));
        assertFalse(ask(null, EARL.test, createTestIri("group1/feature3")));
    }

    @Test
    void setNonRunningTestAssertionsOneFeature() throws MalformedURLException {
        testSuiteDescription.load(List.of( new URL("https://example.org/test-manifest-sample-1.ttl")));
        testSuiteDescription.setNonRunningTestAssertions(Set.of("sf1"), null);
        assertEquals(6, count(null, RDF.type, TD.TestCase));
        assertEquals(2, count(null, RDF.type, EARL.Assertion));
        assertEquals(2, count(null, EARL.outcome, EARL.inapplicable));
        assertFalse(ask(null, EARL.test, createTestIri("group1/feature1")));
        assertFalse(ask(null, EARL.test, createTestIri("group1/feature2")));
        assertFalse(ask(null, EARL.test, createTestIri("group1/feature3")));
        assertFalse(ask(null, EARL.test, createTestIri("group2/feature1")));
    }

    @Test
    void setNonRunningTestAssertionsFeature1And2() throws MalformedURLException {
        testSuiteDescription.load(List.of( new URL("https://example.org/test-manifest-sample-1.ttl")));
        testSuiteDescription.setNonRunningTestAssertions(Set.of("sf1",  "sf2"), null);
        assertEquals(6, count(null, RDF.type, TD.TestCase));
        assertEquals(0, count(null, RDF.type, EARL.Assertion));
        assertEquals(0, count(null, EARL.outcome, EARL.inapplicable));
    }

    @Test
    void setNonRunningTestAssertionsFeature1And3() throws MalformedURLException {
        testSuiteDescription.load(List.of( new URL("https://example.org/test-manifest-sample-1.ttl")));
        testSuiteDescription.setNonRunningTestAssertions(Set.of("sf1",  "sf3"), null);
        assertEquals(6, count(null, RDF.type, TD.TestCase));
        assertEquals(2, count(null, RDF.type, EARL.Assertion));
        assertEquals(2, count(null, EARL.outcome, EARL.inapplicable));
        assertFalse(ask(null, EARL.test, createTestIri("group1/feature1")));
        assertFalse(ask(null, EARL.test, createTestIri("group1/feature2")));
        assertFalse(ask(null, EARL.test, createTestIri("group1/feature3")));
        assertFalse(ask(null, EARL.test, createTestIri("group2/feature1")));
    }

    @Test
    void setNonRunningTestAssertionsNoFeaturesFilterGroup1() throws MalformedURLException {
        testSuiteDescription.load(List.of( new URL("https://example.org/test-manifest-sample-1.ttl")));
        testSuiteDescription.setNonRunningTestAssertions(null, List.of("group1"));
        assertEquals(6, count(null, RDF.type, TD.TestCase));
        assertEquals(3, count(null, RDF.type, EARL.Assertion));
        assertEquals(0, count(null, EARL.outcome, EARL.untested));
        assertEquals(3, count(null, EARL.outcome, EARL.inapplicable));
        assertFalse(ask(null, EARL.test, createTestIri("group2/feature1")));
        assertFalse(ask(null, EARL.test, createTestIri("group3/feature1")));
        assertFalse(ask(null, EARL.test, createTestIri("group3/feature2")));
    }

    @Test
    void setNonRunningTestAssertionsFilterGroup1() throws MalformedURLException {
        testSuiteDescription.load(List.of( new URL("https://example.org/test-manifest-sample-1.ttl")));
        testSuiteDescription.setNonRunningTestAssertions(Set.of("sf1", "sf2"), List.of("group1"));
        assertEquals(6, count(null, RDF.type, TD.TestCase));
        assertEquals(3, count(null, RDF.type, EARL.Assertion));
        assertEquals(3, count(null, EARL.outcome, EARL.untested));
        assertEquals(0, count(null, EARL.outcome, EARL.inapplicable));
        assertFalse(ask(null, EARL.test, createTestIri("group2/feature1")));
        assertFalse(ask(null, EARL.test, createTestIri("group3/feature1")));
        assertFalse(ask(null, EARL.test, createTestIri("group3/feature2")));
    }

    @Test
    void mapEmptyList() {
        assertDoesNotThrow(() -> testSuiteDescription.prepareTestCases(false));
    }

    @Test
    void mapRemoteFeaturePaths() {
        add(iri(TestData.SAMPLE_NS, "testcase"), RDF.type, TD.TestCase);
        add(iri(TestData.SAMPLE_NS, "testcase"), SPEC.testScript, iri("https://remote.org/remote-test"));
        assertThrows(TestHarnessInitializationException.class,
                () -> testSuiteDescription.prepareTestCases(false));
    }

    @Test
    void mapMissingFeaturePaths() {
        add(iri(TestData.SAMPLE_NS, "testcase"), RDF.type, TD.TestCase);
        add(iri(TestData.SAMPLE_NS, "testcase"), SPEC.testScript, iri("https://example.org/features/unknown.feature"));
        testSuiteDescription.prepareTestCases(false);
        assertTrue(testSuiteDescription.getFeaturePaths().isEmpty());
    }

    @Test
    void prepareTestCases() {
        add(iri(TestData.SAMPLE_NS, "testcase"), RDF.type, TD.TestCase);
        add(iri(TestData.SAMPLE_NS, "testcase"), SPEC.testScript, iri("https://example.org/features/test.feature"));
        testSuiteDescription.prepareTestCases(false);
        final List<String> paths = testSuiteDescription.getFeaturePaths();
        final String[] expected = new String[]{TestUtils.getPathUri("src/test/resources/test.feature").toString()};
        assertThat("Locations match", paths, containsInAnyOrder(expected));
    }

    @Test
    void prepareTestCasesWithAssertionInTestMode() {
        add(iri(TestData.SAMPLE_NS, "testcase"), RDF.type, TD.TestCase);
        add(iri(TestData.SAMPLE_NS, "testcase"), SPEC.testScript, iri("https://example.org/features/test.feature"));
        add(iri(TestData.SAMPLE_NS, "assertion"), RDF.type, EARL.Assertion);
        add(iri(TestData.SAMPLE_NS, "assertion"), EARL.test, iri(TestData.SAMPLE_NS, "testcase"));
        testSuiteDescription.prepareTestCases(false);
        assertTrue(testSuiteDescription.getFeaturePaths().isEmpty());
    }

    @Test
    void prepareTestCasesWithAssertionInCoverageMode() {
        add(iri(TestData.SAMPLE_NS, "testcase"), RDF.type, TD.TestCase);
        add(iri(TestData.SAMPLE_NS, "testcase"), SPEC.testScript,
                iri("https://example.org/features/test.feature"));
        add(iri(TestData.SAMPLE_NS, "assertion"), RDF.type, EARL.Assertion);
        add(iri(TestData.SAMPLE_NS, "assertion"), EARL.test, iri(TestData.SAMPLE_NS, "testcase"));
        testSuiteDescription.prepareTestCases(true);
        assertTrue(testSuiteDescription.getFeaturePaths().isEmpty());
//        final List<String> paths = testSuiteDescription.getFeaturePaths();
//        final String[] expected = new String[]{TestUtils.getPathUri("src/test/resources/test.feature").toString()};
//        assertThat("Locations match", paths, containsInAnyOrder(expected));
    }

    @Test
    void prepareTestCasesNotImplemented() {
        add(iri(TestData.SAMPLE_NS, "testcase"), RDF.type, TD.TestCase);
        add(iri(TestData.SAMPLE_NS, "testcase"), SPEC.testScript, literal("test.feature"));
        testSuiteDescription.prepareTestCases(true);
        assertTrue(testSuiteDescription.getFeaturePaths().isEmpty());
    }

    @Test
    void prepareTestCasesTitle() throws Exception {
        add(iri("https://example.org/group1-feature1"), RDF.type, TD.TestCase);
        add(iri("https://example.org/group1-feature1"), SPEC.testScript,
                iri("https://example.org/test/group1/feature1"));
        add(iri("https://example.org/group1-feature2"), RDF.type, TD.TestCase);
        add(iri("https://example.org/group1-feature2"), SPEC.testScript,
                iri("https://example.org/test/group1/feature2"));
        add(iri("https://example.org/group2-feature1"), RDF.type, TD.TestCase);
        add(iri("https://example.org/group2-feature1"), SPEC.testScript,
                iri("https://example.org/test/group2/feature1"));
        add(iri("https://example.org/group2-feature2"), RDF.type, TD.TestCase);
        add(iri("https://example.org/group2-feature2"), SPEC.testScript,
                iri("https://example.org/test/group2/"));
        testSuiteDescription.prepareTestCases(true);
        assertTrue(ask(iri("https://example.org/group1-feature1"), DCTERMS.title, literal("Feature 1 title")));
        assertTrue(ask(iri("https://example.org/group1-feature2"), DCTERMS.title, literal("Feature 2 title")));
        assertEquals(0, count(iri("https://example.org/group2-feature1"), DCTERMS.title, null));
    }

    @Test
    void getTestCases() {
        add(iri(TestData.SAMPLE_NS, "testcase"), RDF.type, TD.TestCase);
        add(iri(TestData.SAMPLE_NS, "testcase"), SPEC.testScript, iri("https://example.org/features/test.feature"));
        add(iri(TestData.SAMPLE_NS, "assertion"), RDF.type, EARL.Assertion);
        add(iri(TestData.SAMPLE_NS, "assertion"), EARL.test, iri(TestData.SAMPLE_NS, "testcase"));
        final List<IRI> testCases = testSuiteDescription.getTestCases(false);
        final IRI[] expected = new IRI[]{iri(TestData.SAMPLE_NS, "testcase")};
        assertThat("TestCases match", testCases, containsInAnyOrder(expected));
    }

    @Test
    void getTestCasesFiltered() {
        add(iri(TestData.SAMPLE_NS, "testcase"), RDF.type, TD.TestCase);
        add(iri(TestData.SAMPLE_NS, "testcase"), SPEC.testScript, iri("https://example.org/features/test.feature"));
        add(iri(TestData.SAMPLE_NS, "assertion"), RDF.type, EARL.Assertion);
        add(iri(TestData.SAMPLE_NS, "assertion"), EARL.test, iri(TestData.SAMPLE_NS, "testcase"));
        assertTrue(testSuiteDescription.getTestCases(true).isEmpty());
    }

    @Test
    void getTestCasesFiltered2() {
        add(iri(TestData.SAMPLE_NS, "testcase"), RDF.type, TD.TestCase);
        add(iri(TestData.SAMPLE_NS, "testcase"), SPEC.testScript, iri("https://example.org/features/test.feature"));
        final List<IRI> testCases = testSuiteDescription.getTestCases(true);
        final IRI[] expected = new IRI[]{iri(TestData.SAMPLE_NS, "testcase")};
        assertThat("TestCases match", testCases, containsInAnyOrder(expected));
    }

    private IRI createTestIri(final String testCase) {
        return iri("https://example.org/test/" + testCase);
    }

    private void add(final Resource subject, final IRI predicate, final Value object) {
        try (RepositoryConnection conn = repository.getConnection()) {
            conn.add(subject, predicate, object);
        }
    }

    private boolean ask(final Resource subject, final IRI predicate, final Value object) {
        try (RepositoryConnection conn = repository.getConnection()) {
            return conn.hasStatement(subject, predicate, object, false);
        }
    }

    private long count(final Resource subject, final IRI predicate, final Value object) {
        try (RepositoryConnection conn = repository.getConnection()) {
            return conn.getStatements(subject, predicate, object, false).stream().count();
        }
    }
}
