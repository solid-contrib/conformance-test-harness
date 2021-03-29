package org.solid.testharness.discovery;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.config.PathMappings;
import org.solid.testharness.utils.DataRepository;

import javax.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;

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
    void load() {
        TestSuiteDescription suite = new TestSuiteDescription(repository);
        suite.load(new File("src/test/resources/testsuite-sample.ttl"));
        try (RepositoryConnection conn = repository.getConnection()) {
            assertFalse(conn.isEmpty());
        }
    }

    @Test
    void getSuitableTestCasesNullFeatures() {
        TestSuiteDescription suite = new TestSuiteDescription(repository);
        suite.load(new File("src/test/resources/testsuite-sample.ttl"));
        List<IRI> features = suite.getSuitableTestCases(null);
        IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3");
        assertThat("Group 1 matches", features, containsInAnyOrder(expected));
    }

    @Test
    void getSuitableTestCasesEmptyFeatures() {
        TestSuiteDescription suite = new TestSuiteDescription(repository);
        suite.load(new File("src/test/resources/testsuite-sample.ttl"));
        List<IRI> features = suite.getSuitableTestCases(Set.of());
        IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3");
        assertThat("Groups 1 matches", features, containsInAnyOrder(expected));
    }

    @Test
    void getSuitableTestCasesOneFeature() {
        TestSuiteDescription suite = new TestSuiteDescription(repository);
        suite.load(new File("src/test/resources/testsuite-sample.ttl"));
        List<IRI> features = suite.getSuitableTestCases(Set.of("sf1"));
        IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3", "group2/feature1");
        assertThat("Groups 1, 2 match", features, containsInAnyOrder(expected));
    }

    @Test
    void getSuitableTestCasesFeature1And2() {
        TestSuiteDescription suite = new TestSuiteDescription(repository);
        suite.load(new File("src/test/resources/testsuite-sample.ttl"));
        List<IRI> features = suite.getSuitableTestCases(Set.of("sf1", "sf2"));
        IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3",
                "group2/feature1", "group3/feature1", "group3/feature2");
        assertThat("Groups 1, 2, 3 match", features, containsInAnyOrder(expected));
    }

    @Test
    void getSuitableTestCasesFeature1And3() {
        TestSuiteDescription suite = new TestSuiteDescription(repository);
        suite.load(new File("src/test/resources/testsuite-sample.ttl"));
        List<IRI> features = suite.getSuitableTestCases(Set.of("sf1", "sf3"));
        IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3",
                "group2/feature1", "group4/feature1", "group4/feature2", "group4/feature3");
        assertThat("Groups 1, 2, 4 match", features, containsInAnyOrder(expected));
    }

    @Test
    void getSuitableTestCasesAllFeatures() {
        TestSuiteDescription suite = new TestSuiteDescription(repository);
        suite.load(new File("src/test/resources/testsuite-sample.ttl"));
        List<IRI> features = suite.getSuitableTestCases(Set.of("sf1", "sf2", "sf3"));
        IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3",
                "group2/feature1", "group3/feature1", "group3/feature2",
                "group4/feature1", "group4/feature2", "group4/feature3"
        );
        assertThat("All groups match", features, containsInAnyOrder(expected));
    }

    @Test
    void getSuitableTestCasesFeature3() {
        TestSuiteDescription suite = new TestSuiteDescription(repository);
        suite.load(new File("src/test/resources/testsuite-sample.ttl"));
        List<IRI> features = suite.getSuitableTestCases(Set.of("sf3"));
        IRI[] expected = createIriList("group1/feature1", "group1/feature2", "group1/feature3");
        assertThat("Group 1 matches", features, containsInAnyOrder(expected));
    }

    @Test
    void convertList() {
        TestSuiteDescription suite = new TestSuiteDescription(null);
        suite.testCases = List.of(
                iri("https://example.org/group1/feature1"), iri("https://example.org/group1/feature2"),
                iri("https://example.org/group2/feature1"), iri("https://example.org/group3/feature1")
        );
        List<PathMappings.Mapping> mappings = List.of(
                PathMappings.Mapping.create("https://example.org/group1", "example/group1"),
                PathMappings.Mapping.create("https://example.org/group2", "otherExample")
        );
        List<String> paths = suite.locateTestCases(mappings);
        String[] expected = new String[]{"example/group1/feature1", "example/group1/feature2", "otherExample/feature1", "https://example.org/group3/feature1"};
        assertThat("Locations match", paths, containsInAnyOrder(expected));
    }

    private IRI[] createIriList(String... testCases) {
        return Stream.of(testCases).map(s -> iri("https://example.org/" + s)).toArray(IRI[]::new);
    }
}
