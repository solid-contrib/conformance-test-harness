package org.solid.testharness;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.config.Config;
import org.solid.testharness.reporting.TestSuiteResults;
import org.solid.testharness.utils.Namespaces;

import javax.inject.Inject;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Formatter;

import static org.eclipse.rdf4j.model.util.Values.iri;

@QuarkusMain
public class Application implements QuarkusApplication {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Inject
    Config config;
    @Inject
    ConformanceTestHarness conformanceTestHarness;

    @Override
    public int run(String... args) {
        logger.debug("Args: {}", Arrays.toString(args));

        Options options = new Options();
        options.addOption(Option.builder().longOpt("coverage").desc("produce a coverage report").build());
        options.addOption("c", "config", true, "URL or path to test subject config (Turtle)");
        options.addOption("t", "target", true, "target server");
        options.addOption("s", "suite", true, "URL or path to test suite description");
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
                File outputDir;
                if (line.hasOption("output") && !StringUtils.isEmpty(line.getOptionValue("output"))) {
                    outputDir = Path.of(line.getOptionValue("output")).toAbsolutePath().normalize().toFile();
                    logger.debug("Output = {}", outputDir.getPath());
                } else {
                    outputDir = Path.of("").toAbsolutePath().toFile();
                }
                Formatter formatter = new Formatter();
                if (!validateOutputDir(outputDir.toPath(), formatter)) {
                    logger.error(formatter.toString());
                    return 1;
                }
                config.setOutputDirectory(outputDir);

                if (line.hasOption("suite")) {
                    URL url = createUrl(line.getOptionValue("suite"), "suite", formatter);
                    if (url == null) {
                        logger.error(formatter.toString());
                        return 1;
                    }
                    config.setTestSuiteDescription(url);
                    logger.debug("Suite = {}", config.getTestSuiteDescription().toString());
                }

                conformanceTestHarness.initialize();

                if (line.hasOption("coverage")) {
                    return conformanceTestHarness.createCoverageReport() ? 0 : 1;
                } else {
                    if (line.hasOption("target") && !StringUtils.isEmpty(line.getOptionValue("target"))) {
                        String target = line.getOptionValue("target");
                        IRI testSubject = target.contains(":") ? iri(target) : iri(Namespaces.TEST_HARNESS_URI, target);
                        logger.debug("Target: {}", testSubject.stringValue());
                        config.setTestSubject(testSubject);
                    }
                    if (line.hasOption("config")){
                        URL url = createUrl(line.getOptionValue("config"), "config", formatter);
                        if (url == null) {
                            logger.error(formatter.toString());
                            return 1;
                        }
                        config.setConfigUrl(url);
                        logger.debug("Config = {}", config.getConfigUrl().toString());
                    }

                    TestSuiteResults results = conformanceTestHarness.runTestSuites();
                    return results != null && results.getFailCount() == 0 ? 0 : 1;
                }
            }
        } catch(Exception e) {
            logger.error("Application initialization failed.  Reason: {}", e.toString());
        }
        return 1;
    }

    public static void main(String... args) {
        Quarkus.run(Application.class, args);
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
                    return Path.of(path).toAbsolutePath().normalize().toUri().toURL();
                }
            } catch (MalformedURLException e) {
                error.format("%s '%s' is not a valid file path or URL: %s", param, path, e);
            }
        }
        return null;
    }
}
