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
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.*;
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

    /**
     * Load data from the list of URLs.
     * @param urlList starting points for discovering tests
     */
    public void load(final List<URL> urlList) {
        for (final URL url: urlList) {
            dataRepository.load(pathMappings.mapUrl(url));
        }
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            conn.getStatements(null, RDF.type, SPEC.Specification).stream()
                    .map(Statement::getSubject)
                    .flatMap(s -> conn.getStatements(s, RDFS.seeAlso, null).stream())
                    .map(Statement::getObject)
                    .filter(Value::isIRI)
                    .map(IRI.class::cast)
                    .forEach(o -> dataRepository.load(pathMappings.mapIri(o)));
        } catch (RDF4JException e) {
            throw (TestHarnessInitializationException) new TestHarnessInitializationException(e.toString())
                    .initCause(e);
        }
    }

    /**
     * This searches the whole dataRepository for supported test cases based on the server capabilities.
     * @param serverFeatures set of supported features
     * @return List of features
     */
    public List<IRI> getSupportedTestCases(final Set<String> serverFeatures) {
        return getFilteredTestCases(serverFeatures, true);
    }

    public List<IRI> getAllTestCases() {
        return getFilteredTestCases(null, false);
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

    private List<IRI> getFilteredTestCases(final Set<String> serverFeatures, final boolean applyFilters) {
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            return conn.getStatements(null, SPEC.requirement, null).stream()
                    .map(Statement::getObject)
                    .filter(Value::isIRI)
                    .map(IRI.class::cast)
                    // get groups that reference the specification requirement
                    .flatMap(specRef -> conn.getStatements(null, TD.specificationReference, specRef).stream())
                    .map(Statement::getSubject)
                    // filter groups based on preConditions
                    // - either no preCondition exists or the preconditions are all in serverFeatures
                    .filter(group -> !applyFilters ||
                            !conn.getStatements(group, TD.preCondition, null).hasNext() ||
                            conn.getStatements(group, TD.preCondition, null).stream()
                                    .map(Statement::getObject)
                                    .filter(Value::isLiteral)
                                    .map(Value::stringValue)
                                    .allMatch(serverFeature ->
                                            serverFeatures != null && serverFeatures.contains(serverFeature)
                                    )
                    )
                    // get features in each group
                    .flatMap(group -> conn.getStatements(group, DCTERMS.hasPart, null).stream())
                    .map(Statement::getObject)
                    .filter(Value::isIRI)
                    .map(IRI.class::cast)
                    .collect(Collectors.toList());
        } catch (RDF4JException e) {
            throw (TestHarnessInitializationException) new TestHarnessInitializationException(e.toString())
                    .initCause(e);
        }
    }
}
