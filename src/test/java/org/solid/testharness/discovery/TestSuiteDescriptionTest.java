package org.solid.testharness.discovery;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.TestHarnessInitializationException;
import org.solid.testharness.utils.TestUtils;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class TestSuiteDescriptionTest {
    @Inject
    DataRepository repository;
    @Inject
    TestSuiteDescription testSuiteDescription;

    @BeforeEach
    void setUp() {
        try (RepositoryConnection conn = repository.getConnection()) {
            conn.clear();
        }
    }

    @Test
    void load() throws MalformedURLException {
        testSuiteDescription.load(TestUtils.getFileUrl("src/test/resources/testsuite-sample.ttl"));
        try (RepositoryConnection conn = repository.getConnection()) {
            assertFalse(conn.isEmpty());
        }
    }

    @Test
    void getSupportedTestCasesNullFeatures() throws MalformedURLException {
        testSuiteDescription.load(TestUtils.getFileUrl("src/test/resources/testsuite-sample.ttl"));
        final List<IRI> testCases = testSuiteDescription.getSupportedTestCases(null);
        final IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3");
        assertThat("Group 1 matches", testCases, containsInAnyOrder(expected));
    }

    @Test
    void getSupportedTestCasesEmptyFeatures() throws MalformedURLException {
        testSuiteDescription.load(TestUtils.getFileUrl("src/test/resources/testsuite-sample.ttl"));
        final List<IRI> testCases = testSuiteDescription.getSupportedTestCases(Set.of());
        final IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3");
        assertThat("Groups 1 matches", testCases, containsInAnyOrder(expected));
    }

    @Test
    void getSupportedTestCasesOneFeature() throws MalformedURLException {
        testSuiteDescription.load(TestUtils.getFileUrl("src/test/resources/testsuite-sample.ttl"));
        final List<IRI> testCases = testSuiteDescription.getSupportedTestCases(Set.of("sf1"));
        final IRI[] expected = createIriList(
                "group1/feature1", "group1/feature2", "group1/feature3", "group2/feature1"
        );
        assertThat("Groups 1, 2 match", testCases, containsInAnyOrder(expected));
    }

    @Test
    void getSupportedTestCasesFeature1And2() throws MalformedURLException {
        testSuiteDescription.load(TestUtils.getFileUrl("src/test/resources/testsuite-sample.ttl"));
        final List<IRI> testCases = testSuiteDescription.getSupportedTestCases(Set.of("sf1", "sf2"));
        final IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3",
                "group2/feature1", "group3/feature1", "group3/feature2");
        assertThat("Groups 1, 2, 3 match", testCases, containsInAnyOrder(expected));
    }

    @Test
    void getSupportedTestCasesFeature1And3() throws MalformedURLException {
        testSuiteDescription.load(TestUtils.getFileUrl("src/test/resources/testsuite-sample.ttl"));
        final List<IRI> testCases = testSuiteDescription.getSupportedTestCases(Set.of("sf1", "sf3"));
        final IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3",
                "group2/feature1", "group4/feature1", "group4/feature2", "group4/feature3");
        assertThat("Groups 1, 2, 4 match", testCases, containsInAnyOrder(expected));
    }

    @Test
    void getSupportedTestCasesAllFeatures() throws MalformedURLException {
        testSuiteDescription.load(TestUtils.getFileUrl("src/test/resources/testsuite-sample.ttl"));
        final List<IRI> testCases = testSuiteDescription.getSupportedTestCases(Set.of("sf1", "sf2", "sf3"));
        final IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3",
                "group2/feature1", "group3/feature1", "group3/feature2",
                "group4/feature1", "group4/feature2", "group4/feature3"
        );
        assertThat("All groups match", testCases, containsInAnyOrder(expected));
    }

    @Test
    void getSupportedTestCasesFeature3() throws MalformedURLException {
        testSuiteDescription.load(TestUtils.getFileUrl("src/test/resources/testsuite-sample.ttl"));
        final List<IRI> testCases = testSuiteDescription.getSupportedTestCases(Set.of("sf3"));
        final IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3");
        assertThat("Group 1 matches", testCases, containsInAnyOrder(expected));
    }

    @Test
    void mapRemoteFeaturePaths() {
        final List<IRI> testCases = List.of(iri("https://example.org/dummy/group3/feature1"));
        assertThrows(TestHarnessInitializationException.class, () -> testSuiteDescription.locateTestCases(testCases));
    }

    @Test
    void mapFeaturePaths() throws MalformedURLException {
        final List<IRI> testCases = List.of(iri("https://example.org/features/test.feature"));
        final List<String> paths = testSuiteDescription.locateTestCases(testCases);
        final String[] expected = new String[]{TestUtils.getPathUri("src/test/resources/test.feature").toString()};
        assertThat("Locations match", paths, containsInAnyOrder(expected));
    }

    @Test
    void mapMissingFeaturePaths() throws MalformedURLException {
        final List<IRI> testCases = List.of(
                iri("https://example.org/dummy/group1/feature1"), iri("https://example.org/dummy/group1/feature2"),
                iri("https://example.org/dummy/group2/feature1")
        );
        final List<String> paths = testSuiteDescription.locateTestCases(testCases);
        final String[] expected = new String[]{TestUtils.getPathUri("src/test/resources/dummy-features/group1/feature1")
                .toString(),
                TestUtils.getPathUri("src/test/resources/dummy-features/group1/feature2").toString(),
                TestUtils.getPathUri("src/test/resources/dummy-features/otherExample/feature1").toString()
        };
        assertThat("Locations match", paths, containsInAnyOrder(expected));
        // TODO: assert changes in repository
    }

    private IRI[] createIriList(final String... testCases) {
        return Stream.of(testCases).map(s -> iri("https://example.org/dummy/" + s)).toArray(IRI[]::new);
    }
}
