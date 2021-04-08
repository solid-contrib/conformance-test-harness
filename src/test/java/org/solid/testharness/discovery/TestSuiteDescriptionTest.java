package org.solid.testharness.discovery;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.config.PathMappings;
import org.solid.testharness.utils.DataRepository;

import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertFalse;

@QuarkusTest
class TestSuiteDescriptionTest {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.discovery.TestSuiteDescriptionTest");

    @Inject
    DataRepository repository;

    @BeforeEach
    void setUp() {
        try (RepositoryConnection conn = repository.getConnection()) {
            conn.clear();
        }
    }

    @Test
    void load() throws FileNotFoundException {
        TestSuiteDescription suite = new TestSuiteDescription();
        suite.load(new FileReader(("src/test/resources/testsuite-sample.ttl")));
        try (RepositoryConnection conn = repository.getConnection()) {
            assertFalse(conn.isEmpty());
        }
    }

    @Test
    void filterSupportedTestCasesNullFeatures() throws FileNotFoundException {
        TestSuiteDescription suite = new TestSuiteDescription();
        suite.load(new FileReader(("src/test/resources/testsuite-sample.ttl")));
        List<IRI> features = suite.filterSupportedTestCases(null);
        IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3");
        assertThat("Group 1 matches", features, containsInAnyOrder(expected));
    }

    @Test
    void filterSupportedTestCasesEmptyFeatures() throws FileNotFoundException {
        TestSuiteDescription suite = new TestSuiteDescription();
        suite.load(new FileReader(("src/test/resources/testsuite-sample.ttl")));
        List<IRI> features = suite.filterSupportedTestCases(Set.of());
        IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3");
        assertThat("Groups 1 matches", features, containsInAnyOrder(expected));
    }

    @Test
    void filterSupportedTestCasesOneFeature() throws FileNotFoundException {
        TestSuiteDescription suite = new TestSuiteDescription();
        suite.load(new FileReader(("src/test/resources/testsuite-sample.ttl")));
        List<IRI> features = suite.filterSupportedTestCases(Set.of("sf1"));
        IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3", "group2/feature1");
        assertThat("Groups 1, 2 match", features, containsInAnyOrder(expected));
    }

    @Test
    void filterSupportedTestCasesFeature1And2() throws FileNotFoundException {
        TestSuiteDescription suite = new TestSuiteDescription();
        suite.load(new FileReader(("src/test/resources/testsuite-sample.ttl")));
        List<IRI> features = suite.filterSupportedTestCases(Set.of("sf1", "sf2"));
        IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3",
                "group2/feature1", "group3/feature1", "group3/feature2");
        assertThat("Groups 1, 2, 3 match", features, containsInAnyOrder(expected));
    }

    @Test
    void filterSupportedTestCasesFeature1And3() throws FileNotFoundException {
        TestSuiteDescription suite = new TestSuiteDescription();
        suite.load(new FileReader(("src/test/resources/testsuite-sample.ttl")));
        List<IRI> features = suite.filterSupportedTestCases(Set.of("sf1", "sf3"));
        IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3",
                "group2/feature1", "group4/feature1", "group4/feature2", "group4/feature3");
        assertThat("Groups 1, 2, 4 match", features, containsInAnyOrder(expected));
    }

    @Test
    void filterSupportedTestCasesAllFeatures() throws FileNotFoundException {
        TestSuiteDescription suite = new TestSuiteDescription();
        suite.load(new FileReader(("src/test/resources/testsuite-sample.ttl")));
        List<IRI> features = suite.filterSupportedTestCases(Set.of("sf1", "sf2", "sf3"));
        IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3",
                "group2/feature1", "group3/feature1", "group3/feature2",
                "group4/feature1", "group4/feature2", "group4/feature3"
        );
        assertThat("All groups match", features, containsInAnyOrder(expected));
    }

    @Test
    void filterSupportedTestCasesFeature3() throws FileNotFoundException {
        TestSuiteDescription suite = new TestSuiteDescription();
        suite.load(new FileReader(("src/test/resources/testsuite-sample.ttl")));
        List<IRI> features = suite.filterSupportedTestCases(Set.of("sf3"));
        IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3");
        assertThat("Group 1 matches", features, containsInAnyOrder(expected));
    }

    @Test
    void mapFeaturePaths() {
        TestSuiteDescription suite = new TestSuiteDescription();
        suite.testCases = List.of(
                iri("https://example.org/group1/feature1"), iri("https://example.org/group1/feature2"),
                iri("https://example.org/group2/feature1"), iri("https://example.org/group3/feature1")
        );
        List<PathMappings.Mapping> mappings = List.of(
                PathMappings.Mapping.create("https://example.org/group1", "src/test/resources/dummy-features/group1"),
                PathMappings.Mapping.create("https://example.org/group2", "src/test/resources/dummy-features/otherExample")
        );
        List<String> paths = suite.locateTestCases(mappings);
        String[] expected = new String[]{"src/test/resources/dummy-features/group1/feature1", "src/test/resources/dummy-features/group1/feature2",
                "src/test/resources/dummy-features/otherExample/feature1", "https://example.org/group3/feature1"};
        assertThat("Locations match", paths, containsInAnyOrder(expected));
    }

    private IRI[] createIriList(String... testCases) {
        return Stream.of(testCases).map(s -> iri("https://example.org/" + s)).toArray(IRI[]::new);
    }
}
