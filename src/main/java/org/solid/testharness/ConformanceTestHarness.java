/*
 * MIT License
 *
 * Copyright (c) 2019 - 2022 W3C Solid Community Group
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
package org.solid.testharness;

import com.intuit.karate.core.Tag;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.DOAP;
import org.solid.common.vocab.EARL;
import org.solid.common.vocab.RDF;
import org.solid.testharness.api.SolidClient;
import org.solid.testharness.config.Config;
import org.solid.testharness.config.PathMappings;
import org.solid.testharness.config.TestSubject;
import org.solid.testharness.discovery.TestSuiteDescription;
import org.solid.testharness.http.AuthManager;
import org.solid.testharness.http.ClientRegistry;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.reporting.ReportGenerator;
import org.solid.testharness.reporting.TestSuiteResults;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.Namespaces;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;

@ApplicationScoped
@SuppressWarnings("PMD.MoreThanOneLogger")  // Additional logger provided for JSON output
public class ConformanceTestHarness {
    private static final Logger logger = LoggerFactory.getLogger(ConformanceTestHarness.class);

    private Map<String, SolidClient> clients;
    private TestSuiteResults results;

    @Inject
    Config config;
    @Inject
    TestSubject testSubject;
    @Inject
    TestSuiteDescription testSuiteDescription;
    @Inject
    TestRunner testRunner;
    @Inject
    ReportGenerator reportGenerator;
    @Inject
    DataRepository dataRepository;
    @Inject
    AuthManager authManager;
    @Inject
    PathMappings pathMappings;
    @Inject
    ClientRegistry clientRegistry;

    @SuppressWarnings("PMD.UseProperClassLoader") // this is not J2EE and the suggestion fails
    public void initialize() throws IOException {
        // set up the report run and create the assertor information in the data repository
        final IRI assertor = iri(Namespaces.TEST_HARNESS_URI);
        dataRepository.setAssertor(assertor);
        reportGenerator.setStartTime(System.currentTimeMillis());
        try (final InputStream is = getClass().getClassLoader().getResourceAsStream("assertor.properties")) {
            final Properties properties = new Properties();
            properties.load(is);
            logger.info("{}: {}", properties.getProperty("package.name"), properties.getProperty("package.version"));
            try (RepositoryConnection conn = dataRepository.getConnection()) {
                final ModelBuilder builder = new ModelBuilder();
                final IRI release = iri(Namespaces.RESULTS_URI, "assertor-release");
                conn.add(builder.subject(assertor)
                        .add(RDF.type, EARL.Software)
                        .add(DOAP.name, properties.getProperty("package.name"))
                        .add(DOAP.description, literal(properties.getProperty("package.description"), "en"))
                        .add(DOAP.created, Date.from(
                                Instant.from(DateTimeFormatter.ISO_INSTANT
                                        .parse(properties.getProperty("package.buildTime")))
                        ))
                        .add(DOAP.developer, iri(properties.getProperty("package.organizationUrl")))
                        .add(DOAP.homepage, iri(properties.getProperty("package.url")))
                        .add(DOAP.programming_language, "Java")
                        .add(DOAP.release, release)
                        .add(release, DOAP.revision, properties.getProperty("package.version"))
                        .build());
            }
        }
        // load the test manifests
        logger.info("===================== DISCOVER TESTS ========================");
        testSuiteDescription.load(config.getTestSources());
        testSuiteDescription.getTestsVersion();
        logger.info("==== TEST CASES FOUND: {} - {}",
                testSuiteDescription.getTestCases(false).size(),
                testSuiteDescription.getTestCases(false));
    }

    public void prepareCoverageReport() {
        testSuiteDescription.prepareTestCases(Config.RunMode.COVERAGE);
    }

    public TestSuiteResults runTestSuites(final List<String> filters, final List<String> statuses) {
        final List<String> featurePaths;

        // TODO: Consider running some initial tests to discover the features provided by a server
        testSubject.loadTestSubjectConfig();

        testSuiteDescription.setNonRunningTestAssertions(filters, statuses);
        logger.info("==== SKIP TAGS:             {}", testSubject.getTargetServer().getSkipTags());
        logger.info("==== APPLY NAME FILTERS:    {}", filters);
        logger.info("==== APPLY STATUS FILTERS:  {}", statuses);
        logger.info("==== FILTERED TEST CASES ({}): {}",
                testSuiteDescription.getTestCases(true).size(),
                testSuiteDescription.getTestCases(true));

        testSuiteDescription.prepareTestCases(Config.RunMode.TEST);
        featurePaths = testSuiteDescription.getFeaturePaths();
        if (featurePaths == null || featurePaths.isEmpty()) {
            logger.warn("There are no tests available");
            results = TestSuiteResults.emptyResults();
        } else {
            logger.info("==== RUNNING TEST CASES ({}): {}", featurePaths.size(), featurePaths);
            setupTestHarness();
            results = runTests(featurePaths, true);
        }

        results.log();
        return results;
    }

    public void buildReports(final Config.RunMode mode) {
        logger.info("===================== BUILD REPORTS ========================");
        final File outputDir = config.getOutputDirectory();
        logger.info("Reports location: [{}]", outputDir.getPath());
        try {
            if (mode == Config.RunMode.COVERAGE) {
                final File coverageHtmlFile = new File(outputDir, "coverage.html");
                logger.info("Coverage report HTML/RDFa file: {}", coverageHtmlFile.toPath().toUri());
                reportGenerator.buildHtmlCoverageReport(Files.newBufferedWriter(coverageHtmlFile.toPath()));
            } else {
                reportGenerator.setResults(results);
                final File reportTurtleFile = new File(outputDir, "report.ttl");
                logger.info("Report Turtle file: {}", reportTurtleFile.toPath().toUri());
                reportGenerator.buildTurtleReport(Files.newBufferedWriter(reportTurtleFile.toPath()));

                final File reportHtmlFile = new File(outputDir, "report.html");
                logger.info("Report HTML/RDFa file: {}", reportHtmlFile.toPath().toUri());
                reportGenerator.buildHtmlResultReport(Files.newBufferedWriter(reportHtmlFile.toPath()));
            }
        } catch (Exception e) {
            logger.error("Failed to write reports", e);
        }
    }

    public TestSuiteResults runSingleTest(final String uri) {
        try {
            testSubject.loadTestSubjectConfig();
            setupTestHarness();
        } catch (TestHarnessInitializationException e) {
            logger.error("Cannot run test", e);
            return null;
        }
        return runTests(List.of(uri), false);
    }

    private void setupTestHarness() {
        logger.info("===================== REGISTER CLIENTS ========================");
        if (config.getUserRegistrationEndpoint() != null) {
            registerUsers();
        }
        registerClients();
        logger.info("===================== PREPARE SERVER ========================");
        testSubject.prepareServer();
    }

    private TestSuiteResults runTests(final List<String> featurePaths, final boolean enableReporting) {
        logger.info("===================== RUN TESTS ========================");
        final List<String> skipTags = testSubject.getTargetServer().getSkipTags();
        results = testRunner.runTests(featurePaths, config.getMaxThreads(),
                skipTags, enableReporting);
        // any features which are skipped are not included in the feature reporting phase so add assertions now
        if (skipTags != null && !skipTags.isEmpty()) {
            results.getFeatures().stream()
                    .map(fc -> fc.feature)
                    .filter(f -> f.getTags() != null)
                    .filter(f -> !f.getTags().isEmpty())
                    .filter(f -> f.getTags().stream().map(Tag::getName).anyMatch(skipTags::contains))
                    .forEach(f -> dataRepository.createSkippedAssertion(
                            f, pathMappings.unmapFeaturePath(f.getResource().getRelativePath()), EARL.inapplicable
                    ));
        }
        // any features which are @ignored are not included in the feature reporting phase so add assertions now
        results.getFeatures().stream()
                .map(fc -> fc.feature)
                .filter(f -> f.getTags() != null)
                .filter(f -> !f.getTags().isEmpty())
                .filter(f -> f.getTags().stream().map(Tag::getName).anyMatch("ignore"::equals))
                .forEach(f -> dataRepository.createSkippedAssertion(
                        f, pathMappings.unmapFeaturePath(f.getResource().getRelativePath()), EARL.untested
                ));
        results.summarizeOutcomes(dataRepository);
        return results;
    }

    public void cleanUp() {
        logger.info("===================== DELETING TEST RESOURCES ========================");
        testSubject.tearDownServer();
    }

    private void registerUsers() {
        authManager.registerUser(HttpConstants.ALICE);
        authManager.registerUser(HttpConstants.BOB);
    }

    private void registerClients() {
        clients = new HashMap<>();
        config.getWebIds().keySet().forEach(user -> clients.put(user, new SolidClient(authManager.authenticate(user))));
    }

    /**
     * Return a map of the <code>SolidClient</code> instances available to the Karate tests.
     * @return the map of clients
     */
    public Map<String, SolidClient> getClients() {
        return clients;
    }
}
