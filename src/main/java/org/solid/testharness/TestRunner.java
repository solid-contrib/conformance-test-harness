package org.solid.testharness;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.config.TargetServer;
import org.solid.testharness.config.TestHarnessConfig;
import org.solid.testharness.http.AuthManager;
import org.solid.testharness.reporting.ResultProcessor;

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
        System.setProperty("testmode", "suite-runner");

        String serverConfigFilename = System.getProperty("config");
        String featuresDirectory = System.getProperty("features");

        logger.info("Config filename {}", serverConfigFilename);
        logger.info("Credentials path {}", System.getProperty("credentials"));
        logger.info("Options {}", System.getProperty("karate.options"));
        logger.info("Feature directory {}", featuresDirectory);

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
        if (targetServer == null) {
            logger.error("No config found for server: {}", targetServerName);
            return null;
        }

        int maxThreads = targetServer.getMaxThreads();
        logger.debug("Max threads: {}", maxThreads);

        logger.info("===================== DISCOVER TESTS ========================");
        List<String> featurePaths = getFeaturePaths(targetServer, featuresDirectory);
        logger.info("==== RUNNING FEATURE_PATHS {}", featurePaths);

        List<Path> featureFiles = new ArrayList<>();
        featurePaths.forEach(s -> featureFiles.addAll(findFeatures(Path.of(s))));
        logger.info("==== FEATURE FILES {}", featureFiles);
        if (featureFiles.isEmpty()) {
            logger.warn("There are no tests available");
        } else {
            logger.info("===================== REGISTER CLIENTS ========================");
            registerClients(targetServer);
        }

        logger.info("===================== START TESTS ========================");

        // we can also create Features which may be useful when fetching from remote resource although this may cause problems with
        // loading other features files due to classpath issues - Karate may need a RemoteResource type that knows how to fetch related
        // resources from the same URL the feature came from
//        List<Feature> featureList = featureFiles.stream().map(f -> Feature.read(new FileResource(f.toFile()))).collect(Collectors.toList());
//        logger.info("==== FEATURES {}", featureList);
//        Results results = Runner.builder()
//                .features(featureList)
//                .tags(tags)
//                .outputHtmlReport(true)
//                .parallel(8);

        List<String> tags = Collections.singletonList("~@ignore");

        Results results = Runner.builder()
                .path(featurePaths)
                .tags(tags)
                .outputCucumberJson(true)
                .outputJunitXml(true)
                .outputHtmlReport(true)
                .parallel(maxThreads);

        logger.info("===================== START REPORT ========================");
        ResultProcessor resultProcessor = new ResultProcessor(new File(results.getReportDir()));
        resultProcessor.processResults();
        resultProcessor.buildCucumberReport();

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
                "", Set.of(),
                "content-negotiation", Set.of(),
                "writing-resources", Set.of(),
                "protected-operations", Set.of("authentication", "acl"),
                "wac-allow", Set.of("authentication", "wac-allow")
//            "acp-operations", Set.of("authentication", "acp")
        );

        List<String> featurePaths = new ArrayList<>();
        testRequirements.forEach((key, value) -> {
            if (value.size() == 0 || targetCapabilities.containsAll(value)) {
                featurePaths.addAll(Collections.singletonList(Paths.get(featuresDirectory, key).toString()));
            }
        });
        return featurePaths;
    }

    public static List<Path> findFeatures(Path path) {
        List<Path> result = new ArrayList<>();
        if (!Files.isDirectory(path)) {
            if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(".feature")) {
                result.add(path);
            }
        } else {
            try (Stream<Path> walk = Files.walk(path)) {
                result.addAll(walk
                        .filter(Files::isRegularFile)   // is a file
                        .filter(p -> p.getFileName().toString().endsWith(".feature"))
                        .collect(Collectors.toList()));
            } catch (IOException e) {
                logger.error("Failed to walk path finding features", e);
            }
        }
        return result;
    }

    private void registerClients(TargetServer targetServer) {
        targetServer.getUsers().keySet().forEach(user -> {
            try {
                AuthManager.authenticate(user, targetServer);
            } catch (Exception e) {
                logger.error("Failed to register clients", e);
            }
        });
    }
}
