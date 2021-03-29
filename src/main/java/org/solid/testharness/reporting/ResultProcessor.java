package org.solid.testharness.reporting;

import org.eclipse.rdf4j.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.utils.DataRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;

@ApplicationScoped
public class ResultProcessor {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.reporting.ResultProcessor");

    private File outputDir;

    @Inject
    DataRepository repository;

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

    public void printReportToConsole() {
        StringWriter sw = new StringWriter();
        repository.export(sw);
        logger.info("REPORT\n{}", sw.toString());
    }

    public Repository getRepository() {
        return repository;
    }

    public void setReportDir(File file) {
        this.outputDir = file;
        logger.info("Output path: {}", outputDir);
    }
}
