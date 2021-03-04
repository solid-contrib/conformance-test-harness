package org.solid.testharness;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.config.TargetServer;
import org.solid.testharness.config.TestHarnessConfig;
import org.solid.testharness.utils.ReportUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestRunner {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.TestRunner");

    public Results runTests() {
        // allow tests to run against server on localhost
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());

        String serverConfigFilename = System.getProperty("config");
        logger.debug("Config filename {}", serverConfigFilename);
        logger.info("Credentials path {}", System.getProperty("credentials"));

        ObjectMapper objectMapper = new ObjectMapper();
        TestHarnessConfig config;
        try {
            config = objectMapper.readValue(new File(serverConfigFilename), TestHarnessConfig.class);
        } catch (Exception e) {
            logger.error("Config not loaded: {}", e.getMessage());
            return null;
        }

        String targetServerName = System.getProperty("karate.env", config.getTarget());
        logger.info("Target server: {}", targetServerName);

        TargetServer targetServer = config.getServers().get(targetServerName);

        int maxThreads = targetServer.getMaxThreads();
        if (maxThreads <= 0) {
            maxThreads = 1;
        }
        logger.debug("Max threads: {}", maxThreads);

        String featuresDirectory = System.getProperty("features");

        logger.info("===================== START TESTS ========================");
        List<String> featurePaths = getFeaturePaths(targetServer, featuresDirectory);
        logger.info("==== RUNNING FEATURE_PATHS {}", featurePaths);

        List<Path> featureFiles = new ArrayList<>();
        featurePaths.forEach(s -> featureFiles.addAll(findFeatures(Path.of(s))));
        logger.info("==== FEATURES FILES {}", featureFiles);

        // we can also create Features which may be useful when fetching from remote resource although this may cause problems with
        // loading other features files due to classpath issues - Karate may need a RemoveResource type that knows how to fetch related
        // resources from the same URL the feature came from
//        List<Feature> featureList = featureFiles.stream().map(f -> Feature.read(new FileResource(f.toFile()))).collect(Collectors.toList());
//        logger.info("==== FEATURES {}", featureList);
//        Results results = Runner.builder()
//                .features(featureList)
//                .tags(tags)
//                .outputHtmlReport(true)
//                .parallel(8);

        List<String> tags = Arrays.asList("~@ignore");

        Results results = Runner.builder()
                .path(featurePaths)
                .tags(tags)
//                .outputCucumberJson(true)
                .outputHtmlReport(true)
                .parallel(maxThreads);

        logger.info("===================== START REPORT ========================");
        ReportUtils.generateReport(results.getReportDir());

        logger.info("Results:\n  Features  passed: {}, failed: {}, total: {}\n  Scenarios passed: {}, failed: {}, total: {}",
                results.getFeaturesPassed(), results.getFeaturesFailed(), results.getFeaturesTotal(),
                results.getScenariosPassed(), results.getScenariosFailed(), results.getScenariosTotal()
        );

        return results;
    }

    private List<String> getFeaturePaths(TargetServer config, String featuresDirectory) {
        // select the tests to be run based on the test requirements and server capabilities
        // TODO: This will be based on a process of reading in the server config and the test description document
        // TODO: Later - test the server capabilities first rather than use config

        Set<String> targetCapabilities = config.getFeatures().keySet();
        logger.debug("Server features: {}", targetCapabilities.toString());

        Map<String, Set<String>> testRequirements = Map.of(
                "content-negotiation", Set.of(),
                "writing-resources", Set.of(),
                "protected-operations", Set.of("authentication", "acl"),
                "wac-allow", Set.of("authentication", "wac-allow")
//            "acp-operations", Set.of("authentication", "acp")
        );

        List<String> featurePaths = new ArrayList<>();
        testRequirements.entrySet().stream().forEach(entry -> {
            if (entry.getValue().size() == 0 || targetCapabilities.containsAll(entry.getValue())) {
                featurePaths.addAll(Collections.singletonList(Paths.get(featuresDirectory, entry.getKey()).toString()));
            }
        });
        return featurePaths;
    }

    public static List<Path> findFeatures(Path path) {
        if (!Files.isDirectory(path)) {
            if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(".feature")) {
                return Collections.singletonList(path);
            } else {
                return null;
            }
        }

        List<Path> result = null;
        try (Stream<Path> walk = Files.walk(path)) {
            result = walk
                    .filter(Files::isRegularFile)   // is a file
                    .filter(p -> p.getFileName().toString().endsWith(".feature"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Failed to walk path finding features", e);
        }
        return result;

    }
}
