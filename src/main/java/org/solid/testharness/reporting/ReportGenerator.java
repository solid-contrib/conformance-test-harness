package org.solid.testharness.reporting;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.DOAP;
import org.solid.testharness.utils.DataRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

@ApplicationScoped
public class ReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ReportGenerator.class);

    @Inject
    DataRepository dataRepository;

    @Location("coverage-report.html")
    Template coverageTemplate;
    @Location("result-report.html")
    Template resultTemplate;


    public void buildTurtleReport(Writer writer) throws Exception {
        dataRepository.export(writer);
    }

    public void printReportToConsole() throws Exception {
        StringWriter sw = new StringWriter();
        dataRepository.export(sw);
        logger.info("REPORT\n{}", sw);
    }

    public void buildHtmlCoverageReport(Writer writer) throws IOException {
        IRI testSuite = getTestSuiteNamespace();
        writer.write(coverageTemplate.data(new ResultData(testSuite)).render());
        writer.flush();
    }

    public void buildHtmlResultReport(Writer writer) throws IOException {
        IRI testSuite = getTestSuiteNamespace();
        writer.write(resultTemplate.data(new ResultData(testSuite)).render());
        writer.flush();
    }

    private IRI getTestSuiteNamespace() {
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            RepositoryResult<Statement> statements = conn.getStatements(null, DOAP.implements_, null);
            // TODO: if we use multiple test suites this will need to iterate
            if (statements.hasNext()) {
                Statement st = statements.next();
                return st.getSubject().isIRI() ? (IRI) st.getSubject() : null;
            }
            return null;
        }
    }
}

