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
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.common.vocab.*;
import org.solid.testharness.config.Config;
import org.solid.testharness.utils.*;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TestSuiteDescriptionTest {
    private static String NS = TestUtils.SAMPLE_NS;
    private static String TCNS = TestUtils.SAMPLE_NS + "test-manifest-sample-1.ttl#";
    private static String FNS = TestUtils.SAMPLE_NS + "test/";

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
        repository.setAssertor(iri(NS, "testharness"));
        repository.setTestSubject(iri(NS, "test"));
    }

    @Test
    void loadOneManifest() throws MalformedURLException {
        testSuiteDescription.load(List.of(
                new URL(NS + "test-manifest-sample-1.ttl"),
                new URL(NS + "specification-sample-1.ttl")
        ));
        assertTrue(ask(iri(NS, "specification1"), RDF.type, DOAP.Specification));
        assertTrue(ask(iri(NS, "test-manifest-sample-1.ttl#group1-feature1"),
                SPEC.requirementReference, iri(NS, "specification1#spec1")));
        assertFalse(ask(iri(NS, "specification2"), RDF.type, DOAP.Specification));
        assertFalse(ask(iri(NS, "specification2"), RDF.type, SPEC.Specification));
        assertFalse(ask(iri(NS, "test-manifest-sample-2.ttl#group4-feature1"),
                SPEC.requirementReference, iri(NS, "specification2#spec1")));
    }

    @Test
    void loadTwoManifest() throws MalformedURLException {
        testSuiteDescription.load(List.of(
                new URL(NS + "test-manifest-sample-1.ttl"),
                new URL(NS + "test-manifest-sample-2.ttl"),
                new URL(NS + "specification-sample-1.ttl"),
                new URL(NS + "specification-sample-2.ttl")
        ));
        assertTrue(ask(iri(NS, "specification1"), RDF.type, DOAP.Specification));
        assertTrue(ask(iri(NS, "test-manifest-sample-1.ttl#group1-feature1"),
                SPEC.requirementReference, iri(NS, "specification1#spec1")));
        assertTrue(ask(iri(NS, "specification2"), RDF.type, DOAP.Specification));
        assertTrue(ask(iri(NS, "test-manifest-sample-2.ttl#group4-feature1"),
                SPEC.requirementReference, iri(NS, "specification2#spec1")));
    }

    @Test
    void loadRdfa() throws MalformedURLException {
        testSuiteDescription.load(List.of(new URL(NS + "specification-sample-1.html")));
        assertTrue(ask(iri(NS, "specification1"), RDF.type, DOAP.Specification));
        assertTrue(ask(iri(NS, "specification1#spec1"), SPEC.statement,
                literal("text of requirement 1")));
    }

    @Test
    void loadTwoManifestMixed() throws MalformedURLException {
        testSuiteDescription.load(List.of(
                new URL(NS + "test-manifest-sample-1.ttl"),
                new URL(NS + "test-manifest-sample-2.ttl"),
                new URL(NS + "specification-sample-1.html"),
                new URL(NS + "specification-sample-2.ttl")
        ));
        assertTrue(ask(iri(NS, "specification1"), RDF.type, DOAP.Specification));
        assertTrue(ask(iri(NS, "test-manifest-sample-1.ttl#group1-feature1"),
                SPEC.requirementReference, iri(NS, "specification1#spec1")));
        assertTrue(ask(iri(NS, "specification2"), RDF.type, DOAP.Specification));
        assertTrue(ask(iri(NS, "test-manifest-sample-2.ttl#group4-feature1"),
                SPEC.requirementReference, iri(NS, "specification2#spec1")));
    }

    @Test
    void setNonRunningTestAssertionsNull() throws MalformedURLException {
        testSuiteDescription.load(List.of( new URL(NS + "test-manifest-sample-1.ttl")));
        testSuiteDescription.setNonRunningTestAssertions(null, null, null);
        assertEquals(6, count(null, RDF.type, TD.TestCase));
        assertEquals(3, count(null, RDF.type, EARL.Assertion));
        assertEquals(3, count(null, EARL.outcome, EARL.inapplicable));
        assertTrue(ask(iri(TCNS, "group1-feature1"), RDF.type, TD.TestCase));
        assertTrue(ask(iri(TCNS, "group1-feature2"), RDF.type, TD.TestCase));
        assertTrue(ask(iri(TCNS, "group1-feature3"), RDF.type, TD.TestCase));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature1")));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature2")));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature3")));
    }

    @Test
    void setNonRunningTestAssertionsEmptyFeatures() throws MalformedURLException {
        testSuiteDescription.load(List.of( new URL(NS + "test-manifest-sample-1.ttl")));
        testSuiteDescription.setNonRunningTestAssertions(Collections.emptySet(), null, null);
        assertEquals(6, count(null, RDF.type, TD.TestCase));
        assertEquals(3, count(null, RDF.type, EARL.Assertion));
        assertEquals(3, count(null, EARL.outcome, EARL.inapplicable));
        assertTrue(ask(iri(TCNS, "group1-feature1"), RDF.type, TD.TestCase));
        assertTrue(ask(iri(TCNS, "group1-feature2"), RDF.type, TD.TestCase));
        assertTrue(ask(iri(TCNS, "group1-feature3"), RDF.type, TD.TestCase));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature1")));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature2")));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature3")));
    }

    @Test
    void setNonRunningTestAssertionsEmptyFilters() throws MalformedURLException {
        testSuiteDescription.load(List.of( new URL(NS + "test-manifest-sample-1.ttl")));
        testSuiteDescription.setNonRunningTestAssertions(null, Collections.emptyList(), null);
        assertEquals(6, count(null, RDF.type, TD.TestCase));
        assertEquals(3, count(null, RDF.type, EARL.Assertion));
        assertEquals(3, count(null, EARL.outcome, EARL.inapplicable));
        assertTrue(ask(iri(TCNS, "group1-feature1"), RDF.type, TD.TestCase));
        assertTrue(ask(iri(TCNS, "group1-feature2"), RDF.type, TD.TestCase));
        assertTrue(ask(iri(TCNS, "group1-feature3"), RDF.type, TD.TestCase));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature1")));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature2")));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature3")));
    }

    @Test
    void setNonRunningTestAssertionsEmptyStatuses() throws MalformedURLException {
        testSuiteDescription.load(List.of( new URL(NS + "test-manifest-sample-1.ttl")));
        testSuiteDescription.setNonRunningTestAssertions(null, null, Collections.emptyList());
        assertEquals(6, count(null, RDF.type, TD.TestCase));
        assertEquals(3, count(null, RDF.type, EARL.Assertion));
        assertEquals(3, count(null, EARL.outcome, EARL.inapplicable));
        assertTrue(ask(iri(TCNS, "group1-feature1"), RDF.type, TD.TestCase));
        assertTrue(ask(iri(TCNS, "group1-feature2"), RDF.type, TD.TestCase));
        assertTrue(ask(iri(TCNS, "group1-feature3"), RDF.type, TD.TestCase));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature1")));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature2")));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature3")));
    }

    @Test
    void setNonRunningTestAssertionsOneFeature() throws MalformedURLException {
        testSuiteDescription.load(List.of( new URL(NS + "test-manifest-sample-1.ttl")));
        testSuiteDescription.setNonRunningTestAssertions(Set.of("sf1"), null, null);
        assertEquals(6, count(null, RDF.type, TD.TestCase));
        assertEquals(2, count(null, RDF.type, EARL.Assertion));
        assertEquals(2, count(null, EARL.outcome, EARL.inapplicable));
        assertTrue(ask(iri(TCNS, "group1-feature1"), RDF.type, TD.TestCase));
        assertTrue(ask(iri(TCNS, "group1-feature2"), RDF.type, TD.TestCase));
        assertTrue(ask(iri(TCNS, "group1-feature3"), RDF.type, TD.TestCase));
        assertTrue(ask(iri(TCNS, "group2-feature1"), RDF.type, TD.TestCase));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature1")));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature2")));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature3")));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group2-feature1")));
    }

    @Test
    void setNonRunningTestAssertionsFeature1And2() throws MalformedURLException {
        testSuiteDescription.load(List.of( new URL(NS + "test-manifest-sample-1.ttl")));
        testSuiteDescription.setNonRunningTestAssertions(Set.of("sf1",  "sf2"), null, null);
        assertEquals(6, count(null, RDF.type, TD.TestCase));
        assertEquals(0, count(null, RDF.type, EARL.Assertion));
        assertEquals(0, count(null, EARL.outcome, EARL.inapplicable));
    }

    @Test
    void setNonRunningTestAssertionsFeature1And3() throws MalformedURLException {
        testSuiteDescription.load(List.of( new URL(NS + "test-manifest-sample-1.ttl")));
        testSuiteDescription.setNonRunningTestAssertions(Set.of("sf1",  "sf3"), null, null);
        assertEquals(6, count(null, RDF.type, TD.TestCase));
        assertEquals(2, count(null, RDF.type, EARL.Assertion));
        assertEquals(2, count(null, EARL.outcome, EARL.inapplicable));
        assertTrue(ask(iri(TCNS, "group1-feature1"), RDF.type, TD.TestCase));
        assertTrue(ask(iri(TCNS, "group1-feature2"), RDF.type, TD.TestCase));
        assertTrue(ask(iri(TCNS, "group1-feature3"), RDF.type, TD.TestCase));
        assertTrue(ask(iri(TCNS, "group2-feature1"), RDF.type, TD.TestCase));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature1")));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature2")));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature3")));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group2-feature1")));
    }

    @Test
    void setNonRunningTestAssertionsNoFeaturesFilterGroup1() throws MalformedURLException {
        testSuiteDescription.load(List.of( new URL(NS + "test-manifest-sample-1.ttl")));
        testSuiteDescription.setNonRunningTestAssertions(null, List.of("group1"), null);
        assertEquals(6, count(null, RDF.type, TD.TestCase));
        assertEquals(3, count(null, RDF.type, EARL.Assertion));
        assertEquals(0, count(null, EARL.outcome, EARL.untested));
        assertEquals(3, count(null, EARL.outcome, EARL.inapplicable));
        assertTrue(ask(iri(TCNS, "group1-feature1"), RDF.type, TD.TestCase));
        assertTrue(ask(iri(TCNS, "group1-feature2"), RDF.type, TD.TestCase));
        assertTrue(ask(iri(TCNS, "group1-feature3"), RDF.type, TD.TestCase));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature1")));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature2")));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature3")));
    }

    @Test
    void setNonRunningTestAssertionsFilterGroup1() throws MalformedURLException {
        testSuiteDescription.load(List.of( new URL(NS + "test-manifest-sample-1.ttl")));
        testSuiteDescription.setNonRunningTestAssertions(Set.of("sf1", "sf2"), List.of("group1"), null);
        assertEquals(6, count(null, RDF.type, TD.TestCase));
        assertEquals(3, count(null, RDF.type, EARL.Assertion));
        assertEquals(3, count(null, EARL.outcome, EARL.untested));
        assertEquals(0, count(null, EARL.outcome, EARL.inapplicable));
        assertTrue(ask(iri(TCNS, "group1-feature1"), RDF.type, TD.TestCase));
        assertTrue(ask(iri(TCNS, "group1-feature2"), RDF.type, TD.TestCase));
        assertTrue(ask(iri(TCNS, "group1-feature3"), RDF.type, TD.TestCase));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature1")));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature2")));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature3")));
    }

    @Test
    void setNonRunningTestAssertionsStatuesAccepted() throws MalformedURLException {
        testSuiteDescription.load(List.of( new URL(NS + "test-manifest-sample-1.ttl")));
        testSuiteDescription.setNonRunningTestAssertions(Set.of("sf1",  "sf2"), null, List.of("accepted"));
        assertEquals(6, count(null, RDF.type, TD.TestCase));
        assertEquals(1, count(null, RDF.type, EARL.Assertion));
        assertEquals(1, count(null, EARL.outcome, EARL.untested));
        assertTrue(ask(iri(TCNS, "group1-feature1"), RDF.type, TD.TestCase));
        assertTrue(ask(iri(TCNS, "group1-feature2"), RDF.type, TD.TestCase));
        assertTrue(ask(iri(TCNS, "group2-feature1"), RDF.type, TD.TestCase));
        assertTrue(ask(iri(TCNS, "group3-feature1"), RDF.type, TD.TestCase));
        assertTrue(ask(iri(TCNS, "group3-feature2"), RDF.type, TD.TestCase));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature1")));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature2")));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group2-feature1")));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group3-feature1")));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group3-feature2")));
    }

    @Test
    void setNonRunningTestAssertionsStatusUnreviewed() throws MalformedURLException {
        testSuiteDescription.load(List.of( new URL(NS + "test-manifest-sample-1.ttl")));
        testSuiteDescription.setNonRunningTestAssertions(Set.of("sf1",  "sf2"), null, List.of("unreviewed"));
        assertEquals(6, count(null, RDF.type, TD.TestCase));
        assertEquals(5, count(null, RDF.type, EARL.Assertion));
        assertEquals(5, count(null, EARL.outcome, EARL.untested));
        assertTrue(ask(iri(TCNS, "group1-feature3"), RDF.type, TD.TestCase));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature3")));
    }

    @Test
    void setNonRunningTestAssertionsBadStatus() throws MalformedURLException {
        testSuiteDescription.load(List.of( new URL(NS + "test-manifest-sample-1.ttl")));
        add(iri(NS, "testcase"), RDF.type, TD.TestCase);
        add(iri(NS, "testcase"), TD.reviewStatus, literal("ACCEPTED"));
        testSuiteDescription.setNonRunningTestAssertions(Set.of("sf1",  "sf2"), null, List.of("unreviewed"));
        assertEquals(7, count(null, RDF.type, TD.TestCase));
        assertEquals(6, count(null, RDF.type, EARL.Assertion));
        assertEquals(6, count(null, EARL.outcome, EARL.untested));
        assertTrue(ask(iri(TCNS, "group1-feature3"), RDF.type, TD.TestCase));
        assertFalse(ask(null, EARL.test, iri(TCNS, "group1-feature3")));
    }

    @Test
    void setNonRunningTestAssertionsBadStatusNotChecked() throws MalformedURLException {
        testSuiteDescription.load(List.of( new URL(NS + "test-manifest-sample-1.ttl")));
        add(iri(NS, "testcase"), RDF.type, TD.TestCase);
        add(iri(NS, "testcase"), TD.reviewStatus, literal("ACCEPTED"));
        testSuiteDescription.setNonRunningTestAssertions(Set.of("sf1",  "sf2"), null, null);
        assertEquals(7, count(null, RDF.type, TD.TestCase));
        assertEquals(0, count(null, RDF.type, EARL.Assertion));
        assertEquals(0, count(null, EARL.outcome, EARL.untested));
    }

    @Test
    void setNonRunningTestAssertionsMissingStatus() throws MalformedURLException {
        testSuiteDescription.load(List.of( new URL(NS + "test-manifest-sample-1.ttl")));
        add(iri(NS, "testcase"), RDF.type, TD.TestCase);
        testSuiteDescription.setNonRunningTestAssertions(Set.of("sf1",  "sf2"), null,
                List.of("accepted", "unreviewed"));
        assertEquals(7, count(null, RDF.type, TD.TestCase));
        assertEquals(1, count(null, RDF.type, EARL.Assertion));
        assertEquals(1, count(null, EARL.outcome, EARL.untested));
        assertTrue(ask(iri(NS, "testcase"), RDF.type, TD.TestCase));
        assertTrue(ask(null, EARL.test, iri(NS, "testcase")));
    }

    @Test
    void mapEmptyList() {
        assertDoesNotThrow(() -> testSuiteDescription.prepareTestCases(Config.RunMode.TEST));
    }

    @Test
    void mapRemoteFeaturePaths() {
        add(iri(NS, "testcase"), RDF.type, TD.TestCase);
        add(iri(NS, "testcase"), SPEC.testScript, iri("https://remote.org/remote-test"));
        assertThrows(TestHarnessInitializationException.class,
                () -> testSuiteDescription.prepareTestCases(Config.RunMode.TEST));
    }

    @Test
    void mapMissingFeaturePaths() {
        add(iri(NS, "testcase"), RDF.type, TD.TestCase);
        add(iri(NS, "testcase"), SPEC.testScript, iri(NS, "features/unknown.feature"));
        testSuiteDescription.prepareTestCases(Config.RunMode.TEST);
        assertTrue(testSuiteDescription.getFeaturePaths().isEmpty());
    }

    @Test
    void mapMissingFeature() {
        add(iri(NS, "testcase"), RDF.type, TD.TestCase);
        testSuiteDescription.prepareTestCases(Config.RunMode.TEST);
        assertTrue(testSuiteDescription.getFeaturePaths().isEmpty());
    }

    @Test
    void prepareTestCases() {
        add(iri(NS, "testcase"), RDF.type, TD.TestCase);
        add(iri(NS, "testcase"), SPEC.testScript, iri(NS, "features/test.feature"));
        testSuiteDescription.prepareTestCases(Config.RunMode.TEST);
        final List<String> paths = testSuiteDescription.getFeaturePaths();
        final String[] expected = new String[]{TestUtils.getPathUri("src/test/resources/test.feature").toString()};
        assertThat("Locations match", paths, containsInAnyOrder(expected));
    }

    @Test
    void prepareTestCasesWithAssertionInTestMode() {
        add(iri(NS, "testcase"), RDF.type, TD.TestCase);
        add(iri(NS, "testcase"), SPEC.testScript, iri(NS, "features/test.feature"));
        add(iri(NS, "assertion"), RDF.type, EARL.Assertion);
        add(iri(NS, "assertion"), EARL.test, iri(NS, "testcase"));
        testSuiteDescription.prepareTestCases(Config.RunMode.TEST);
        assertTrue(testSuiteDescription.getFeaturePaths().isEmpty());
    }

    @Test
    void prepareTestCasesWithAssertionInCoverageMode() {
        add(iri(NS, "testcase"), RDF.type, TD.TestCase);
        add(iri(NS, "testcase"), SPEC.testScript, iri(NS, "features/test.feature"));
        add(iri(NS, "assertion"), RDF.type, EARL.Assertion);
        add(iri(NS, "assertion"), EARL.test, iri(NS, "testcase"));
        testSuiteDescription.prepareTestCases(Config.RunMode.COVERAGE);
        assertTrue(testSuiteDescription.getFeaturePaths().isEmpty());
    }

    @Test
    void prepareTestCasesNotImplemented() {
        add(iri(NS, "testcase"), RDF.type, TD.TestCase);
        add(iri(NS, "testcase"), SPEC.testScript, literal("test.feature"));
        testSuiteDescription.prepareTestCases(Config.RunMode.COVERAGE);
        assertTrue(testSuiteDescription.getFeaturePaths().isEmpty());
    }

    @Test
    void prepareTestCasesTitle() {
        add(iri(NS, "group1-feature1"), RDF.type, TD.TestCase);
        add(iri(NS, "group1-feature1"), SPEC.testScript,
                iri(FNS, "group1/feature1"));
        add(iri(NS, "group1-feature2"), RDF.type, TD.TestCase);
        add(iri(NS, "group1-feature2"), SPEC.testScript,
                iri(FNS, "group1/feature2"));
        add(iri(NS, "group2-feature1"), RDF.type, TD.TestCase);
        add(iri(NS, "group2-feature1"), SPEC.testScript,
                iri(FNS, "group2/feature1"));
        add(iri(NS, "group2-feature2"), RDF.type, TD.TestCase);
        add(iri(NS, "group2-feature2"), SPEC.testScript,
                iri(FNS, "group2/"));
        testSuiteDescription.prepareTestCases(Config.RunMode.COVERAGE);
        assertTrue(ask(iri(NS, "group1-feature1"), DCTERMS.title, literal("Feature 1 title")));
        assertTrue(ask(iri(NS, "group1-feature2"), DCTERMS.title, literal("Feature 2 title")));
        assertEquals(0, count(iri(NS, "group2-feature1"), DCTERMS.title, null));
    }

    @Test
    void getTestCases() {
        add(iri(NS, "testcase"), RDF.type, TD.TestCase);
        add(iri(NS, "testcase"), SPEC.testScript, iri(NS, "features/test.feature"));
        add(iri(NS, "assertion"), RDF.type, EARL.Assertion);
        add(iri(NS, "assertion"), EARL.test, iri(NS, "testcase"));
        final List<IRI> testCases = testSuiteDescription.getTestCases(false);
        final IRI[] expected = new IRI[]{iri(NS, "testcase")};
        assertThat("TestCases match", testCases, containsInAnyOrder(expected));
    }

    @Test
    void getTestCasesFiltered() {
        add(iri(NS, "testcase"), RDF.type, TD.TestCase);
        add(iri(NS, "testcase"), SPEC.testScript, iri(NS, "features/test.feature"));
        add(iri(NS, "assertion"), RDF.type, EARL.Assertion);
        add(iri(NS, "assertion"), EARL.test, iri(NS, "testcase"));
        assertTrue(testSuiteDescription.getTestCases(true).isEmpty());
    }

    @Test
    void getTestCasesFiltered2() {
        add(iri(NS, "testcase"), RDF.type, TD.TestCase);
        add(iri(NS, "testcase"), SPEC.testScript, iri(NS, "features/test.feature"));
        final List<IRI> testCases = testSuiteDescription.getTestCases(true);
        final IRI[] expected = new IRI[]{iri(NS, "testcase")};
        assertThat("TestCases match", testCases, containsInAnyOrder(expected));
    }

    @Test
    void getTestsVersion() {
        TestSuiteDescription.TEST_VERSION = iri(NS, "features/discovery/test-version.txt");
        testSuiteDescription.getTestsVersion();
        assertTrue(ask(iri(Namespaces.SPECIFICATION_TESTS_IRI), RDF.type, DOAP.Project));
        assertTrue(ask(iri(Namespaces.SPECIFICATION_TESTS_IRI), DOAP.created,
                literal("2021-11-15T00:00:00Z", XSD.DATETIME)));
        assertTrue(ask(iri(Namespaces.RESULTS_URI, "tests-release"), DOAP.revision, literal("1.0.0")));
    }

    @Test
    void getTestsVersionPartial() {
        TestSuiteDescription.TEST_VERSION = iri(NS, "features/discovery/test-version-partial.txt");
        testSuiteDescription.getTestsVersion();
        assertTrue(ask(iri(Namespaces.SPECIFICATION_TESTS_IRI), RDF.type, DOAP.Project));
        assertFalse(ask(iri(Namespaces.SPECIFICATION_TESTS_IRI), DOAP.created, null));
        assertTrue(ask(iri(Namespaces.RESULTS_URI, "tests-release"), DOAP.revision, literal("1.0.1")));
    }

    @Test
    void getTestsVersionNoVersion() {
        TestSuiteDescription.TEST_VERSION = iri(NS, "features/discovery/test-version-empty.txt");
        testSuiteDescription.getTestsVersion();
        assertTrue(ask(iri(Namespaces.SPECIFICATION_TESTS_IRI), RDF.type, DOAP.Project));
        assertFalse(ask(iri(Namespaces.SPECIFICATION_TESTS_IRI), DOAP.created, null));
        assertTrue(ask(iri(Namespaces.RESULTS_URI, "tests-release"), DOAP.revision, literal("unknown")));
    }

    @Test
    void getTestsVersionMissing() {
        TestSuiteDescription.TEST_VERSION = iri(NS, "features/discovery/test-version-missing.txt");
        testSuiteDescription.getTestsVersion();
        assertTrue(ask(iri(Namespaces.SPECIFICATION_TESTS_IRI), RDF.type, DOAP.Project));
        assertFalse(ask(iri(Namespaces.SPECIFICATION_TESTS_IRI), DOAP.created, null));
        assertTrue(ask(iri(Namespaces.RESULTS_URI, "tests-release"), DOAP.revision, literal("unknown")));
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
        try (
                RepositoryConnection conn = repository.getConnection();
                RepositoryResult<Statement> statements = conn.getStatements(subject, predicate, object, false)
        ) {
            return statements.stream().count();
        }
    }
}
