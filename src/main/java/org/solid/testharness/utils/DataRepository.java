/*
 * MIT License
 *
 * Copyright (c) 2021 Solid
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.solid.testharness.utils;

import com.intuit.karate.StringUtils;
import com.intuit.karate.Suite;
import com.intuit.karate.core.FeatureResult;
import com.intuit.karate.core.ScenarioResult;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.RDFaParserSettings;
import org.eclipse.rdf4j.rio.helpers.RDFaVersion;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.*;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.eclipse.rdf4j.model.util.Values.*;

@ApplicationScoped
public class DataRepository implements Repository {
    private static final Logger logger = LoggerFactory.getLogger(DataRepository.class);
    private static final String GITHUB_LINE_ANCHOR = "#L";
    private static final Pattern JS_ERROR = Pattern.compile(
            "\\Rjs failed:\\R>>>>.*<<<<\\Rorg.graalvm.polyglot.PolyglotException: " +
                    "([^\\r\\n]+)\\R" +             // exception message
                    "(?:(Caused[^\\r\\n]+)\\R)?" +  // optional cause
                    "(?:([^\\r\\n]+).*)?" +         // first line of stack trace
                    "(- <js>[^\\r\\n]+).*",         // last line of stack
            Pattern.DOTALL);

    private Repository repository = new SailRepository(new MemoryStore());
    // TODO: Determine if this should be a separate IRI to the base
    private IRI assertor;
    private IRI testSubject;

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

    public void load(final URL url) {
        load(url, null);
    }

    public void load(final URL url, final String baseUri) {
        try (RepositoryConnection conn = getConnection()) {
            try {
                final IRI context = iri(url.toString());
                logger.info("Loading {} with base {}", url, baseUri);
                final ParserConfig parserConfig = new ParserConfig();
                parserConfig.set(RDFaParserSettings.RDFA_COMPATIBILITY, RDFaVersion.RDFA_1_1);
                conn.setParserConfig(parserConfig);
                conn.add(url, baseUri, null, context);
                logger.debug("Loaded data into temporary context, size={}", conn.size(context));
                try (var statements = conn.getStatements(null, SPEC.requirement, null, context)) {
                    if (statements.hasNext()) {
                        // copy the spec and requirement triples to the main graph context
                        final Resource spec = statements.next().getSubject();
                        conn.add(spec, RDF.type, DOAP.Specification);
                        try (var requirements = conn.getStatements(spec, SPEC.requirement, null, context)) {
                            requirements.stream()
                                    .peek(st -> conn.add(st, (Resource) null))
                                    .map(Statement::getObject)
                                    .filter(Value::isIRI)
                                    .map(Resource.class::cast)
                                    .forEach(req -> {
                                        try (var details = conn.getStatements(req, null, null, context)) {
                                            conn.add(details, (Resource) null);
                                        }
                                    });
                        }
                    } else {
                        // copy all statements to main graph context
                        try (var tempStatements = conn.getStatements(null, null, null, context)) {
                            conn.add(tempStatements, (Resource) null);
                        }
                    }
                }
                // remove the temporary context
                conn.clear(context);
                logger.debug("Repository size={}", conn.size());
            } catch (IOException e) {
                throw (TestHarnessInitializationException) new TestHarnessInitializationException(
                        "Failed to read data from %s: %s",
                        url.toString(), e.toString()
                ).initCause(e);
            }
        } catch (RDF4JException | UnsupportedRDFormatException e) {
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

    public void addFeatureResult(final Suite suite, final FeatureResult fr, final IRI featureIri,
                                 final FeatureFileParser featureFileParser) {
        final long startTime = suite.startTime;
        try (RepositoryConnection conn = getConnection()) {
            final IRI testCaseIri = getTestCase(conn, featureIri);
            if (testCaseIri != null) {
                conn.add(testCaseIri, DCTERMS.title, literal(fr.getFeature().getName()));
                final String featureComments = featureFileParser.getFeatureComments();
                if (featureComments != null) {
                    conn.add(testCaseIri, DCTERMS.description, literal(featureComments));
                }
            }
            createAssertion(conn, fr.isFailed() ? EARL.failed : EARL.passed,
                    new Date((long) (startTime + fr.getDurationMillis())), testCaseIri);
            for (ScenarioResult sr: fr.getScenarioResults()) {
                createScenarioActivity(conn, sr, testCaseIri, featureIri, featureFileParser);
            }
        } catch (Exception e) {
            logger.error("Failed to load feature result", e);
        }
    }

    private IRI getTestCase(final RepositoryConnection conn, final IRI featureIri) {
        return conn.getStatements(null, SPEC.testScript, featureIri).stream()
                .map(Statement::getSubject)
                .filter(Value::isIRI)
                .map(IRI.class::cast)
                .findFirst().orElse(null);
    }

    public void createAssertion(final RepositoryConnection conn, final Value outcome, final Date date,
                                 final IRI testCaseIri) {
        final IRI featureAssertion = createNode();
        final ModelBuilder builder = new ModelBuilder();
        final IRI featureResult = createNode();
        conn.add(builder.subject(featureAssertion)
                .add(RDF.type, EARL.Assertion)
                .add(EARL.assertedBy, assertor)
                .add(EARL.subject, testSubject)
                .add(EARL.mode, EARL.automatic)
                .add(EARL.result, featureResult)
                .add(featureResult, RDF.type, EARL.TestResult)
                .add(featureResult, EARL.outcome, outcome)
                .add(featureResult, DCTERMS.date, date)
                .build());
        if (testCaseIri != null) {
            conn.add(featureAssertion, EARL.test, testCaseIri);
        }
    }

    private void createScenarioActivity(final RepositoryConnection conn, final ScenarioResult sr,
                                        final IRI testCaseIri, final IRI featureIri,
                                        final FeatureFileParser featureFileParser) {
        final ModelBuilder builder;
        final IRI scenarioIri = createNode();
        final IRI scenarioResultIri = createNode();
        builder = new ModelBuilder();
        builder.subject(scenarioIri)
                .add(RDF.type, PROV.Activity)
                .add(DCTERMS.title, sr.getScenario().getName())
                .add(PROV.used, iri(featureIri.stringValue() + GITHUB_LINE_ANCHOR + sr.getScenario().getLine()))
                .add(PROV.startedAtTime, new Date(sr.getStartTime()))
                .add(PROV.endedAtTime, new Date(sr.getEndTime()))
                .add(PROV.generated, scenarioResultIri)
                .add(scenarioResultIri, RDF.type, PROV.Entity)
                .add(scenarioResultIri, PROV.generatedAtTime, new Date(sr.getEndTime()))
                .add(scenarioResultIri, PROV.value, sr.isFailed() ? EARL.failed : EARL.passed);
        final String scenarioComments = featureFileParser.getScenarioComments(sr.getScenario().getSection().getIndex());
        if (!StringUtils.isBlank(scenarioComments)) {
            conn.add(scenarioIri, DCTERMS.description, literal(scenarioComments));
        }
        conn.add(builder.build());
        if (testCaseIri != null) {
            conn.add(testCaseIri, DCTERMS.hasPart, scenarioIri);
        }
        if (!sr.getStepResults().isEmpty()) {
            createStepActivityList(conn, sr, scenarioIri, featureIri);
        }
    }

    private void createStepActivityList(final RepositoryConnection conn, final ScenarioResult sr,
                                        final IRI scenarioIri, final IRI featureIri) {
        final List<Resource> steps = sr.getStepResults().stream().map(str -> {
            final IRI stepIri = createNode();
            final IRI stepResultIri = createNode();
            final ModelBuilder stepBuilder = new ModelBuilder();
            stepBuilder.subject(stepIri)
                    .add(RDF.type, PROV.Activity)
                    .add(DCTERMS.title, str.getStep().getPrefix() + " " + str.getStep().getText())
                    .add(PROV.used, iri(featureIri.stringValue() + GITHUB_LINE_ANCHOR + str.getStep().getLine()))
                    .add(PROV.generated, stepResultIri)
                    .add(stepResultIri, RDF.type, PROV.Entity)
                    .add(stepResultIri, PROV.value, EARL_RESULT.get(str.getResult().getStatus()));
            if (str.getStep().getComments() != null) {
                stepBuilder.add(stepIri, DCTERMS.description,
                        str.getStep().getComments().stream().collect(Collectors.joining("\n")));
            }
            if (!str.getStepLog().isEmpty()) {
                stepBuilder.add(stepResultIri, DCTERMS.description, simplify(str.getStepLog()));
            }
            if (!str.getStep().isBackground()) {
                stepBuilder.add(stepIri, PROV.wasInformedBy, scenarioIri);
            }
            conn.add(stepBuilder.build());
            return stepIri;
        }).collect(Collectors.toList());
        final Resource head = bnode();
        final Model stepList = RDFCollections.asRDF(steps, head, new LinkedHashModel());
        stepList.add(scenarioIri, DCTERMS.hasPart, head);
        // remove the list type as it is inferred anyway and RDFa @inlist does not generate it
        stepList.remove(head, RDF.type, RDF.List);
        conn.add(stepList);
    }

    private IRI createNode() {
        return iri(Namespaces.RESULTS_URI, bnode().getID());
    }

    private String simplify(final String data) {
        // strip out unnecessary logging and remove blank lines
        return JS_ERROR.matcher(data).replaceFirst("\n$1\n$2\n$3\n$4").replaceAll("\\R+", "\n");
    }

    public void export(final Writer wr) throws Exception {
        final RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, wr);
        try (RepositoryConnection conn = getConnection()) {
            rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true)
                    .set(BasicWriterSettings.INLINE_BLANK_NODES, true);
            conn.export(rdfWriter);
        } catch (RDF4JException e) {
            throw new Exception("Failed to write repository", e);
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
