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

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.config.Config;
import org.solid.testharness.reporting.TestSuiteResults;

import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
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
    public static final String STATUS = "status";
    public static final String IGNORE_FAILURES = "ignore-failures";
    public static final String SKIP_TEARDOWN = "skip-teardown";
    public static final String SKIP_REPORTS = "skip-reports";
    public static final String TOLERABLE = "tolerable-failures";

    private List<String> filters;
    private List<String> statuses;

    private Config.RunMode runMode;
    private boolean skipReports;
    private boolean skipTearDown;
    private boolean ignoreFailures;

    @Inject
    Config config;
    @Inject
    ConformanceTestHarness conformanceTestHarness;

    public static void main(final String... args) {
        Quarkus.run(Application.class, args);
    }

    @Override
    public int run(final String... args) throws Exception {
        try {
            final int result = processCommandLine(args);
            if (result >= 0) return result;

            conformanceTestHarness.initialize();

            if (runMode == Config.RunMode.COVERAGE) {
                conformanceTestHarness.prepareCoverageReport();
                conformanceTestHarness.buildReports(Config.RunMode.COVERAGE);
                return 0;
            } else {
                final TestSuiteResults results = conformanceTestHarness.runTestSuites(filters, statuses);
                if (results.getFeatureTotal() > 0) {
                    if (!skipReports) {
                        conformanceTestHarness.buildReports(Config.RunMode.TEST);
                    }
                    if (!skipTearDown) {
                        conformanceTestHarness.cleanUp();
                    }
                }
                return !results.hasFailures() || ignoreFailures ? 0 : 1;
            }
        } catch (Exception e) {
            logger.error("Application failed", e);
        }
        return 1;
    }

    private int processCommandLine(final String... args) throws ParseException, IOException {
        logger.debug("Args: {}", Arrays.toString(args));
        final Options options = setupOptions();
        final CommandLineParser parser = new DefaultParser();
        final CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption(HELP)) {
            final HelpFormatter formatter = HelpFormatter.builder().get();
            formatter.printHelp("run", null, options, null, false);
            return 0;
        }
        if (cmd.hasOption(SKIP_REPORTS) && cmd.hasOption(COVERAGE)) {
            logger.error("The skip-reports option cannot apply when the coverage option is used");
            return 1;
        }
        if (!cmd.hasOption(SKIP_REPORTS) && !handleReportOptions(cmd)) {
            return 1;
        }

        if (cmd.hasOption(SOURCE)) {
            config.setTestSources(Arrays.stream(cmd.getOptionValues(SOURCE))
                    .filter(s -> !StringUtils.isBlank(s))
                    .collect(Collectors.toList()));
            logger.debug("Suite = {}", config.getTestSources());
        }

        if (!cmd.hasOption(COVERAGE)) {
            handleTestRunOptions(cmd);
        }
        runMode = cmd.hasOption(COVERAGE) ? Config.RunMode.COVERAGE : Config.RunMode.TEST;
        skipReports = cmd.hasOption(SKIP_REPORTS);
        skipTearDown = cmd.hasOption(SKIP_TEARDOWN);
        ignoreFailures = cmd.hasOption(IGNORE_FAILURES);
        config.logConfigSettings(runMode);
        return -1;
    }

    private void handleTestRunOptions(final CommandLine cmd) {
        if (cmd.hasOption(SUBJECTS)) {
            config.setSubjectsUrl(cmd.getOptionValue(SUBJECTS));
            logger.debug("Subjects = {}", config.getSubjectsUrl());
        }
        logger.debug("TARGET SETTING {}", cmd.getOptionValue(TARGET));
        if (cmd.hasOption(TARGET) && !StringUtils.isBlank(cmd.getOptionValue(TARGET))) {
            final String subjectsBaseUri = iri(config.getSubjectsUrl().toString()).getNamespace();
            final String target = cmd.getOptionValue(TARGET);
            final IRI testSubject = target.contains(":")
                    ? iri(target)
                    : iri(subjectsBaseUri, target);
            logger.debug("Target: {}", testSubject);
            config.setTestSubject(testSubject);
        }
        if (cmd.hasOption(FILTER)) {
            filters = Arrays.stream(cmd.getOptionValues(FILTER))
                    .filter(s -> !StringUtils.isBlank(s))
                    .collect(Collectors.toList());
            logger.debug("Filters = {}", filters);
        }
        if (cmd.hasOption(STATUS)) {
            statuses = Arrays.stream(cmd.getOptionValues(STATUS))
                    .filter(s -> !StringUtils.isBlank(s))
                    .collect(Collectors.toList());
            logger.debug("Statuses = {}", statuses);
        }
        if (cmd.hasOption(TOLERABLE)) {
            config.setTolerableFailuresFile(cmd.getOptionValue(TOLERABLE));
            logger.debug("Tolerable failures = {}", config.getTolerableFailuresFile());
        }
    }

    private boolean handleReportOptions(final CommandLine cmd) {
        final File outputDir;
        if (cmd.hasOption(OUTPUT) && !StringUtils.isBlank(cmd.getOptionValue(OUTPUT))) {
            outputDir = Path.of(cmd.getOptionValue(OUTPUT)).toAbsolutePath().normalize().toFile();
            logger.debug("Output = {}", outputDir.getPath());
        } else {
            outputDir = Path.of("").toAbsolutePath().toFile();
        }
        try (final Formatter formatter = new Formatter()) {
            if (!validateOutputDir(outputDir.toPath(), formatter)) {
                logger.error("{}", formatter);
                return false;
            }
        }
        config.setOutputDirectory(outputDir);
        return true;
    }

    private Options setupOptions() {
        final Options options = new Options();
        options.addOption(Option.builder().longOpt(COVERAGE).desc("produce a coverage report only").get());
        options.addOption(Option.builder().longOpt(SKIP_TEARDOWN)
                .desc("skip teardown (when server itself is being stopped)").get());
        options.addOption(Option.builder().longOpt(SKIP_REPORTS)
                .desc("skip report generation").get());
        options.addOption(Option.builder().longOpt(IGNORE_FAILURES)
                .desc("return success even if there are failures").get());
        options.addOption(
                Option.builder().longOpt(SUBJECTS).hasArg().desc("URL or path to test subject config (Turtle)").get()
        );
        options.addOption("t", TARGET, true, "target server");
        options.addOption(
                Option.builder("s").longOpt(SOURCE).desc("URL or path to test source(s)")
                        .hasArgs().valueSeparator(',').get()
        );
        options.addOption(
                Option.builder().longOpt(STATUS).desc("status(es) of tests to run")
                        .hasArgs().valueSeparator(',').get()
        );
        options.addOption("o", OUTPUT, true, "output directory");
        options.addOption(
                Option.builder("f").longOpt(FILTER).desc("feature filter(s)").hasArgs().valueSeparator(',').get()
        );
        options.addOption(
                Option.builder().longOpt(TOLERABLE).hasArg().desc("path to a list of tests known to fail").get()
        );
        options.addOption("h", HELP, false, "print this message");
        return options;
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
        return true;
    }
}
