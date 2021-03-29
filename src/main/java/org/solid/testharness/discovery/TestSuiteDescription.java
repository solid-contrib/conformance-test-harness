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
import org.solid.common.vocab.TD;
import org.solid.testharness.config.PathMappings;
import org.solid.testharness.utils.DataRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Representation of a test suite description document parsed from RDF
 */
public class TestSuiteDescription {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.discovery.TestSuiteDescription");

    DataRepository repository;
    List<IRI> testCases = new ArrayList<>();

    public TestSuiteDescription(DataRepository repository) {
        this.repository = repository;
    }

    private static final String SELECT_TEST_CASES =
            "PREFIX " + TD.PREFIX + ": <" + TD.NAMESPACE + ">\n" +
            "PREFIX " + DCTERMS.PREFIX + ": <" + DCTERMS.NAMESPACE + ">\n" +
            "SELECT DISTINCT ?feature WHERE {" +
            "  ?group a td:SpecificationTestCase ." +
            "  ?group dcterms:hasPart ?feature ." +
            "  ?feature a td:TestCase ." +
            "  FILTER NOT EXISTS { ?group td:preCondition ?precondition FILTER(?precondition NOT IN (%s))}" +
            " } ";

    public void load(File file) {
        repository.loadTurtle(file);
    }

    public List<IRI> getSuitableTestCases(Set<String> serverFeatures) {
        try (RepositoryConnection conn = repository.getConnection()) {
            String serverFeatureList = serverFeatures != null && !serverFeatures.isEmpty() ? "\"" + String.join("\", \"", serverFeatures) + "\"" : "";
            TupleQuery tupleQuery = conn.prepareTupleQuery(String.format(SELECT_TEST_CASES, serverFeatureList));
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
        return testCases;
    }

    public List<String> locateTestCases(List<PathMappings.Mapping> pathMappings) {
        if (pathMappings == null || pathMappings.isEmpty()) {
            return testCases.stream().map(t -> t.stringValue()).collect(Collectors.toList());
        } else {
            return testCases.stream().map(t -> {
                String iri = t.stringValue();
                PathMappings.Mapping mapping = pathMappings.stream().filter(m -> iri.startsWith(m.prefix)).findFirst().orElse(null);
                if (mapping != null) {
                    return iri.replace(mapping.prefix, mapping.path);
                } else {
                    return iri;
                }
            }).collect(Collectors.toList());
        }
    }
}
