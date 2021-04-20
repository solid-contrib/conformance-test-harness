package org.solid.testharness;

import com.intuit.karate.Results;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.config.TestHarnessConfig;
import org.solid.testharness.reporting.ResultProcessor;
import org.solid.testharness.utils.Namespaces;

import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;

@QuarkusMain
public class Application implements QuarkusApplication {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.Application");

    @Inject
    TestHarnessConfig testHarnessConfig;
    @Inject
    TestRunner testRunner;
    @Inject
    ResultProcessor resultProcessor;

    File outputDir;

    /**
     * test(s) to run
     * output settings
     * options for CI use e.g. overall pass/fail
     */

    @Override
    public int run(String... args) throws Exception {
        logger.debug("Args: {}", Arrays.toString(args));

        Options options = new Options();
        options.addOption(Option.builder().longOpt("coverage").desc("produce a coverage report").build());
        options.addOption("c", "config", true, "path to test subject config (Turtle)");
        options.addOption("t", "target", true, "target server");
        options.addOption("s", "suite", true, "test suite URI");
        options.addOption("o", "output", true, "output directory");
//        options.addOption("f", "feature", true, "feature filter");
        options.addOption("h", "help", false, "print this message");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp( "run", options );
            } else {
                if (line.hasOption("output") && !StringUtils.isEmpty(line.getOptionValue("output"))) {
                    outputDir = Paths.get(line.getOptionValue("output")).toAbsolutePath().normalize().toFile();
                    logger.debug("Output = {}", outputDir.getCanonicalPath());
                } else {
                    outputDir = Paths.get("").toAbsolutePath().toFile();
                }
                Formatter formatter = new Formatter();
                if (!validateOutputDir(outputDir.toPath(), formatter)) {
                    logger.error(formatter.toString());
                    return 1;
                }
                if (line.hasOption("suite")) {
                    URL url = createUrl(line.getOptionValue("suite"), "suite", formatter);
                    if (url == null) {
                        logger.error(formatter.toString());
                        return 1;
                    }
                    testHarnessConfig.setTestSuiteDescription(url);
                    logger.debug("Suite = {}", testHarnessConfig.getTestSuiteDescription().toString());
                }

                if (line.hasOption("coverage")) {
                    return coverageReport();
                } else {
                    if (line.hasOption("target") && !StringUtils.isEmpty(line.getOptionValue("target"))) {
                        String target = line.getOptionValue("target");
                        IRI testSubject = target.contains(":") ? iri(target) : iri(Namespaces.TEST_HARNESS_URI, target);
                        logger.debug("Target: {}", testSubject.stringValue());
                        testHarnessConfig.setTestSubject(testSubject);
                    }
                    if (line.hasOption("config")){
                        URL url = createUrl(line.getOptionValue("config"), "config", formatter);
                        if (url == null) {
                            logger.error(formatter.toString());
                            return 1;
                        }
                        testHarnessConfig.setConfigUrl(url);
                        logger.debug("Config = {}", testHarnessConfig.getConfigUrl().toString());
                    }

                    return runTestSuites();
                }
            }
        } catch(ParseException e) {
            logger.error("Parsing failed.  Reason: {}", e.getMessage());
        }
        return 1;
    }

    public static void main(String... args) {
        Quarkus.run(Application.class, args);
    }

    int runTestSuites() throws IOException {
        testRunner.loadTestSuite();
        testRunner.filterSupportedTests();
        List<String> featurePaths = testRunner.mapTestLocations();
        if (featurePaths.isEmpty()) {
            return 0;
        }
        Results results = testRunner.runTests(featurePaths);

        logger.info("===================== START REPORT ========================");
        try {
            logger.info("Reports location: {}", outputDir.getCanonicalPath());
            File reportTurtleFile = new File(outputDir, "report.ttl");
            logger.info("Report Turtle file: {}", reportTurtleFile.getPath());
            resultProcessor.buildTurtleReport(new FileWriter(reportTurtleFile));
            File reportHtmlFile = new File(outputDir, "report.html");
            logger.info("Report HTML/RDFa file: {}", reportHtmlFile.getPath());
            resultProcessor.buildHtmlResultReport(new FileWriter(reportHtmlFile));
//            resultProcessor.printReportToConsole();
        } catch (Exception e) {
            logger.error("Failed to write reports", e);
        }

        logger.info("Results:\n  Features  passed: {}, failed: {}, total: {}\n  Scenarios passed: {}, failed: {}, total: {}",
                results.getFeaturesPassed(), results.getFeaturesFailed(), results.getFeaturesTotal(),
                results.getScenariosPassed(), results.getScenariosFailed(), results.getScenariosTotal()
        );

        return results != null && results.getFailCount() == 0 ? 0 : 1;
    }

    int coverageReport() {
        testRunner.loadTestSuite();
        if (!testRunner.mapTestLocations().isEmpty()) {
            try {
                logger.info("Reports location: {}", outputDir.getCanonicalPath());
                File coverageHtmlFile = new File(outputDir, "coverage.html");
                logger.info("Coverage report HTML/RDFa file: {}", coverageHtmlFile.getPath());
                resultProcessor.buildHtmlCoverageReport(new FileWriter(coverageHtmlFile));
            } catch (Exception e) {
                logger.error("Failed to write reports", e);
                return 1;
            }
        }
        return 0;
    }

    private boolean validateOutputDir(Path dir, Formatter error) {
        if (!Files.exists(dir)) {
            error.format("Output directory '%s' does not exist", dir);
            return false;
        }
        if (!Files.isDirectory(dir)) {
            error.format("Output directory '%s' is not a directory", dir);
            return false;
        }
        if (!Files.isWritable(dir)) {
            error.format("Output directory '%s' is not writeable", dir);
            return false;
        }
        if (!Files.isExecutable(dir)) {
            error.format("Output directory '%s' is not executable", dir);
            return false;
        }
        return true;
    }

    private URL createUrl(String path, String param, Formatter error) {
        if (!StringUtils.isEmpty(path)) {
            try {
                if (path.startsWith("file:") || path.startsWith("http:") || path.startsWith("https:")) {
                    return new URL(path);
                } else {
                    return Paths.get(path).toAbsolutePath().normalize().toUri().toURL();
                }
            } catch (MalformedURLException e) {
                error.format("%s '%s' is not a valid file path or URL: %s", param, path, e.getMessage());
            }
        }
        return null;
    }

}
