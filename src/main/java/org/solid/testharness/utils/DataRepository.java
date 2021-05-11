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
public class DataRepository implements Repository {
    private static final Logger logger = LoggerFactory.getLogger(DataRepository.class);

    private Repository repository = new SailRepository(new MemoryStore());
    // TODO: Determine if this should be a separate IRI to the base
    private IRI assertor;
    private IRI testSubject;

    public static final String GENID = "/genid/";

    public static Map<String, IRI> EARL_RESULT = Map.of(
            "passed", EARL.passed,
            "failed", EARL.failed,
            "skipped", EARL.untested
    );

    @PostConstruct
    void postConstruct() {
        Namespaces.addToRepository(repository);
        logger.debug("INITIALIZE DATA REPOSITORY");
    }

    public void loadTurtle(final URL url) {
        loadData(url, null, RDFFormat.TURTLE);
    }

    public void loadRdfa(final URL url, final String baseUri) {
        loadData(url, baseUri, RDFFormat.RDFA);
    }

    public void loadData(final URL url, final String baseUri, final RDFFormat format) {
        try (RepositoryConnection conn = getConnection()) {
            try {
                if (logger.isInfoEnabled()) {
                    logger.info("Loading {} from {}", format.getName(), url.toString());
                }
                conn.add(url, baseUri, format);
                if (logger.isDebugEnabled()) {
                    logger.debug("Loaded data into repository, size={}", conn.size());
                }
            } catch (IOException e) {
                throw (TestHarnessInitializationException) new TestHarnessInitializationException(
                        "Failed to read data from %s: %s",
                        url.toString(), e.toString()
                ).initCause(e);
            }
        } catch (RDF4JException e) {
            throw (TestHarnessInitializationException) new TestHarnessInitializationException(
                    "Failed to parse data: %s", e.toString()
            ).initCause(e);
        }
    }

    public void setTestSubject(final IRI testSubject) {
        this.testSubject = testSubject;
    }

    public void setAssertor(final IRI assertor) {
        this.assertor = assertor;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    // This method creates an unknown number of objects in a loop dependent on the test cases
    public void addFeatureResult(final Suite suite, final FeatureResult fr, final IRI featureIri) {
        final long startTime = suite.startTime;
        try (RepositoryConnection conn = getConnection()) {
            ModelBuilder builder = new ModelBuilder();
            final IRI featureAssertion = createSkolemizedBlankNode(featureIri);
            final IRI featureResult = createSkolemizedBlankNode(featureIri);
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
                final IRI scenarioIri = createSkolemizedBlankNode(featureIri);
                final IRI scenarioAssertion = createSkolemizedBlankNode(featureIri);
                final IRI scenarioResult = createSkolemizedBlankNode(featureIri);
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
                    final List<Resource> steps = sr.getStepResults().stream().map(str -> {
                        final IRI step = createSkolemizedBlankNode(featureIri);
                        final ModelBuilder stepBuilder = new ModelBuilder();
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
                    final Resource head = createSkolemizedBlankNode(featureIri);
                    final Model stepList = RDFCollections.asRDF(steps, head, new LinkedHashModel());
                    stepList.add(scenarioIri, EARL.steps, head);
                    conn.add(stepList);
                }
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Failed to load feature result: {}", e.toString());
            }
        }
    }

    private IRI createSkolemizedBlankNode(final IRI base) {
        return iri(base.stringValue() + GENID + bnode().getID());
    }

    public void export(final Writer wr) throws Exception {
        export(Rio.createWriter(RDFFormat.TURTLE, wr));
    }

    public void export(final OutputStream os) throws Exception {
        export(Rio.createWriter(RDFFormat.TURTLE, os));
    }

    private void export(final RDFWriter rdfWriter) throws Exception {
        try (RepositoryConnection conn = getConnection()) {
            rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true)
                    .set(BasicWriterSettings.INLINE_BLANK_NODES, true);
            conn.export(rdfWriter);
        } catch (RDF4JException e) {
            throw (Exception) new Exception("Failed to write repository: " + e).initCause(e);
        }
    }

    @Override
    public void setDataDir(final File dataDir) {
        repository.setDataDir(dataDir);
    }

    @Override
    public File getDataDir() {
        return repository.getDataDir();
    }

    @SuppressWarnings("deprecation")
    // Ignore warning as we have to override this to complete the interface
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
