package org.solid.testharness.reporting;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.utils.DataRepository;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ApplicationScoped
public class ResultProcessor {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.reporting.ResultProcessor");

    private File outputDir;

    @Inject
    private DataRepository repository;

    public void buildCucumberReport() {
        Collection<File> jsonFiles = FileUtils.listFiles(outputDir, new String[]{"json"}, true);
        List<String> jsonPaths = new ArrayList<>(jsonFiles.size());
        jsonFiles.forEach(file -> jsonPaths.add(file.getAbsolutePath()));
        logger.info("==== REPORT ON {}", jsonPaths);
        if (jsonPaths.size() != 0) {
            Configuration config = new Configuration(new File("build"), "solid-test-harness");
            ReportBuilder reportBuilder = new ReportBuilder(jsonPaths, config);
            reportBuilder.generateReports();
        }
    }

    public void buildTurtleReport() {
        try {
            File reportFile = new File(outputDir, "report.ttl");
            FileWriter wr = new FileWriter(reportFile);
            repository.export(wr);
            logger.info("Report file: {}", reportFile);
        } catch (IOException e) {
            logger.error("Failed to generate test suite result report", e);
        }
    }

    public Repository getRepository() {
        return repository;
    }

    public void setReportDir(File file) {
        this.outputDir = file;
        logger.info("Output path: {}", outputDir);
    }
}
