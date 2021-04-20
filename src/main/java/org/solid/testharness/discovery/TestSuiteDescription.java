package org.solid.testharness.discovery;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.DCTERMS;
import org.solid.common.vocab.EARL;
import org.solid.common.vocab.TD;
import org.solid.testharness.config.PathMappings;
import org.solid.testharness.utils.DataRepository;

import javax.enterprise.inject.spi.CDI;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Representation of a test suite description document parsed from RDF
 */
public class TestSuiteDescription {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.discovery.TestSuiteDescription");

    DataRepository repository;
    List<IRI> testCases = new ArrayList<>();

    public TestSuiteDescription() {
        this.repository = CDI.current().select(DataRepository.class).get();;
    }

    private static final String SELECT_SUPPORTED_TEST_CASES =
            "PREFIX " + TD.PREFIX + ": <" + TD.NAMESPACE + ">\n" +
            "PREFIX " + DCTERMS.PREFIX + ": <" + DCTERMS.NAMESPACE + ">\n" +
            "SELECT DISTINCT ?feature WHERE {" +
            "  ?group a td:SpecificationTestCase ." +
            "  ?group dcterms:hasPart ?feature ." +
            "  ?feature a td:TestCase ." +
            "  FILTER NOT EXISTS { ?group td:preCondition ?precondition FILTER(?precondition NOT IN (%s))}" +
            "} ";

    private static final String SELECT_ALL_TEST_CASES =
            "PREFIX " + TD.PREFIX + ": <" + TD.NAMESPACE + ">\n" +
            "PREFIX " + DCTERMS.PREFIX + ": <" + DCTERMS.NAMESPACE + ">\n" +
            "SELECT DISTINCT ?feature WHERE {" +
            "  ?group a td:SpecificationTestCase ." +
            "  ?group dcterms:hasPart ?feature ." +
            "  ?feature a td:TestCase ." +
            "} ";

    public void load(URL url) {
        repository.loadTurtle(url);
    }

    public List<IRI> filterSupportedTestCases(Set<String> serverFeatures) {
        String serverFeatureList = serverFeatures != null && !serverFeatures.isEmpty() ? "\"" + String.join("\", \"", serverFeatures) + "\"" : "";
        selectTestCases(String.format(SELECT_SUPPORTED_TEST_CASES, serverFeatureList));
        return testCases;
    }

    public List<String> locateTestCases(List<PathMappings.Mapping> pathMappings) {
        if (testCases.isEmpty()) {
            selectTestCases(SELECT_ALL_TEST_CASES);
        }
        Stream<String> testCaseStream;
        try (RepositoryConnection conn = repository.getConnection()) {
            if (pathMappings == null || pathMappings.isEmpty()) {
                testCaseStream = testCases.stream().map(t -> t.stringValue());
            } else {
                testCaseStream = testCases.stream().map(t -> {
                    String location = t.stringValue();
                    PathMappings.Mapping mapping = pathMappings.stream().filter(m -> location.startsWith(m.prefix)).findFirst().orElse(null);
                    if (mapping != null) {
                        String mappedLocation = location.replace(mapping.prefix, mapping.path);
                        File file = new File(mappedLocation);
                        if (!file.exists()) {
                            logger.warn("FEATURE NOT IMPLEMENTED: {}", mappedLocation);
                            conn.add(t, EARL.mode, EARL.untested);
                            return null;
                        } else {
                            return mappedLocation;
                        }
                    } else {
                        return location;
                    }
                }).filter(Objects::nonNull);
            }
            return testCaseStream.collect(Collectors.toList());
        } catch (RDF4JException e) {
            logger.error("Failed to setup namespaces", e);
        }
        return Collections.EMPTY_LIST;
    }

    private void selectTestCases(String query) {
        try (RepositoryConnection conn = repository.getConnection()) {
            TupleQuery tupleQuery = conn.prepareTupleQuery(query);
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    Value featureIri = bindingSet.getValue("feature");
                    testCases.add((IRI)featureIri);
                }
            }
        } catch (RDF4JException e) {
            logger.error("Failed to setup namespaces", e);
        }
    }
}
