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
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Representation of a test suite description document parsed from RDF
 */
@ApplicationScoped
public class TestSuiteDescription {
    private static final Logger logger = LoggerFactory.getLogger(TestSuiteDescription.class);

    @Inject
    DataRepository dataRepository;

    // TODO: This currently finds all td:SpecificationTestCase. It should pay attention to:
    //   manifest: dcterms:hasPart manifest:testgroup
    // This will give the developer a simple way to control which tests groups are run, either by changing the file
    // or specifying on the comment line. It may also help if loading >1 test suite description
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

    /**
     * Load data from the URL
     * @param url starting point for discovering tests
     */
    public void load(URL url) {
        // TODO: Search for linked test suite documents or specifications and load data from them as well
        dataRepository.loadTurtle(url);
    }

    /**
     * This searches the whole dataRepository for supported test cases based on the server capabilities
     * @param serverFeatures set of supported features
     * @return List of features
     */
    public List<IRI> getSupportedTestCases(Set<String> serverFeatures) {
        String serverFeatureList = serverFeatures != null && !serverFeatures.isEmpty() ? "\"" + String.join("\", \"", serverFeatures) + "\"" : "";
        return selectTestCases(String.format(SELECT_SUPPORTED_TEST_CASES, serverFeatureList));
    }

    public List<IRI> getAllTestCases() {
        return selectTestCases(SELECT_ALL_TEST_CASES);
    }

    public List<String> locateTestCases(List<IRI> testCases, List<PathMappings.Mapping> pathMappings) {
        if (testCases.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        Stream<String> testCaseStream;
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            if (pathMappings == null || pathMappings.isEmpty()) {
                testCaseStream = testCases.stream().map(Value::stringValue);
            } else {
                testCaseStream = testCases.stream().map(t -> {
                    String location = t.stringValue();
                    PathMappings.Mapping mapping = pathMappings.stream().filter(m -> location.startsWith(m.prefix)).findFirst().orElse(null);
                    if (mapping != null) {
                        String mappedLocation = location.replace(mapping.prefix, mapping.path);
                        File file = new File(mappedLocation);
                        if (!file.exists()) {
                            // TODO: if starter feature files are auto-generated, read for @ignore as well
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
            throw new TestHarnessInitializationException(e.toString());
        }
    }

    private List<IRI> selectTestCases(String query) {
        List<IRI> testCases = new ArrayList<>();
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            TupleQuery tupleQuery = conn.prepareTupleQuery(query);
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();
                    Value featureIri = bindingSet.getValue("feature");
                    testCases.add((IRI)featureIri);
                }
            }
        } catch (RDF4JException e) {
            throw new TestHarnessInitializationException(e.toString());
        }
        return testCases;
    }
}
