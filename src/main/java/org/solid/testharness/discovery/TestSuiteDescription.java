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
import org.eclipse.rdf4j.model.datatypes.XMLDateTime;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.*;
import org.solid.testharness.config.Config;
import org.solid.testharness.config.PathMappings;
import org.solid.testharness.http.HttpUtils;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.FeatureFileParser;
import org.solid.testharness.utils.Namespaces;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;

/**
 * Representation of a test suite description document parsed from RDF.
 */
@ApplicationScoped
public class TestSuiteDescription {
    private static final Logger logger = LoggerFactory.getLogger(TestSuiteDescription.class);
    private static final Pattern VERSION_INFO = Pattern.compile("^v?(\\d+\\.\\d+\\.\\d+)(?: (\\d{4}-\\d{2}-\\d{2}))?$");
    private static final String START_OF_DAY = "T00:00:00Z";
    protected static IRI TEST_VERSION = iri(Namespaces.TESTS_REPO_URI, "version.txt");

    private List<String> featurePaths;
    private String currentVersion;
    private String releaseDate;

    @Inject
    DataRepository dataRepository;

    @Inject
    PathMappings pathMappings;

    public List<String> getFeaturePaths() {
        return featurePaths;
    }

    public List<IRI> getTestCases(final boolean filtered) {
        try (
                RepositoryConnection conn = dataRepository.getConnection();
                var statements = conn.getStatements(null, RDF.type, TD.TestCase)
        ) {
            return statements.stream()
                    .map(Statement::getSubject)
                    .filter(tc -> !filtered || !conn.hasStatement(null, EARL.test, tc, false))
                    .filter(Value::isIRI)
                    .map(IRI.class::cast)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Load data from the list of URLs.
     * @param urlList starting points for discovering tests
     */
    public void load(final List<URL> urlList) {
        for (final URL url: urlList) {
            dataRepository.load(pathMappings.mapUrl(url), url.toString());
        }
        dataRepository.identifySpecifications();
    }

    public void getTestsVersion() {
        getCurrentVersionInfo();
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            final ModelBuilder builder = new ModelBuilder();
            final IRI release = iri(Namespaces.RESULTS_URI, "tests-release");
            builder.subject(Namespaces.SPECIFICATION_TESTS_IRI)
                    .add(RDF.type, DOAP.Project)
                    .add(DOAP.name, "Specification Tests")
                    .add(DOAP.description, literal("Solid Specification Conformance Tests", "en"))
                    .add(DOAP.developer, iri("https://solidproject.org"))
                    .add(DOAP.homepage, iri(Namespaces.TESTS_REPO_URI))
                    .add(DOAP.programming_language, "KarateDSL")
                    .add(DOAP.release, release)
                    .add(release, DOAP.revision, currentVersion != null ? currentVersion : "unknown");
            if (releaseDate != null) {
                builder.add(DOAP.created,
                        literal(new XMLDateTime(releaseDate + START_OF_DAY).toString(),XSD.DATETIME));
            }
            conn.add(builder.build());
        }
    }

    @SuppressWarnings({"PMD.AvoidLiteralsInIfCondition", "PMD.NullAssignment"})
    private void getCurrentVersionInfo() {
        currentVersion = null;
        releaseDate = null;
        try {
            final String str = new String(Files.readAllBytes(Paths.get(pathMappings.mapIri(TEST_VERSION))));
            final Matcher matcher = VERSION_INFO.matcher(str.trim());
            if (matcher.matches()) {
                currentVersion = matcher.group(1);
                releaseDate = matcher.group(2);
                logger.info("Test suite version: {} {}", currentVersion, releaseDate);
            }
        } catch (Exception e) {
            logger.warn("Failed to read the version of the tests: {}", e.getMessage());
        }
    }

    public void setNonRunningTestAssertions(final List<String> filters, final List<String> statuses) {
        final List<String> filterList = filters != null && !filters.isEmpty() ? filters : null;
        final List<IRI> statusList = statuses != null && !statuses.isEmpty()
                ? statuses.stream().map(s -> iri(TD.NAMESPACE, s)).collect(Collectors.toList())
                : null;
        try (
                RepositoryConnection conn = dataRepository.getConnection();
                var statements = conn.getStatements(null, RDF.type, TD.TestCase)
        ) {
            // add assertions to any filtered out tests
            statements.stream()
                    .map(Statement::getSubject)
                    .filter(Value::isIRI)
                    .map(IRI.class::cast)
                    .filter(tc -> failsFilterCheck(tc, filterList) || failsStatusCheck(conn, tc, statusList))
                    .forEach(tc -> dataRepository.createAssertion(conn, EARL.untested, new Date(), tc));
        } catch (RDF4JException e) {
            throw (TestHarnessInitializationException) new TestHarnessInitializationException(e.toString())
                    .initCause(e);
        }
    }

    private boolean failsFilterCheck(final IRI testCase, final List<String> filters) {
        // the test case doesn't match the filter so will not be tested
        return filters != null && filters.stream().noneMatch(f -> testCase.stringValue().contains(f));
    }

    private boolean failsStatusCheck(final RepositoryConnection conn, final IRI testCase, final List<IRI> statuses) {
        // the test case review status doesn't match the status list so will not be tested
        return statuses != null &&
                statuses.stream().noneMatch(s -> conn.hasStatement(testCase, TD.reviewStatus, s, false));
    }

    public void prepareTestCases(final Config.RunMode runMode) {
        try (
                RepositoryConnection conn = dataRepository.getConnection();
                var statements = conn.getStatements(null, RDF.type, TD.TestCase)
        ) {
            featurePaths = statements.stream()
                    .map(Statement::getSubject)
                    .filter(Value::isIRI)
                    .map(IRI.class::cast)
                    .map(tc -> new Feature(conn, tc, runMode))
                    .peek(Feature::findFeatureIri)
                    .filter(Feature::isImplemented)
                    .peek(Feature::locateFeature)
                    .filter(Feature::isFound)
                    .peek(Feature::extractTitleIfNeeded)
                    .filter(Feature::isRunnable)
                    .map(Feature::getLocation)
                    .collect(Collectors.toList());
        } catch (RDF4JException e) {
            throw (TestHarnessInitializationException) new TestHarnessInitializationException(e.toString())
                    .initCause(e);
        }
    }

    private class Feature {
        private final RepositoryConnection conn;
        private final IRI testCaseIri;
        private IRI featureIri;
        private File featureFile;
        private String location;
        private final boolean runnable;

        public Feature(final RepositoryConnection conn, final IRI testCaseIri, final Config.RunMode runMode) {
            this.conn = conn;
            this.testCaseIri = testCaseIri;
            // test is runnable when not in coverage mode and it doesn't have an assertion already
            runnable = runMode == Config.RunMode.TEST && !conn.hasStatement(null, EARL.test, testCaseIri, false);
        }

        public void findFeatureIri() {
            try (var statements = conn.getStatements(testCaseIri, SPEC.testScript, null)) {
                statements.stream()
                        .map(Statement::getObject)
                        .filter(Value::isIRI)
                        .map(IRI.class::cast)
                        .findFirst()
                        .map(obj -> this.featureIri = obj);
            }
        }
        public boolean isImplemented() {
            return featureIri != null;
        }

        public void locateFeature() {
            // map feature IRI to file
            final URI mappedLocation = pathMappings.mapIri(featureIri);
            if (HttpUtils.isHttpProtocol(mappedLocation.getScheme())) {
                throw new TestHarnessInitializationException("Remote test cases are not yet supported - " +
                        "use mappings to point to local copies");
            }
            final File file = new File(mappedLocation.getPath());
            if (!file.exists()) {
                logger.warn("FEATURE NOT FOUND: {}", mappedLocation);
            } else {
                featureFile = file;
                location = mappedLocation.toString();
            }
        }
        public boolean isFound() {
            return featureFile != null;
        }
        public void extractTitleIfNeeded() {
            if (!runnable) {
                final String title = FeatureFileParser.getFeatureTitle(featureFile);
                if (title != null) {
                    conn.add(testCaseIri, DCTERMS.title, literal(title));
                }
            }
        }
        public String getLocation() {
            return location;
        }
        public boolean isRunnable() {
            return runnable;
        }
    }
}
