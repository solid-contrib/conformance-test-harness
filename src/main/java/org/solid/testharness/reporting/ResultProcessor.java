package org.solid.testharness.reporting;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ResultProcessor {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.reporting.ResultProcessor");
    private Collection<File> jsonFiles;
    private List<TestJsonResult> results;
    private Repository repository;

    public ResultProcessor(File outputDir) {
        // convert JSON results to TestResult objects
        logger.info("===================== BUILD REPORT ========================");
        logger.info("Output path: {}", outputDir);
        jsonFiles = FileUtils.listFiles(outputDir, new String[]{"json"}, true);
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
     * TODO: This is experimental and the context is still being developed.
     */
    public void processResultsLD() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("test-result.jsonld");
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String context = br.lines().collect(Collectors.joining("\n"));
        String baseUri = "http://solidcommunity.org/testsuite/";
        repository = new SailRepository(new MemoryStore());
        try (RepositoryConnection conn = repository.getConnection()) {
            jsonFiles.stream().forEach(f -> {
                try {
                    conn.add(new JsonLdContextWrappingReader(new FileReader(f), context), baseUri + f.getName(), RDFFormat.JSONLD);
                } catch (IOException e) {
                    logger.error("Failed to read " + f.getAbsolutePath(), e);
                }
            });
        } catch (RDF4JException e) {
            logger.error("Failed to parse test result", e);
        }
        // do a sparql update to modify data if needed
//        conn.prepareUpdate("DELETE WHERE { ?s a ex:Foo; ?p ?o }").execute();

        /*
        // EARL output - EarlReportBuilder(jsonPaths, config)
        Ref: https://www.w3.org/TR/EARL10-Schema/
        OUT: <#target.server> a earl:TestSubject .
        OUT: <#assertor> a earl:Assertor, earl:Software; doap:name "Karate Test"@en; doap:description "Sample Solid tests"@en .
        for each file
            for each feature (perhaps only 1 per file)
                result = earl:passed if $json.elements[$.type == "scenario"].steps.result.status == passed for all steps
                OUT: [] a earl:Assertion ;
                    earl:assertedBy <#assertor> ;
                    earl:subject <#target.server> ;
                    earl:test </${json.uri}> ;
                    earl:result [
                        a earl:TestResult ;
                        earl:outcome ${result}
                    ] ;
                    earl:mode earl:automatic ;
                    earl:message "Test response message"@en .
                OUT: </${json.uri}>
                    a earl:TestCase ;
                    dc11:title "${json.description}"@en .
         */
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
        return results.stream().map(r -> r.countPassedScenarios()).reduce(0, Integer::sum);
    }
    public int countScenarios() {
        return results.stream().map(r -> r.countScenarios()).reduce(0, Integer::sum);
    }

}
