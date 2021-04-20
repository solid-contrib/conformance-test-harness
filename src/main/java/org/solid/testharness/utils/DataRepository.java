package org.solid.testharness.utils;

import com.intuit.karate.Suite;
import com.intuit.karate.core.FeatureResult;
import com.intuit.karate.core.ScenarioResult;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.vocabulary.RDF;
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
import org.solid.common.vocab.DCTERMS;
import org.solid.common.vocab.EARL;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.io.*;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.eclipse.rdf4j.model.util.Values.bnode;
import static org.eclipse.rdf4j.model.util.Values.iri;

@ApplicationScoped
public class DataRepository implements Repository {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.utils.DataRepository");

    private Repository repository = new SailRepository(new MemoryStore());
    // TODO: Determine if this should be a separate IRI to the base
    private IRI assertor = iri(Namespaces.TEST_HARNESS_URI);
    private IRI testSubject;

    public static final String GENID = "/genid/";

    public static Map<String, IRI> EARL_RESULT = Map.of("passed", EARL.passed, "failed", EARL.failed, "skipped", EARL.untested);

    @PostConstruct
    void postConstruct() {
        Namespaces.addToRepository(repository);
        logger.debug("INITIALIZE DATA REPOSITORY");
    }

    public void loadTurtle(URL url) {
        try (RepositoryConnection conn = getConnection()) {
            try {
                conn.add(url, RDFFormat.TURTLE);
                logger.debug("Loaded data into repository, size={}", conn.size());
            } catch (IOException e) {
                logger.error("Failed to read data from {}: {}", url.toString(), e.getMessage());
            }
        } catch (RDF4JException e) {
            logger.error("Failed to parse data from {}: {}", url.toString(), e.getMessage());
        }
    }

    public void loadRdfa(Reader reader, String baseUri) {
        try (RepositoryConnection conn = getConnection()) {
            conn.add(reader, baseUri, RDFFormat.RDFA);
            logger.debug("Loaded data into repository, size={}", conn.size());
        } catch (IOException | RDF4JException e) {
            logger.error("Failed to parse data", e.getMessage());
        }
    }

    public void addFeatureResult(Suite suite, FeatureResult fr) {
        long startTime = suite.startTime;
        try (RepositoryConnection conn = getConnection()) {
            ModelBuilder builder = new ModelBuilder();
            IRI featureIri = iri(Namespaces.TEST_HARNESS_URI, fr.getDisplayName());
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
        } catch (Exception e) {
            logger.error("Failed to load feature result", e.getMessage());
        }
    }

    private IRI createSkolemizedBlankNode(IRI base) {
        return iri(base.stringValue() + GENID + bnode().getID());
    }

    public void export(Writer wr) throws Exception {
        try (RepositoryConnection conn = getConnection()) {
            RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, wr);
            rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true).set(BasicWriterSettings.INLINE_BLANK_NODES, true);
            conn.export(rdfWriter);
        } catch (RDF4JException e) {
            throw new Exception("Failed to write repository: " + e.getMessage());
        }
    }

    public void export(OutputStream os) throws Exception {
        try (RepositoryConnection conn = getConnection()) {
            RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, os);
            rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true).set(BasicWriterSettings.INLINE_BLANK_NODES, true);
            conn.export(rdfWriter);
        } catch (RDF4JException e) {
            throw new Exception("Failed to write repository: " + e.getMessage());
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
