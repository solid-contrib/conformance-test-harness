package org.solid.testharness.utils;

import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ReportUtils {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.utils.ReportUtils");

    public static void generateReport(String karateOutputPath) {
        logger.info("===================== BUILD REPORT ========================");
        logger.info("Output path: {}", karateOutputPath);
        Collection<File> jsonFiles = FileUtils.listFiles(new File(karateOutputPath), new String[]{"json"}, true);
        List<String> jsonPaths = new ArrayList<String>(jsonFiles.size());
        jsonFiles.forEach(file -> jsonPaths.add(file.getAbsolutePath()));
        logger.info("==== REPORT ON {}", jsonPaths);
        if (jsonPaths.size() != 0) {
            Configuration config = new Configuration(new File("build"), "solid-test-harness");
            ReportBuilder reportBuilder = new ReportBuilder(jsonPaths, config);
            reportBuilder.generateReports();
        }

        // TODO: add custom formatter "config.CustomTagsFormatter:target/tags/txt"
        // TODO: Use the JSON or XML output files. Perhaps process JSON into a Jena/RDF4J model then weave into HTML/RDFa doc with a template such as mustache

        // EARL output - EarlReportBuilder(jsonPaths, config) [ or XML output ]
        /*
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
}
