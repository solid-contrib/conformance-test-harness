package org.solid.testharness.utils;

import com.intuit.karate.Suite;
import com.intuit.karate.core.FeatureResult;
import com.intuit.karate.core.ScenarioResult;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.eclipse.rdf4j.model.util.Values.bnode;
import static org.eclipse.rdf4j.model.util.Values.iri;

@ApplicationScoped
public class DataRepository implements Repository { //extends SailRepository
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.utils.DataRepository");

    private Repository repository;

    private DataRepository() {
        repository = new SailRepository(new MemoryStore());
        logger.debug("INITIALIZE DATA REPOSITORY");
    }
    private Namespace NS;
    private IRI assertor;
    private IRI testSubject;

    public static final String GENID = "/genid/";

    public static Map<String, IRI> EARL_RESULT = Map.of("passed", EARL.passed, "failed", EARL.failed, "skipped", EARL.untested);

    public void loadTurtle(URL url) {
        try (RepositoryConnection conn = getConnection()) {
            try {
                conn.add(url, RDFFormat.TURTLE);
            } catch (IOException e) {
                logger.error("Failed to read " + url, e);
            }
        } catch (RDF4JException e) {
            logger.error("Failed to parse data from " + url, e);
        }
    }

    public void loadTurtle(File file) {
        try (RepositoryConnection conn = getConnection()) {
            try {
                conn.add(file, RDFFormat.TURTLE);
            } catch (IOException e) {
                logger.error("Failed to read " + file, e);
            }
        } catch (RDF4JException e) {
            logger.error("Failed to parse data from " + file, e);
        }
    }

    public void setupNamespaces(String baseUri) {
        // TODO: Determine if this should be a separate IRI to the base
        this.assertor = iri(baseUri);
        this.NS = new SimpleNamespace("test-harness", baseUri);
        try (RepositoryConnection conn = getConnection()) {
            conn.setNamespace(NS.getPrefix(), NS.getName());
            conn.setNamespace(EARL.PREFIX, EARL.NAMESPACE);
            conn.setNamespace(DOAP.PREFIX, DOAP.NAMESPACE);
            conn.setNamespace(SOLID.PREFIX, SOLID.NAMESPACE);
            conn.setNamespace(SOLID_TEST.PREFIX, SOLID_TEST.NAMESPACE);
            conn.setNamespace(DCTERMS.PREFIX, DCTERMS.NAMESPACE);
            conn.setNamespace(XSD.PREFIX, XSD.NAMESPACE);
        } catch (RDF4JException e) {
            logger.error("Failed to setup namespaces", e);
        }
    }

    public void addFeatureResult(Suite suite, FeatureResult fr) {
        long startTime = suite.startTime;
        try (RepositoryConnection conn = getConnection()) {
            ModelBuilder builder = new ModelBuilder();
            IRI featureIri = iri(NS, fr.getDisplayName());
            IRI featureAssertion = createSkolemizedBlankNode(featureIri);
            IRI featureResult = createSkolemizedBlankNode(featureIri);
            conn.add(builder.subject(featureIri)
                    .add(RDF.TYPE, EARL.TestCriterion)
                    .add(RDF.TYPE, EARL.TestFeature)
                    .add(DCTERMS.title, fr.getFeature().getName())
                    .add(EARL.assertions, featureAssertion)
                    .add(featureAssertion, RDF.TYPE, EARL.Assertion)
                    .add(featureAssertion, EARL.assertedBy, assertor)
                    .add(featureAssertion, EARL.test, featureIri)
                    .add(featureAssertion, EARL.subject, testSubject)
                    .add(featureAssertion, EARL.mode, EARL.automatic)
                    .add(featureAssertion, EARL.result, featureResult)
                    .add(featureResult, RDF.TYPE, EARL.TestResult)
                    .add(featureResult, EARL.outcome, fr.isFailed() ? EARL.failed : EARL.passed)
                    .add(featureResult, DCTERMS.date, new Date((long) (startTime + fr.getDurationMillis())))
                    .build());
            for (ScenarioResult sr: fr.getScenarioResults()) {
                IRI scenarioIri = createSkolemizedBlankNode(featureIri);
                IRI scenarioAssertion = createSkolemizedBlankNode(featureIri);
                IRI scenarioResult = createSkolemizedBlankNode(featureIri);
                builder = new ModelBuilder();
                conn.add(builder.subject(scenarioIri)
                        .add(RDF.TYPE, EARL.TestCriterion)
                        .add(RDF.TYPE, EARL.TestCase)
                        .add(DCTERMS.title, sr.getScenario().getName())
                        .add(DCTERMS.isPartOf, featureIri)
                        .add(EARL.assertions, scenarioAssertion)
                        .add(scenarioAssertion, RDF.TYPE, EARL.Assertion)
                        .add(scenarioAssertion, EARL.assertedBy, assertor)
                        .add(scenarioAssertion, EARL.test, scenarioIri)
                        .add(scenarioAssertion, EARL.subject, testSubject)
                        .add(scenarioAssertion, EARL.mode, EARL.automatic)
                        .add(scenarioAssertion, EARL.result, scenarioResult)
                        .add(scenarioResult, RDF.TYPE, EARL.TestResult)
                        .add(scenarioResult, EARL.outcome, sr.isFailed() ? EARL.failed : EARL.passed)
                        .add(scenarioResult, DCTERMS.date, new Date((long) (startTime + sr.getDurationMillis())))
                        .add(featureIri, DCTERMS.hasPart, scenarioIri)
                        .build());
                if (!sr.getStepResults().isEmpty()) {
                    List<Resource> steps = sr.getStepResults().stream().map(str -> {
                        IRI step = createSkolemizedBlankNode(featureIri);
                        ModelBuilder stepBuilder = new ModelBuilder();
                        stepBuilder.subject(step)
                                .add(RDF.TYPE, EARL.TestStep)
                                .add(DCTERMS.title, str.getStep().getPrefix() + " " + str.getStep().getText())
                                .add(EARL.outcome, EARL_RESULT.get(str.getResult().getStatus()));
                        if (!str.getStepLog().isEmpty()) {
                            stepBuilder.add(EARL.info, str.getStepLog());
                        }
                        conn.add(stepBuilder.build());
                        return step;
                    }).collect(Collectors.toList());
                    Resource head = createSkolemizedBlankNode(featureIri);
                    Model stepList = RDFCollections.asRDF(steps, head, new LinkedHashModel());
                    stepList.add(scenarioIri, EARL.steps, head);
                    conn.add(stepList);
                }
            }
        } catch (RDF4JException e) {
            logger.error("Failed to load feature result", e);
        }
    }

/* Example of a manifest:
@prefix manifest: <http://www.w3.org/2013/TurtleTests/manifest.ttl#>.
manifest:IRI_subject
    a earl:TestCriterion, earl:TestCase;
    dc:title "IRI_subject";
    dc:description "IRI subject";
    mf:action <http://www.w3.org/2013/TurtleTests/IRI_subject.ttl>;
    mf:result <http://www.w3.org/2013/TurtleTests/IRI_spo.nt>;
    earl:assertions _:assertions0.
 */


    private IRI createSkolemizedBlankNode(IRI base) {
        return iri(base.stringValue() + GENID + bnode().getID());
    }

    public void export(Writer wr) {
        try (RepositoryConnection conn = getConnection()) {
            RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, wr);
            rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true).set(BasicWriterSettings.INLINE_BLANK_NODES, true);
            conn.export(rdfWriter);
        } catch (RDF4JException e) {
            logger.error("Failed to generate test suite result report", e);
        }
    }

    public void export(OutputStream os) {
        try (RepositoryConnection conn = getConnection()) {
            RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, os);
            rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true).set(BasicWriterSettings.INLINE_BLANK_NODES, true);
            conn.export(rdfWriter);
        } catch (RDF4JException e) {
            logger.error("Failed to generate test suite result report", e);
        }
    }

    public void setTestSubject(IRI testSubject) {
        this.testSubject = testSubject;
    }

    @Override
    public void setDataDir(File dataDir) {
        repository.setDataDir(dataDir);
    }

    @Override
    public File getDataDir() {
        return repository.getDataDir();
    }

    @Override
    public void initialize() throws RepositoryException {
        repository.initialize();
    }

    @Override
    public boolean isInitialized() {
        return repository.isInitialized();
    }

    @Override
    public void shutDown() throws RepositoryException {
        repository.shutDown();
    }

    @Override
    public boolean isWritable() throws RepositoryException {
        return repository.isWritable();
    }

    @Override
    public RepositoryConnection getConnection() throws RepositoryException {
        return repository.getConnection();
    }

    @Override
    public ValueFactory getValueFactory() {
        return repository.getValueFactory();
    }
}
