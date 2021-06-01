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
import org.solid.testharness.http.HttpUtils;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Representation of a test suite description document parsed from RDF.
 */
@ApplicationScoped
public class TestSuiteDescription {
    private static final Logger logger = LoggerFactory.getLogger(TestSuiteDescription.class);

    @Inject
    DataRepository dataRepository;

    @Inject
    PathMappings pathMappings;

    // TODO: This currently finds all td:SpecificationTestCase. It should pay attention to:
    //   manifest: dcterms:hasPart manifest:testgroup
    // This will give the developer a simple way to control which tests groups are run, either by changing the file
    // or specifying on the comment line. It may also help if loading >1 test suite description
    private static final String PREFIXES = String.format("PREFIX %s: <%s>\nPREFIX %s: <%s>\n",
            TD.PREFIX, TD.NAMESPACE, DCTERMS.PREFIX, DCTERMS.NAMESPACE);
    private static final String SELECT_SUPPORTED_TEST_CASES = PREFIXES +
            "SELECT DISTINCT ?feature WHERE {" +
            "  ?group a td:SpecificationTestCase ." +
            "  ?group dcterms:hasPart ?feature ." +
            "  ?feature a td:TestCase ." +
            "  FILTER NOT EXISTS { ?group td:preCondition ?precondition FILTER(?precondition NOT IN (%s))}" +
            "} ";

    private static final String SELECT_ALL_TEST_CASES = PREFIXES +
            "SELECT DISTINCT ?feature WHERE {" +
            "  ?group a td:SpecificationTestCase ." +
            "  ?group dcterms:hasPart ?feature ." +
            "  ?feature a td:TestCase ." +
            "} ";

    /**
     * Load data from the URL.
     * @param url starting point for discovering tests
     */
    public void load(final URL url) {
        // TODO: Search for linked test suite documents or specifications and load data from them as well
        dataRepository.loadTurtle(url);
    }

    /**
     * This searches the whole dataRepository for supported test cases based on the server capabilities.
     * @param serverFeatures set of supported features
     * @return List of features
     */
    public List<IRI> getSupportedTestCases(final Set<String> serverFeatures) {
        final String serverFeatureList = serverFeatures != null && !serverFeatures.isEmpty()
                ? "\"" + String.join("\", \"", serverFeatures) + "\""
                : "";
        return selectTestCases(String.format(SELECT_SUPPORTED_TEST_CASES, serverFeatureList));
    }

    public List<IRI> getAllTestCases() {
        return selectTestCases(SELECT_ALL_TEST_CASES);
    }

    public List<String> locateTestCases(final List<IRI> testCases) {
        if (testCases.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            return testCases.stream().map(t -> {
                final URI mappedLocation = pathMappings.mapFeatureIri(t);
                if (HttpUtils.isHttpProtocol(mappedLocation.getScheme())) {
                    throw new TestHarnessInitializationException("Remote test cases are not yet supported - use " +
                            "mappings to point to local copies");
                }
                final File file = new File(mappedLocation.getPath());
                if (!file.exists()) {
                    // TODO: if starter feature files are auto-generated, read for @ignore as well
                    logger.warn("FEATURE NOT IMPLEMENTED: {}", mappedLocation);
                    conn.add(t, EARL.mode, EARL.untested);
                    return null;
                } else {
                    return mappedLocation.toString();
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (RDF4JException e) {
            throw (TestHarnessInitializationException) new TestHarnessInitializationException(e.toString())
                    .initCause(e);
        }
    }

    private List<IRI> selectTestCases(final String query) {
        final List<IRI> testCases = new ArrayList<>();
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            final TupleQuery tupleQuery = conn.prepareTupleQuery(query);
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                while (result.hasNext()) {
                    final BindingSet bindingSet = result.next();
                    final Value featureIri = bindingSet.getValue("feature");
                    testCases.add((IRI)featureIri);
                }
            }
        } catch (RDF4JException e) {
            throw (TestHarnessInitializationException) new TestHarnessInitializationException(e.toString())
                    .initCause(e);
        }
        return testCases;
    }
}
