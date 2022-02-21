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
import com.intuit.karate.core.*;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
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
import org.solid.testharness.reporting.Scores;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.*;
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

    private final Repository repository = new SailRepository(new MemoryStore());
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
                        // copy the spec and requirement triples to the spec-related graph context
                        final Resource spec = statements.next().getSubject();
                        conn.add(spec, RDF.type, DOAP.Specification, Namespaces.SPEC_RELATED_CONTEXT);
                        try (var requirements = conn.getStatements(spec, SPEC.requirement, null, context)) {
                            requirements.stream()
                                    .peek(st -> conn.add(st, Namespaces.SPEC_RELATED_CONTEXT))
                                    .map(Statement::getObject)
                                    .filter(Value::isIRI)
                                    .map(Resource.class::cast)
                                    .forEach(req -> {
                                        try (var details = conn.getStatements(req, null, null, context)) {
                                            conn.add(details, Namespaces.SPEC_RELATED_CONTEXT);
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

    public void identifySpecifications() {
        try (
                RepositoryConnection conn = getConnection();
                var statements = conn.getStatements(null, RDF.type, DOAP.Specification)
        ) {
            while (statements.hasNext()) {
                Namespaces.addSpecification((IRI) statements.next().getSubject());
            }
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
            final Set<FeatureSection> sections = new HashSet<>();
            final ScenarioData scenarioData = new ScenarioData();
            for (ScenarioResult sr: fr.getScenarioResults()) {
                createScenarioActivity(conn, fr, sr, scenarioData.fromScenario(sr.getScenario()),
                        testCaseIri, featureIri, featureFileParser);
                sections.add(sr.getScenario().getSection());
            }
            // find scenario sections which were not run (i.e. have no results)
            final List<FeatureSection> otherSections = fr.getFeature().getSections().stream()
                    .filter(s -> !sections.contains(s))
                    .collect(Collectors.toList());
            for (FeatureSection section: otherSections) {
                createScenarioActivity(conn, fr, null, scenarioData.fromFeatureSection(section),
                        testCaseIri, featureIri, featureFileParser);
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

    public void createUntestedAssertion(final Feature feature, final String featurePath) {
        try (
                RepositoryConnection conn = getConnection();
                var statements = conn.getStatements(null, SPEC.testScript, iri(featurePath))
        ) {
            if (statements.hasNext()) {
                final IRI testCaseIri = (IRI) statements.next().getSubject();
                // add assertion
                createAssertion(conn, EARL.inapplicable, new Date(), testCaseIri);
                conn.add(testCaseIri, DCTERMS.title, literal(feature.getName()));
            }
        }
    }

    private void createScenarioActivity(final RepositoryConnection conn, final FeatureResult fr,
                                        final ScenarioResult sr, final ScenarioData sc,
                                        final IRI testCaseIri, final IRI featureIri,
                                        final FeatureFileParser featureFileParser) {
        final ModelBuilder builder;
        final IRI scenarioIri = createNode();
        final IRI scenarioResultIri = createNode();
        builder = new ModelBuilder();
        builder.subject(scenarioIri)
                .add(RDF.type, PROV.Activity)
                .add(DCTERMS.title, sc.getName())
                .add(PROV.used, iri(featureIri.stringValue() + GITHUB_LINE_ANCHOR + sc.getLine()))
                .add(PROV.generated, scenarioResultIri)
                .add(scenarioResultIri, RDF.type, PROV.Entity)
                .add(scenarioResultIri, PROV.generatedAtTime, sr != null ? new Date(sr.getEndTime()) : new Date());
        if (sr != null) {
            builder.subject(scenarioIri)
                    .add(PROV.startedAtTime, new Date(sr.getStartTime()))
                    .add(PROV.endedAtTime, new Date(sr.getEndTime()))
                    .add(scenarioResultIri, PROV.value, sr.isFailed() ? EARL.failed : EARL.passed);
        } else {
            final boolean ignored = sc.getTags().stream().anyMatch(tag -> tag.getName().equals(Tag.IGNORE));
            builder.subject(scenarioIri)
                    .add(scenarioResultIri, PROV.value, ignored ? EARL.untested : EARL.inapplicable);
        }
        final String scenarioComments = featureFileParser.getScenarioComments(sc.getSection().getIndex());
        if (!StringUtils.isBlank(scenarioComments)) {
            conn.add(scenarioIri, DCTERMS.description, literal(scenarioComments));
        }
        conn.add(builder.build());
        if (testCaseIri != null) {
            conn.add(testCaseIri, DCTERMS.hasPart, scenarioIri);
        }
        if (sr != null && !sr.getStepResults().isEmpty()) {
            createStepActivityList(conn, fr, sr, scenarioIri, featureIri);
        }
    }

    private void createStepActivityList(final RepositoryConnection conn, final FeatureResult fr,
                                        final ScenarioResult sr, final IRI scenarioIri, final IRI featureIri) {
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
                        String.join("\n", str.getStep().getComments()));
            }
            if (!str.getStepLog().isEmpty()) {
                final String log;
                if (str.getStepLog().contains("callonce lock:")) {
                    // the step log is in another scenario result so copy it here
                    log = fr.getScenarioResults().stream()
                            .flatMap(s -> s.getStepResults().stream())
                            .filter(s -> s.getStep() == str.getStep() && s.getStepLog().contains("lock acquired"))
                            .map(StepResult::getStepLog)
                            .findFirst()
                            .map(s -> str.getStepLog() + s.substring(s.indexOf('\n') + 1))
                            .orElse("");
                } else {
                    log = str.getStepLog();
                }
                stepBuilder.add(stepResultIri, DCTERMS.description, simplify(log));
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

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public Map<String, Scores> getOutcomeCounts() {
        final Map<String, Scores> counts = new HashMap<>();
        try (
                RepositoryConnection conn = getConnection()
        ) {
            final String queryString = Namespaces.generateTurtlePrefixes(List.of(SPEC.PREFIX, EARL.PREFIX)) +
                    "SELECT ?level ?outcome (COUNT(?outcome) AS ?count) " +
                    "WHERE {" +
                    "  ?r spec:requirementLevel ?level ." +
                    "  ?t spec:requirementReference ?r ." +
                    "  ?a earl:test ?t ;" +
                    "     earl:result/earl:outcome ?outcome ." +
                    "}" +
                    "GROUP BY ?level ?outcome";
            final TupleQuery tupleQuery = conn.prepareTupleQuery(queryString);
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                while (result.hasNext()) {
                    final BindingSet bindingSet = result.next();
                    final String level = ((IRI)bindingSet.getValue("level")).getLocalName();
                    final String outcome = ((IRI)bindingSet.getValue("outcome")).getLocalName();
                    final int count = Integer.parseInt(bindingSet.getValue("count").stringValue());
                    Scores scores = counts.get(level);
                    if (scores == null) {
                        scores = new Scores();
                        counts.put(level, scores);
                    }
                    scores.setScore(outcome, count);
                }
            }
        }
        return counts;
    }

    public void export(final Writer wr, final Resource... contexts) throws Exception {
        final RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, wr);
        try (RepositoryConnection conn = getConnection()) {
            rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true)
                    .set(BasicWriterSettings.INLINE_BLANK_NODES, true);
            conn.export(rdfWriter, contexts);
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

    private static class ScenarioData {
        private FeatureSection section;
        private int line;
        private List<Tag> tags;
        private String name;

        public ScenarioData fromScenario(final Scenario scenario) {
            section = scenario.getSection();
            line = scenario.getLine();
            tags = scenario.getTags();
            name = scenario.getName();
            return this;
        }

        public ScenarioData fromFeatureSection(final FeatureSection featureSection) {
            if (featureSection.getScenario() != null) {
                section = featureSection.getScenario().getSection();
                line = featureSection.getScenario().getLine();
                tags = featureSection.getScenario().getTags();
                name = featureSection.getScenario().getName();
            } else {
                section = featureSection.getScenarioOutline().getSection();
                line = featureSection.getScenarioOutline().getLine();
                tags = featureSection.getScenarioOutline().getTags();
                name = featureSection.getScenarioOutline().getName();
            }
            return this;
        }

        public FeatureSection getSection() {
            return section;
        }

        public int getLine() {
            return line;
        }

        public List<Tag> getTags() {
            return tags;
        }

        public String getName() {
            return name;
        }
    }
}
