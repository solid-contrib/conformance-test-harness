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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.rdf4j.model.util.Values.iri;

@QuarkusMain
public class Application implements QuarkusApplication {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static final String SUBJECTS = "subjects";
    public static final String TARGET = "target";
    public static final String SOURCE = "source";
    public static final String OUTPUT = "output";
    public static final String HELP = "help";
    public static final String COVERAGE = "coverage";
    public static final String FILTER = "filter";

    @Inject
    Config config;
    @Inject
    ConformanceTestHarness conformanceTestHarness;

    @Override
    public int run(final String... args) {
        logger.debug("Args: {}", Arrays.toString(args));

        final Options options = new Options();
        options.addOption(Option.builder().longOpt(COVERAGE).desc("produce a coverage report").build());
        options.addOption(
                Option.builder().longOpt(SUBJECTS).hasArg().desc("URL or path to test subject config (Turtle)").build()
        );
        options.addOption("t", TARGET, true, "target server");
        options.addOption(
                Option.builder("s").longOpt(SOURCE).desc("URL or path to test source(s)")
                        .hasArgs().valueSeparator(',').build()
        );
        options.addOption("o", OUTPUT, true, "output directory");
        options.addOption(
                Option.builder("f").longOpt(FILTER).desc("feature filter(s)").hasArgs().valueSeparator(',').build()
        );
        options.addOption("h", HELP, false, "print this message");

        final CommandLineParser parser = new DefaultParser();
        try {
            final CommandLine line = parser.parse(options, args);
            if (line.hasOption(HELP)) {
                final HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp( "run", options );
            } else {
                final File outputDir;
                if (line.hasOption(OUTPUT) && !StringUtils.isBlank(line.getOptionValue(OUTPUT))) {
                    outputDir = Path.of(line.getOptionValue(OUTPUT)).toAbsolutePath().normalize().toFile();
                    logger.debug("Output = {}", outputDir.getPath());
                } else {
                    outputDir = Path.of("").toAbsolutePath().toFile();
                }
                try (final Formatter formatter = new Formatter()) {
                    if (!validateOutputDir(outputDir.toPath(), formatter)) {
                        logger.error("{}", formatter);
                        return 1;
                    }
                }
                config.setOutputDirectory(outputDir);

                if (line.hasOption(SOURCE)) {
                    config.setTestSources(Arrays.stream(line.getOptionValues(SOURCE))
                            .filter(s -> !StringUtils.isBlank(s))
                            .collect(Collectors.toList()));
                    logger.debug("Suite = {}", config.getTestSources().toString());
                }

                conformanceTestHarness.initialize();

                if (line.hasOption(COVERAGE)) {
                    return conformanceTestHarness.createCoverageReport() ? 0 : 1;
                } else {
                    logger.debug("TARGET SETTING {}", line.getOptionValue(TARGET));
                    if (line.hasOption(TARGET) && !StringUtils.isBlank(line.getOptionValue(TARGET))) {
                        final String target = line.getOptionValue(TARGET);
                        final IRI testSubject = target.contains(":")
                                ? iri(target)
                                : iri(Namespaces.TEST_HARNESS_URI, target);
                        logger.debug("Target: {}", testSubject);
                        config.setTestSubject(testSubject);
                    }
                    if (line.hasOption(SUBJECTS)) {
                        config.setSubjectsUrl(line.getOptionValue(SUBJECTS));
                        logger.debug("Subjects = {}", config.getSubjectsUrl());
                    }
                    List<String> filters = null;
                    if (line.hasOption(FILTER)) {
                        filters = Arrays.stream(line.getOptionValues(FILTER))
                                .filter(s -> !StringUtils.isBlank(s))
                                .collect(Collectors.toList());
                        logger.debug("Filters = {}", filters);
                    }

                    final TestSuiteResults results = conformanceTestHarness.runTestSuites(filters);
                    return results != null && results.getFailCount() == 0 ? 0 : 1;
                }
            }
        } catch (Exception e) {
            logger.error("Application initialization failed.  Reason: {}", e.toString());
        }
        return 1;
    }

    public static void main(final String... args) {
        Quarkus.run(Application.class, args);
    }

    private boolean validateOutputDir(final Path dir, final Formatter error) {
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
}
