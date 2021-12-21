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
package org.solid.testharness.reporting;

import com.intuit.karate.Results;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.DCTERMS;
import org.solid.common.vocab.SOLID_TEST;
import org.solid.common.vocab.TD;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.Namespaces;
import org.solid.testharness.utils.TestUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.time.Instant;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class ReportGeneratorTest {
    private static final Logger logger = LoggerFactory.getLogger(ReportGeneratorTest.class);

    @Inject
    DataRepository dataRepository;

    @Inject
    ReportGenerator reportGenerator;

    @BeforeEach
    void setUp() {
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            conn.clear();
        }
    }

    @Test
    void buildTurtleReport() {
        final StringWriter sw = new StringWriter();
        assertDoesNotThrow(() -> reportGenerator.buildTurtleReport(sw));
        assertTrue(sw.toString().length() > 1);
    }

    @Test
    void buildHtmlResultReport() throws Exception {
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/config/harness-sample.ttl"),
                Namespaces.TEST_HARNESS_URI);
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/config/config-sample.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/specification-sample-1.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-1.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/specification-sample-2.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-2.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/reporting/testsuite-results-sample.ttl"),
                TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-1.ttl").toString());
        final Results results = mock(Results.class);
        when(results.getErrorMessages()).thenReturn("ERRORS");
        reportGenerator.setResults(new TestSuiteResults(results));

        final StringWriter sw = new StringWriter();
        reportGenerator.buildHtmlResultReport(sw);
        final String report = sw.toString();
        logger.debug("OUTPUT:\n{}", report);
        assertTrue(report.length() > 1);
        assertTrue(report.contains("rel=\"owl:sameAs\">https://github.com/solid/specification-tests/"));
        assertTrue(report.contains("about=\"" + Namespaces.TEST_HARNESS_URI +
                "\" id=\"assertor\" typeof=\"earl:Software\""));
        // TODO: ASSERT:
        /*
        spec & manifest links
        spec section <section about="https://example.org/specification1" typeof="doap:Specification">

         */
    }

    @Test
    void resultsContainsModel() throws Exception {
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/config/harness-sample.ttl"),
                Namespaces.TEST_HARNESS_URI);
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/config/config-sample.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/specification-sample-1.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-1.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/specification-sample-2.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-2.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/reporting/testsuite-results-sample.ttl"),
                TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-1.ttl").toString());

        // remove unused parts of test model
        final Model resultModel;
        try (
                RepositoryConnection conn = dataRepository.getConnection();
                var statements = conn.getStatements(null, null, null)
        ) {
            final String namespace = iri(TestUtils.getFileUrl("src/test/resources/config/config-sample.ttl").toString())
                    .getNamespace();
            resultModel = QueryResults.asModel(statements);
            resultModel.remove(null, SOLID_TEST.features, null);
            resultModel.remove(iri(namespace, "testserver2"), null, null);
            resultModel.remove(iri(namespace, "testserver2#test-subject-release"), null, null);
            resultModel.remove(null, TD.preCondition, null);
            logger.debug("Results contains {} triples", resultModel.size());
        }

        final Results results = mock(Results.class);
        reportGenerator.setResults(new TestSuiteResults(results));
        final StringWriter sw = new StringWriter();
        reportGenerator.buildHtmlResultReport(sw);
        final String report = sw.toString();
//        logger.debug("OUTPUT:\n{}", report);
        final Model reportModel = TestUtils.parseRdfa(report, Namespaces.TEST_HARNESS_URI);
//        logger.debug("TURTLE:\n{}", TestUtils.toTurtle(reportModel));
        logger.debug("Report contains {} triples", reportModel.size());

        TestUtils.showModelDifferences(reportModel, resultModel, logger);
        assertTrue(Models.isSubset(resultModel, reportModel));
    }

    @Test
    void buildHtmlCoverageReport() throws IOException {
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/config/harness-sample.ttl"),
                Namespaces.TEST_HARNESS_URI);
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/specification-sample-1.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-1.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/specification-sample-2.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-2.ttl"));
        final StringWriter sw = new StringWriter();
        reportGenerator.buildHtmlCoverageReport(sw);
//        logger.debug("OUTPUT:\n{}", sw);
        assertTrue(sw.toString().length() > 1);
    }

    @Test
    void coverageContainsModel() throws Exception {
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/specification-sample-1.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-1.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/specification-sample-2.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-2.ttl"));

        // remove unused parts of test model
        final Model resultModel;
        try (
                RepositoryConnection conn = dataRepository.getConnection();
                var statements = conn.getStatements(null, null, null)
        ) {
            resultModel = QueryResults.asModel(statements);
            resultModel.remove(null, TD.preCondition, null);
            resultModel.remove(null, DCTERMS.description, null);
        }

        final StringWriter sw = new StringWriter();
        reportGenerator.buildHtmlCoverageReport(sw);
        final String report = sw.toString();
//        logger.debug("OUTPUT:\n{}", report);
        final Model reportModel = TestUtils.parseRdfa(report, Namespaces.TEST_HARNESS_URI);
//        logger.debug("TURTLE:\n{}", TestUtils.toTurtle(reportModel));
        logger.debug("Report contains {} triples", reportModel.size());

        TestUtils.showModelDifferences(reportModel, resultModel, logger);
        assertTrue(Models.isSubset(resultModel, reportModel));
    }

    @Test
    void buildHtmlResultReportFile() throws IOException {
        final File reportFile = new File("target/example-result-report.html");
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/config/harness-sample.ttl"),
                Namespaces.TEST_HARNESS_URI);
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/config/config-sample.ttl"),
                Namespaces.TEST_HARNESS_URI);
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/specification-sample-1.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-1.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/specification-sample-2.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-2.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/reporting/testsuite-results-sample.ttl"),
                TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-1.ttl").toString());
        final Results results = mock(Results.class);
        when(results.getFeaturesPassed()).thenReturn(0);
        when(results.getFeaturesFailed()).thenReturn(1);
        when(results.getFeaturesTotal()).thenReturn(1);
        when(results.getScenariosPassed()).thenReturn(1);
        when(results.getScenariosFailed()).thenReturn(1);
        when(results.getScenariosTotal()).thenReturn(2);
        when(results.getEndTime()).thenReturn(Instant.now().toEpochMilli());
        reportGenerator.setStartTime(System.currentTimeMillis() - 1000);
        reportGenerator.setResults(new TestSuiteResults(results));
        final Writer wr = Files.newBufferedWriter(reportFile.toPath());
        reportGenerator.buildHtmlResultReport(wr);
        wr.close();
        assertTrue(reportFile.exists());
    }

    @Test
    void buildHtmlCoverageReportFile() throws IOException {
        final File reportFile = new File("target/example-coverage-report.html");
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/config/harness-sample.ttl"),
                Namespaces.TEST_HARNESS_URI);
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/specification-sample-1.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-1.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/specification-sample-2.ttl"));
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/discovery/test-manifest-sample-2.ttl"));
        final Writer wr = Files.newBufferedWriter(reportFile.toPath());
        reportGenerator.buildHtmlCoverageReport(wr);
        wr.close();
        assertTrue(reportFile.exists());
    }

    @Test
    void buildHtmlCoverageReportEmpty() {
        final StringWriter sw = new StringWriter();
        assertDoesNotThrow(() -> reportGenerator.buildHtmlCoverageReport(sw));
    }

    @Test
    void buildHtmlCoverageReportBadSubject() throws IOException {
        TestUtils.insertData(dataRepository, TestUtils.PREFIXES + "_:b0 a doap:Specification .");
        final StringWriter sw = new StringWriter();
        assertThrows(ClassCastException.class, () -> reportGenerator.buildHtmlCoverageReport(sw));
    }

    @Test
    void setTime() {
        final TestSuiteResults testSuiteResults = new TestSuiteResults(null);
        final long startTime = System.currentTimeMillis();
        reportGenerator.setStartTime(startTime);
        reportGenerator.setResults(testSuiteResults);
        assertTrue(testSuiteResults.getElapsedTime() >= 0);
    }
}

