/*
 * MIT License
 *
 * Copyright (c) 2019 - 2022 W3C Solid Community Group
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

import com.intuit.karate.Suite;
import com.intuit.karate.core.*;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.*;
import org.solid.testharness.reporting.Scores;
import org.solid.testharness.reporting.TestSuiteResults;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.eclipse.rdf4j.model.util.Values.*;

@ApplicationScoped
public class DataRepository implements Repository {
    private static final Logger logger = LoggerFactory.getLogger(DataRepository.class);
    private static final String GITHUB_LINE_ANCHOR = "#L";
    private static final String POLYGLOT_EXCEPTION = "org.graalvm.polyglot.PolyglotException: ";

    private final Repository repository = new SailRepository(new MemoryStore());

    @Inject
    GraalRdfaParser graalRdfaParser;

    /**
     * Sets the RDFa parser (for testing when CDI is not available).
     */
    void setGraalRdfaParser(final GraalRdfaParser parser) {
        this.graalRdfaParser = parser;
    }

    // TODO: Determine if this should be a separate IRI to the base
    private IRI assertor;
    private IRI testSubject;
    private List<String> failingScenarios;

    public static final Map<String, IRI> EARL_RESULT = Map.of(
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
        try (var conn = getConnection()) {
            final var context = iri(url.toString());
            logger.info("Loading {} with base {}", url, baseUri);

            // Check if URL points to RDFa content (HTML/XHTML)
            final var urlPath = url.getPath().toLowerCase();
            final var isRdfa = urlPath.endsWith(".html") || urlPath.endsWith(".xhtml")
                    || urlPath.endsWith(".htm");

            if (isRdfa) {
                // Use GraalJS-based RDFa parser
                loadRdfaDocument(conn, url, baseUri, context);
            } else {
                // Use standard Rio parser for other formats
                conn.add(url, baseUri, null, context);
            }
            logger.debug("Loaded data into temporary context, size={}", conn.size(context));
            try (var statements = conn.getStatements(null, SPEC.requirement, null, context)) {
                if (statements.hasNext()) {
                    // copy the spec and requirement triples to the spec-related graph context
                    final var spec = statements.next().getSubject();
                    conn.add(spec, RDF.type, DOAP.Specification, Namespaces.SPEC_RELATED_CONTEXT);
                    try (var requirements = conn.getStatements(spec, SPEC.requirement, null, context)) {
                        requirements.stream().forEach(st -> conn.add(st, Namespaces.SPEC_RELATED_CONTEXT));
                    }
                    try (var requirements = conn.getStatements(spec, SPEC.requirement, null, context)) {
                        requirements.stream()
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
        } catch (IOException | RDF4JException | UnsupportedRDFormatException e) {
            throw new TestHarnessInitializationException("Failed to read data from [" + url + "]", e);
        }
    }

    /**
     * Load an RDFa document using the GraalJS-based parser.
     */
    private void loadRdfaDocument(final RepositoryConnection conn, final URL url,
                                  final String baseUri, final IRI context) throws IOException {
        // Fetch the document content
        final var connection = url.openConnection();
        final var contentType = connection.getContentType();
        final String effectiveContentType;
        if (contentType == null) {
            effectiveContentType = "text/html";
        } else if (contentType.contains(";")) {
            effectiveContentType = contentType.substring(0, contentType.indexOf(';')).trim();
        } else {
            effectiveContentType = contentType;
        }

        final String content;
        try (final var is = connection.getInputStream()) {
            content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Parse with GraalRdfaParser
        final var effectiveBaseUri = baseUri != null ? baseUri : url.toString();
        final var model = graalRdfaParser.parse(content, effectiveBaseUri, effectiveContentType);

        // Add statements to the repository in the given context
        conn.add(model, context);
        logger.debug("Loaded {} RDFa statements from {}", model.size(), url);
    }

    public void identifySpecifications() {
        try (
                var conn = getConnection();
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

    public void setFailingScenarios(final List<String> failingScenarios) {
        this.failingScenarios = failingScenarios;
    }

    public void addFeatureResult(final Suite suite, final FeatureResult fr, final IRI featureIri,
                                 final FeatureFileParser featureFileParser) {
        final var startTime = suite.startTime;
        try (var conn = getConnection()) {
            final var testCaseIri = getTestCase(conn, featureIri);
            if (testCaseIri != null) {
                conn.add(testCaseIri, DCTERMS.title, literal(fr.getFeature().getName()));
                final var featureComments = featureFileParser.getFeatureComments();
                if (featureComments != null) {
                    conn.add(testCaseIri, DCTERMS.description, literal(featureComments));
                }
            }
            final var sections = new HashSet<FeatureSection>();
            final var scenarioData = new ScenarioData();
            final var scores = new Scores();
            // find results from reportable scenarios (not @setup)
            final var resultSections = fr.getScenarioResults()
                    .stream()
                    .filter(s -> isReportableScenario(s.getScenario()))
                    .toList();
            for (var sr: resultSections) {
                final var outcome = createScenarioActivity(conn, fr, sr, scenarioData.fromScenario(sr.getScenario()),
                        testCaseIri, featureIri, featureFileParser);
                sections.add(sr.getScenario().getSection());
                scores.incrementScore(outcome.getLocalName());
            }
            // find scenario sections (not @setup) which were not run (i.e. have no results)
            final var otherSections = fr.getFeature()
                    .getSections()
                    .stream()
                    .filter(s -> !sections.contains(s))
                    .filter(s -> s.getScenarioOutline() != null ||
                            isReportableScenario(s.getScenario()))
                    .toList();
            for (FeatureSection section: otherSections) {
                final var outcome = createScenarioActivity(conn, fr, null, scenarioData.fromFeatureSection(section),
                        testCaseIri, featureIri, featureFileParser);
                scores.incrementScore(outcome.getLocalName());
            }
            createAssertion(conn, scores.getOutcome(), new Date((long) (startTime + fr.getDurationMillis())),
                    testCaseIri);
        } catch (Exception e) {
            logger.error("Failed to load feature result", e);
        }
    }

    private boolean isReportableScenario(final Scenario scenario) {
        return scenario.getTags() == null ||
                scenario.getTags()
                .stream()
                .noneMatch(tag -> Tag.SETUP.equals(tag.getName()));
    }

    private IRI getTestCase(final RepositoryConnection conn, final IRI featureIri) {
        try (
                var statements = conn.getStatements(null, SPEC.testScript, featureIri)
        ) {
            return statements.stream()
                    .map(Statement::getSubject)
                    .filter(Value::isIRI)
                    .map(IRI.class::cast)
                    .findFirst().orElse(null);
        }
    }

    public void createAssertion(final RepositoryConnection conn, final Value outcome, final Date date,
                                 final IRI testCaseIri) {
        final var featureAssertion = createNode();
        final var builder = new ModelBuilder();
        final var featureResult = createNode();
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

    public void createSkippedAssertion(final Feature feature, final String featurePath, final IRI outcome) {
        try (
                var conn = getConnection();
                var statements = conn.getStatements(null, SPEC.testScript, iri(featurePath))
        ) {
            if (statements.hasNext()) {
                final var testCaseIri = (IRI) statements.next().getSubject();
                // add assertion
                createAssertion(conn, outcome, new Date(), testCaseIri);
                conn.add(testCaseIri, DCTERMS.title, literal(feature.getName()));
            }
        }
    }

    private IRI createScenarioActivity(final RepositoryConnection conn, final FeatureResult fr,
                                        final ScenarioResult sr, final ScenarioData sc,
                                        final IRI testCaseIri, final IRI featureIri,
                                        final FeatureFileParser featureFileParser) {
        final var scenarioIri = createNode();
        final var scenarioResultIri = createNode();
        final IRI outcome;
        final var builder = new ModelBuilder();
        builder.subject(scenarioIri)
                .add(RDF.type, PROV.Activity)
                .add(DCTERMS.title, sc.getName())
                .add(PROV.used, iri(featureIri.stringValue() + GITHUB_LINE_ANCHOR + sc.getLine()))
                .add(PROV.generated, scenarioResultIri)
                .add(scenarioResultIri, RDF.type, PROV.Entity)
                .add(scenarioResultIri, PROV.generatedAtTime, sr != null ? new Date(sr.getEndTime()) : new Date());
        outcome = addOutcomeToScenario(sr, sc, builder, scenarioIri, scenarioResultIri);
        final var scenarioComments = featureFileParser.getScenarioComments(sc.getSection().getIndex());
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
        return outcome;
    }

    private IRI addOutcomeToScenario(final ScenarioResult sr, final ScenarioData sc, final ModelBuilder builder,
                                     final IRI scenarioIri, final IRI scenarioResultIri) {
        final IRI outcome;
        if (sr != null) {
            if (sr.isFailed() && sr.getFailedStep().getStepLog().contains("\nCANTTELL\n")) {
                outcome = EARL.cantTell;
            } else {
                outcome = sr.isFailed() ? EARL.failed : EARL.passed;
            }
            builder.subject(scenarioIri)
                    .add(PROV.startedAtTime, new Date(sr.getStartTime()))
                    .add(PROV.endedAtTime, new Date(sr.getEndTime()))
                    .add(scenarioResultIri, PROV.value, outcome);
        } else {
            final var ignored = sc.getTags() != null && sc.getTags()
                    .stream()
                    .anyMatch(tag -> Tag.IGNORE.equals(tag.getName()));
            outcome = ignored ? EARL.untested : EARL.inapplicable;
            builder.subject(scenarioIri)
                    .add(scenarioResultIri, PROV.value, outcome);
        }
        return outcome;
    }

    private void createStepActivityList(final RepositoryConnection conn, final FeatureResult fr,
                                        final ScenarioResult sr, final IRI scenarioIri, final IRI featureIri) {
        final List<Resource> steps = sr.getStepResults().stream().map(str -> {
            final var stepIri = createNode();
            final var stepResultIri = createNode();
            final var stepBuilder = new ModelBuilder();
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
            if (!StringUtils.isEmpty(str.getStepLog())) {
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
        final var head = bnode();
        final var stepList = RDFCollections.asRDF(steps, head, new LinkedHashModel());
        stepList.add(scenarioIri, DCTERMS.hasPart, head);
        // remove the list type as it is inferred anyway and RDFa @inlist does not generate it
        stepList.remove(head, RDF.type, RDF.List);
        conn.add(stepList);
    }

    private IRI createNode() {
        return iri(Namespaces.RESULTS_URI, bnode().getID());
    }

    // strip out unnecessary logging and remove blank lines
    static String simplify(final String data) {
        // split and filter out unused lines
        final var lines = Arrays.stream(data.strip().split("\\R"))
                .map(String::strip)
                .filter(line -> !"js failed:".equals(line))
                .filter(line -> !line.matches("^>>>>.*<<<<$"))
                .collect(Collectors.toList());
        final var count = lines.size();
        // find Polyglot exception and reformat it
        var exceptionLine = IntStream.range(0, count)
                .filter(i-> lines.get(i).startsWith(POLYGLOT_EXCEPTION))
                .findFirst()
                .orElse(-1);
        if (exceptionLine != -1) {
            lines.set(exceptionLine, lines.get(exceptionLine).substring(POLYGLOT_EXCEPTION.length()));
            if (lines.get(exceptionLine + 1).startsWith("Caused by")) {
                exceptionLine += 1;
            }
            // find stack trace start and end
            final var stackStarts = exceptionLine + 1;
            final var stackEnds = IntStream.range(stackStarts, count)
                    .filter(i-> lines.get(i).startsWith("- <js>"))
                    .findFirst()
                    .orElse(count);
            return IntStream.range(0, count)
                    .filter(i -> i <= stackStarts || i == stackEnds)
                    .mapToObj(lines::get)
                    .collect(Collectors.joining("\n"));
        } else {
            return String.join("\n", lines);
        }
    }

    public Map<String, Scores> getFeatureScores() {
        final var queryString = Namespaces.generateTurtlePrefixes(List.of(SPEC.PREFIX, EARL.PREFIX)) +
                "SELECT ?level ?outcome (COUNT(?outcome) AS ?count) " +
                "WHERE {" +
                "  [] earl:test [spec:requirementReference/spec:requirementLevel ?level] ;" +
                "     earl:result/earl:outcome ?outcome ." +
                "}" +
                "GROUP BY ?level ?outcome";
        return getScoresByOutcomeLevel(queryString);
    }

    public Map<String, Scores> getScenarioScores() {
        final var queryString = Namespaces.generateTurtlePrefixes(
                List.of(SPEC.PREFIX, PROV.PREFIX, DCTERMS.PREFIX)
        ) +
                "SELECT ?level ?outcome (COUNT(?outcome) AS ?count) " +
                "WHERE {" +
                "  ?t dcterms:hasPart ?s ;" +
                "    spec:requirementReference/spec:requirementLevel ?level ." +
                "  ?s a prov:Activity ;" +
                "    dcterms:hasPart ?l ;" +
                "    prov:generated/prov:value ?outcome ." +
                "}" +
                "GROUP BY ?level ?outcome";
        return getScoresByOutcomeLevel(queryString);
    }

    public int countToleratedFailures() {
        if (failingScenarios == null || failingScenarios.isEmpty()) {
            return 0;
        }
        final var queryString = Namespaces.generateTurtlePrefixes(
                List.of(SPEC.PREFIX, PROV.PREFIX, DCTERMS.PREFIX)
        ) +
                "SELECT ?scenario ?outcome ?level " +
                "WHERE {" +
                "  ?t dcterms:hasPart ?s ;" +
                "    spec:requirementReference/spec:requirementLevel ?level ." +
                "  ?s a prov:Activity ;" +
                "    dcterms:title ?scenario ;" +
                "    prov:generated/prov:value ?outcome ." +
                "  FILTER (?scenario IN (" +
                failingScenarios.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",")) +
                ")) ." +
                "}";
        try (
                var conn = getConnection()
        ) {
            final var tupleQuery = conn.prepareTupleQuery(queryString);
            final var passing = new HashSet<String>();
            final var failing = new HashSet<String>();
            final var all = new HashSet<String>();
            try (var result = tupleQuery.evaluate()) {
                while (result.hasNext()) {
                    final var bindingSet = result.next();
                    final var scenario = bindingSet.getValue("scenario").stringValue();
                    final var outcome = ((IRI)bindingSet.getValue("outcome")).getLocalName();
                    final var level = ((IRI)bindingSet.getValue("level")).getLocalName();
                    if (TestSuiteResults.MUST.equals(level) || TestSuiteResults.MUST_NOT.equals(level)) {
                        if (Scores.PASSED.equals(outcome)) {
                            passing.add(scenario);
                        } else if (Scores.FAILED.equals(outcome)) {
                            failing.add(scenario);
                        }
                    }
                    all.add(scenario);
                }
            }
            passing.stream()
                    .filter(p -> failingScenarios.contains(p))
                    .forEach(p -> logger.warn("Scenario listed as a tolerable failure but passed: " + p));
            failingScenarios.stream()
                    .filter(s -> !all.contains(s))
                    .forEach(p -> logger.warn("Scenario listed as a tolerable failure but not found in results: " + p));
            logger.info("Tolerating {} scenario failure(s):  {}", failing.size(), failing);
            return failing.size();
        }
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private Map<String, Scores> getScoresByOutcomeLevel(final String queryString) {
        final var counts = new HashMap<String, Scores>();
        try (
                var conn = getConnection()
        ) {
            final var tupleQuery = conn.prepareTupleQuery(queryString);
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                while (result.hasNext()) {
                    final var bindingSet = result.next();
                    final var level = ((IRI)bindingSet.getValue("level")).getLocalName();
                    final var outcome = ((IRI)bindingSet.getValue("outcome")).getLocalName();
                    final var count = Integer.parseInt(bindingSet.getValue("count").stringValue());
                    final var scores = counts.computeIfAbsent(level, k -> new Scores());
                    scores.setScore(outcome, count);
                }
            }
        }
        return counts;
    }

    public void export(final Writer wr, final Resource... contexts) throws TestHarnessException {
        final var rdfWriter = Rio.createWriter(RDFFormat.TURTLE, wr);
        try (var conn = getConnection()) {
            rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true)
                    .set(BasicWriterSettings.INLINE_BLANK_NODES, true);
            conn.export(rdfWriter, contexts);
        } catch (RDF4JException e) {
            throw new TestHarnessException("Failed to write repository", e);
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

    @Override
    public void init() throws RepositoryException {
        repository.init();
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

    private static final class ScenarioData {
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
