package org.solid.testharness.reporting;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.utils.DataRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import static org.eclipse.rdf4j.model.util.Values.iri;

@ApplicationScoped
public class ResultProcessor {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.reporting.ResultProcessor");

    @Inject
    DataRepository repository;

    @ConfigProperty(name = "testSuiteNamespace")
    String testSuiteNamespace;

    @Location("coverage-report.html")
    Template coverageTemplate;
    @Location("result-report.html")
    Template resultTemplate;


    public void buildTurtleReport(Writer writer) throws Exception {
        repository.export(writer);
    }

    public void printReportToConsole() throws Exception {
        StringWriter sw = new StringWriter();
        repository.export(sw);
        logger.info("REPORT\n{}", sw.toString());
    }

    public void buildHtmlCoverageReport(Writer writer) throws IOException {
        writer.write(coverageTemplate.data(new ResultData(iri(testSuiteNamespace))).render());
        writer.flush();
    }

    public void buildHtmlResultReport(Writer writer) throws IOException {
        writer.write(resultTemplate.data(new ResultData(iri(testSuiteNamespace))).render());
        writer.flush();
    }
}

