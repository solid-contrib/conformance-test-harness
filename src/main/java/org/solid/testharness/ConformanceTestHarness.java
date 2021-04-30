package org.solid.testharness;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.DOAP;
import org.solid.common.vocab.EARL;
import org.solid.testharness.config.Config;
import org.solid.testharness.config.TestSubject;
import org.solid.testharness.discovery.TestSuiteDescription;
import org.solid.testharness.reporting.ReportGenerator;
import org.solid.testharness.reporting.TestSuiteResults;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.Namespaces;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.eclipse.rdf4j.model.util.Values.iri;

@ApplicationScoped
public class ConformanceTestHarness {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.ConformanceTestHarness");

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

    public void initialize() throws IOException {
        try (final InputStream is = getClass().getClassLoader().getResourceAsStream("assertor.properties")) {
            Properties properties = new Properties();
            properties.load(is);
            try (RepositoryConnection conn = dataRepository.getConnection()) {
                ModelBuilder builder = new ModelBuilder();
                BNode bnode = Values.bnode();
                conn.add(builder.subject(iri(Namespaces.TEST_HARNESS_URI))
                        .add(RDF.TYPE, EARL.Software)
                        .add(DOAP.name, properties.getProperty("package.name"))
                        .add(DOAP.description, properties.getProperty("package.description"))
                        .add(DOAP.created, Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(properties.getProperty("package.buildTime")))))
                        .add(DOAP.developer, iri(properties.getProperty("package.organizationUrl")))
                        .add(DOAP.homepage, iri(properties.getProperty("package.url")))
                        .add(DOAP.release, bnode)
                        .add(bnode, DOAP.revision, properties.getProperty("package.version"))
                        .build());
            }
        }
    }

    public boolean createCoverageReport() {
        config.logConfigSettings();
        logger.info("===================== DISCOVER TESTS ========================");
        try {
            testSuiteDescription.load(config.getTestSuiteDescription());
            List<IRI> testCases = testSuiteDescription.getAllTestCases();
            List<String> featurePaths = testSuiteDescription.locateTestCases(testCases, config.getPathMappings());
            if (featurePaths.isEmpty()) {
                logger.warn("There are no tests available");
                return true;
            }
        } catch (TestHarnessInitializationException e) {
            logger.error("Cannot build report", e);
            return false;
        }

        logger.info("===================== BUILD REPORT ========================");
        File outputDir = config.getOutputDirectory();
        logger.info("Reports location: {}", outputDir.getPath());
        try {
            File coverageHtmlFile = new File(outputDir, "coverage.html");
            logger.info("Coverage report HTML/RDFa file: {}", coverageHtmlFile.getPath());
            reportGenerator.buildHtmlCoverageReport(new FileWriter(coverageHtmlFile));
            return true;
        } catch (IOException e) {
            logger.error("Failed to write coverage report", e);
            return false;
        }
    }

    public TestSuiteResults runTestSuites() {
        config.logConfigSettings();
        logger.info("===================== DISCOVER TESTS ========================");
        List<String> featurePaths;
        try {
            testSuiteDescription.load(config.getTestSuiteDescription());
            testSubject.loadTestSubjectConfig(); // TODO:is this in right place?
            // TODO: Consider running some initial tests to discover the features provided by a server
            List<IRI> testCases = testSuiteDescription.getSupportedTestCases(testSubject.getTargetServer().getFeatures().keySet());
            logger.info("==== TEST CASES FOUND: {} - {}", testCases.size(), testCases);

            featurePaths = testSuiteDescription.locateTestCases(testCases, config.getPathMappings());
            if (featurePaths.isEmpty()) {
                logger.warn("There are no tests available");
                return null;
            } else {
                logger.info("==== RUNNING {} TEST CASES: {}", featurePaths.size(), featurePaths);
            }

            testSubject.registerClients();
        } catch (TestHarnessInitializationException e) {
            logger.error("Cannot run test suites", e);
            return null;
        }

        logger.info("===================== RUN TESTS ========================");
        TestSuiteResults results = testRunner.runTests(featurePaths, testSubject.getTargetServer().getMaxThreads());

        logger.info("===================== BUILD REPORTS ========================");
        File outputDir = config.getOutputDirectory();
        logger.info("Reports location: {}", outputDir.getPath());
        try {
            File reportTurtleFile = new File(outputDir, "report.ttl");
            logger.info("Report Turtle file: {}", reportTurtleFile.getPath());
            reportGenerator.buildTurtleReport(new FileWriter(reportTurtleFile));

            File reportHtmlFile = new File(outputDir, "report.html");
            logger.info("Report HTML/RDFa file: {}", reportHtmlFile.getPath());
            reportGenerator.buildHtmlResultReport(new FileWriter(reportHtmlFile));
//            resultProcessor.printReportToConsole();
        } catch (Exception e) {
            logger.error("Failed to write reports", e);
        }

        logger.info(results.toString());
        return results;
    }
}
