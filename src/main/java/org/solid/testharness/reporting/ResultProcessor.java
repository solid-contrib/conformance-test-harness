package org.solid.testharness.reporting;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.EARL;
import org.solid.testharness.utils.DataRepository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ResultProcessor {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.reporting.ResultProcessor");

    private File outputDir;
    private Collection<File> jsonFiles;
    private List<TestJsonResult> results;
    private DataRepository repository;

    public ResultProcessor(DataRepository repository, File outputDir) {
        // convert JSON results to TestResult objects
        logger.info("===================== BUILD REPORT ========================");
        logger.info("Output path: {}", outputDir);
        this.outputDir = outputDir;
        jsonFiles = FileUtils.listFiles(outputDir, new String[]{"json"}, true);
        this.repository = repository;
    }

    public void buildCucumberReport() {
        List<String> jsonPaths = new ArrayList<>(jsonFiles.size());
        jsonFiles.forEach(file -> jsonPaths.add(file.getAbsolutePath()));
        logger.info("==== REPORT ON {}", jsonPaths);
        if (jsonPaths.size() != 0) {
            Configuration config = new Configuration(new File("build"), "solid-test-harness");
            ReportBuilder reportBuilder = new ReportBuilder(jsonPaths, config);
            reportBuilder.generateReports();
        }
    }

    /**
     * Parse all JSON result files into an RDF repository by adding a JSON-LD context.
     */
    public void processResultsLD() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("test-result.jsonld");
        if (inputStream == null) {
            logger.error("Failed to read JSON-LD context as a resource: test-result.jsonld");
            return;
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String context = br.lines().collect(Collectors.joining("\n"));
        try (RepositoryConnection conn = repository.getConnection()) {
            jsonFiles.forEach(f -> {
                try {
                    logger.debug("Parse {}", f.getAbsolutePath());
                    conn.add(new JsonLdContextWrappingReader(new FileReader(f), context), TESTSUITE_BASE + f.getName(), RDFFormat.JSONLD);
                } catch (IOException e) {
                    logger.error("Failed to read " + f.getAbsolutePath(), e);
                }
            });
            transformToEarl(conn);
        } catch (RDF4JException e) {
            logger.error("Failed to parse test result", e);
        }
    }

    public void buildTurtleReport() {
        try (RepositoryConnection conn = repository.getConnection()) {
            File reportFile = new File(outputDir, "report.ttl");
            FileWriter wr = new FileWriter(reportFile);
            RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, wr);
            rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true).set(BasicWriterSettings.INLINE_BLANK_NODES, true);
            conn.export(rdfWriter);
            logger.info("Report file: {}", reportFile);
        } catch (RDF4JException | IOException e) {
            logger.error("Failed to generate test suite result report", e);
        }
    }

    /**
     * Parse all JSON result files.
     */
    public void processResults() {
        ObjectMapper objectMapper = new ObjectMapper();
        results = jsonFiles.stream().map(f -> {
            try {
                List<TestJsonResult> res = objectMapper.readValue(f, new TypeReference<>(){});
                if (res.isEmpty()) {
                    throw new Exception("File didn't contain any results");
                }
                if (res.size() > 1) {
                    logger.warn("File {} contained {} results", f, res.size());
                }
                return res.get(0);
            } catch (Exception e) {
                logger.error("Couldn't read {}: {}", f, e.getMessage());
                return null;
            }
        }).collect(Collectors.toList());
    }

    public List<TestJsonResult> getResults() {
        return results;
    }

    public Repository getRepository() {
        return repository;
    }

    public int countPassedScenarios() {
        return results.stream().map(TestJsonResult::countPassedScenarios).reduce(0, Integer::sum);
    }
    public int countScenarios() {
        return results.stream().map(TestJsonResult::countScenarios).reduce(0, Integer::sum);
    }

    public int countFeatures(){
        int count = 0;
        try (RepositoryConnection conn = repository.getConnection()) {
            // TODO Change into a COUNT query
            String queryString = PREFIXES + "SELECT ?feature WHERE { ?feature a earl:TestCase } ";
            TupleQuery tupleQuery = conn.prepareTupleQuery(queryString);
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                while (result.hasNext()) {  // iterate over the result
                    BindingSet bindingSet = result.next();
                    Value feature = bindingSet.getValue("feature");
                    logger.debug("Feature: {}", feature);
                    count++;
                }
            }
        }
        return count;
    }

    private void transformToEarl(RepositoryConnection conn) {
        Update update = conn.prepareUpdate(buildTransformUpdate());
        update.execute();
    }

    private String buildTransformUpdate() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd:HH:mm:ss");
        String date = now.format(dateFormat);
        // add boilerplate for the report
        String systemData = "INSERT {\n" +
                "<" + TESTSUITE_BASE + "Report01> foaf:primaryTopic " + subject + " ;\n" +
                "  dc:issued '" + date + "'^^xsd:dateTime ;\n" +
                "  foaf:maker " + ASSERTOR + ".\n" +
                subject + " a doap:Project, earl:TestSubject, earl:Software ;\n" +
                "  doap:name         '"+targetServerLongName+"' ;\n" +
                "  doap:release [\n" +
                "    doap:name     '" + targetServerShortName + " " + targetServerVersion + "';\n" +
                "    doap:revision '" + targetServerVersion + "' ;\n" +
                "    doap:created  '" + targetServerReleaseDate + "'^^xsd:date\n" +
                "  ] ;\n" +
                "  doap:developer     <" + targetServerDeveloper + "> ;\n" +
                "  doap:homepage      <" + targetServerDeveloper + "> ;\n" +
                "  doap:description   '" + targetServerLongName + "'@en ;\n" +
                "  doap:programming-language '" + targetServerLanguage + "' .\n" +
                "} WHERE {}";
        // move the renamed outcome up to the step level and delete the old result node
        String changeOutcomes = "DELETE {\n" +
                "  ?step testsuite:hasResult ?result .\n" +
                "  ?result earl:outcome ?status .\n" +
                "} INSERT { \n" +
                "  ?step earl:outcome ?newStatus .\n" +
                "} WHERE {\n" +
                "  ?step testsuite:hasResult ?result .\n" +
                "  ?result earl:outcome ?status .\n" +
                "  BIND(URI(REPLACE(REPLACE(STR(?status), '" + TESTSUITE_BASE + "', '" + EARL.NAMESPACE + "'), 'skipped', 'untested')) AS ?newStatus)\n" +
                "}";
        // add classes to a feature and change the comment to a title
        String changeFeatures = "DELETE {\n" +
                "  ?feature testsuite:keyword 'Feature' .\n" +
                "  ?feature rdfs:comment ?featureTitle .\n" +
                "  ?feature rdfs:label ?featureLabel .\n" +
                "} INSERT { \n" +
                "  ?feature a earl:TestCriterion, earl:TestCase .\n" +
                "  ?feature dc:title ?featureTitle .\n" +
                "} WHERE {\n" +
                "  ?feature testsuite:keyword 'Feature' .\n" +
                "  ?feature rdfs:comment ?featureTitle .\n" +
                "  ?feature rdfs:label ?featureLabel .\n" +
                "}";
        // move the trace info to the step level and remove the old trace node
        // TODO: Should this use earl:message rather than earl:info
        String moveTraces = "DELETE {\n" +
                "  ?step testsuite:trace ?trace .\n" +
                "  ?trace earl:info ?info .\n" +
                "} INSERT { \n" +
                "  ?step earl:info ?info .\n" +
                "} WHERE {\n" +
                "  ?step testsuite:trace ?trace .\n" +
                "  ?trace earl:info ?info .\n" +
                "}";
        // delete the background assertions and their contents
        String deleteBackgrounds = "DELETE { \n" +
                "  ?feature earl:assertions ?assertion.\n" +
                "  ?assertion ?p1 ?o1 .\n" +
                "  ?step ?p2 ?o2 .\n" +
                "} WHERE { \n" +
                "  ?feature earl:assertions ?assertion.\n" +
                "  ?assertion testsuite:keyword 'Background'.\n" +
                "  ?assertion earl:result ?step .\n" +
                "  ?assertion ?p1 ?o1 .\n" +
                "  ?step ?p2 ?o2 .\n" +
                "}";
        String changeAssertions = "DELETE {\n" +
                "  ?assertion testsuite:keyword ?keyword .\n" +
                "  ?assertion rdfs:comment ?comment .\n" +
                "} INSERT { \n" +
                "  ?assertion a earl:Assertion .\n" +
                "  ?assertion earl:assertedBy " + ASSERTOR + " .\n" +
                "  ?assertion earl:test ?feature .\n" +
                "  ?assertion earl:subject " + subject + " .\n" +
                "  ?assertion earl:mode earl:automatic .\n" +
                "} WHERE {\n" +
                "  ?feature earl:assertions ?assertion .\n" +
                "  ?assertion testsuite:keyword ?keyword .\n" +
                "  ?assertion rdfs:comment ?comment .\n" +
                "}";
        String changeResults = "DELETE {\n" +
                "  ?result testsuite:keyword ?keyword .\n" +
                "  ?result rdfs:label ?label .\n" +
                "} INSERT { \n" +
                "  ?result a earl:TestResult .\n" +
                // TODO: add duration to start date for actual time
                "  ?result dc:date '" + date + "'^^xsd:dateTime .\n" +
                "  ?result rdfs:label ?keyword .\n" +
                "  ?result rdfs:description ?label .\n" +
                "} WHERE {\n" +
                "  ?element earl:result ?result .\n" +
                "  ?result testsuite:keyword ?keyword .\n" +
                "  ?result rdfs:label ?label .\n" +
                "}";
        List<String> updates = Arrays.asList(
                systemData,
                changeOutcomes,
                changeFeatures,
                moveTraces,
                deleteBackgrounds,
                changeAssertions,
                changeResults
        );
        String updateString = updates.stream().collect(Collectors.joining(";\n", PREFIXES, ""));
//        logger.debug("EARL Transform Update:\n{}", updateString);
        return updateString;
    }

    // TODO: The following will move to a data object passed into this class
    String subject = "<https://pod-compat.inrupt.com/>";
    String targetServerLongName = "Enterprise Solid Server";
    String targetServerLanguage = "Java";
    String targetServerDeveloper = "http://inrupt.com";
    String targetServerShortName = "ESS";
    String targetServerVersion = "1.0.6";
    String targetServerReleaseDate = "2021-02-19";

    // TODO: The following will be built by the artifact generator
    private static String TESTSUITE_BASE = "http://solidcommunity.org/testsuite/";
    private static String DC_NS = "http://purl.org/dc/elements/1.1/";
    private static String TESTSUITE_NS = "http://solidcommunity.org/testsuite#";
    private static String DOAP_NS = "http://usefulinc.com/ns/doap#";
    private static String FOAF_NS = "http://xmlns.com/foaf/0.1/";
    private static String PREFIXES = "PREFIX earl: <" + EARL.NAMESPACE + ">\n" +
            "PREFIX testsuite: <" + TESTSUITE_NS + ">\n" +
            "PREFIX dc: <" + DC_NS + ">\n" +
            "PREFIX foaf: <" + FOAF_NS + ">\n" +
            "PREFIX doap: <" + DOAP_NS + ">\n" +
            "";
    private String ASSERTOR = "<https://github.com/solid/conformance-test-harness>";
}
