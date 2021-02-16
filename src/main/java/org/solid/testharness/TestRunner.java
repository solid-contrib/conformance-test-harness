package org.solid.testharness;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.solid.testharness.utils.ReportUtils;
import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestRunner {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.runner.TestRunner");

    public Results runTests() {
        // allow tests to run against server on localhost
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());

        String serverConfigFilename = System.getProperty("config");
        logger.debug("Config filename {}", serverConfigFilename);
        Map<String, Object> config = loadConfig(serverConfigFilename);
        if (config == null) {
            logger.error("Config not found");
            return null;
        }
        logger.info("Credentials path {}", System.getProperty("credentials"));

        String targetServer = getTarget(config, System.getProperty("karate.env"));
        logger.info("Target server: {}", targetServer);

        Set<String> targetCapabilities = getTargetCapablities(config, targetServer);
        logger.debug("Server features: {}", targetCapabilities.toString());

        String featuresDirectory = System.getProperty("features");

        logger.info("===================== START TESTS ========================");
        List<String> featurePaths = getFeaturePaths(targetCapabilities, featuresDirectory);
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
                .parallel(8);

        logger.info("===================== START REPORT ========================");
        ReportUtils.generateReport(results.getReportDir());

        logger.info("Results:\n  Features  passed: {}, failed: {}, total: {}\n  Scenarios passed: {}, failed: {}, total: {}",
                results.getFeaturesPassed(), results.getFeaturesFailed(), results.getFeaturesTotal(),
                results.getScenariosPassed(), results.getScenariosFailed(), results.getScenariosTotal()
        );

        return results;
    }

    private List<String> getFeaturePaths(Set<String> targetCapabilities, String featuresDirectory) {
        // select the tests to be run based on the test requirements and server capabilities
        // TODO: This will be based on a process of reading in the server config and the test description document
        // TODO: Later - test the server capabilities first rather than use config

        Map<String, Set<String>> testRequirements = Map.of(
                "content-negotiation", Set.of(),
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

    String getTarget(Map<String, Object> config, String override) {
        return override != null && override.length() != 0 ? override : (String) config.get("target");
    }

    Set<String> getTargetCapablities(Map<String, Object> config, String target) {
        // TODO: This is horrible but it is only a placeholder for future work
        Map<String, Object> servers = (Map<String, Object>) config.get("servers");
        Map<String, Object> serverConfig = (Map<String, Object>) servers.get(target);
        Map<String, Object> featureConfig = (Map<String, Object>) serverConfig.get("features");
        return featureConfig.keySet();
    }

    Map<String,Object> loadConfig(String filename) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            if (filename == null) {
                logger.error("Config file missing: {}", filename);
                return null;
            }
            return mapper.readValue(new File(filename), Map.class);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
