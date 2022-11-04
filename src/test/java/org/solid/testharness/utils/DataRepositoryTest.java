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

import com.intuit.karate.core.*;
import com.intuit.karate.resource.Resource;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Test;
import org.solid.common.vocab.*;
import org.solid.testharness.reporting.Scores;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.rdf4j.model.util.Values.bnode;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class DataRepositoryTest {
    private static final IRI assertor = iri(TestUtils.SAMPLE_NS, "testharness");
    private static final IRI testSubject = iri(TestUtils.SAMPLE_NS, "test");
    private static final IRI featureIri = iri(TestUtils.SAMPLE_NS, "feature");
    private static final IRI testCaseIri = iri(TestUtils.SAMPLE_NS, "testCase");
    private static final IRI requirementIri = iri(TestUtils.SAMPLE_NS, "requirement");
    private static final IRI assertionIri = iri(TestUtils.SAMPLE_NS, "assertion");

    @Test
    void addFeatureResult() {
        final DataRepository dataRepository = createRepository();
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            conn.add(testCaseIri, SPEC.testScript, featureIri);
        }

        final Feature feature = mock(Feature.class);
        when(feature.getName()).thenReturn("FEATURE NAME");
        final Scenario scenario1 = mockScenario("SCENARIO 1", 1, 0, null);
        final Scenario scenario2 = mockScenario("SCENARIO 2", 10, 1, null);
        final Scenario scenario3 = mockScenario("SCENARIO 3 IGNORED", 20, 2, List.of(Tag.IGNORE));
        final Scenario scenario4 = mockScenario("SCENARIO 4 SKIPPED", 30, 3, List.of("skip"));
        final Scenario scenario5 = mockScenario("SCENARIO 5 SKIPPED NO TAGS", 40, 4, null);
        final Scenario scenario6 = mockScenario("SETUP SCENARIO 6", 50, 5, List.of(Tag.SETUP));
        final ScenarioOutline scenarioOutline = mockScenarioOutline("SCENARIO OUTLINE SKIPPED", 60, 6, List.of("skip"));
        final List<FeatureSection> sections = Stream.of(scenario1, scenario2, scenario3,
                        scenario4, scenario5, scenario6, scenarioOutline)
                .map(sc -> {
                    if (sc instanceof Scenario) {
                        return ((Scenario) sc).getSection();
                    } else {
                        return ((ScenarioOutline) sc).getSection();
                    }
                }).collect(Collectors.toList());
        when(feature.getSections()).thenReturn(sections);
        final Step step1 = mockStep("When", "method GET", 1, true, List.of("STEP COMMENT"));
        final Step step2 = mockStep("Then", "Status 200", 2, false, null);
        final StepResult str1 = mockStepResult(step1, "passed", "STEP1 LOG\n" +
                "js failed:\n>>>>?<<<<\n" +
                "org.graalvm.polyglot.PolyglotException: EXCEPTION\n" +
                "Caused by EXCEPTION2\nSTACK1\nSTACK2\n- <js>LAST\nother stuff");
        final StepResult str2 = mockStepResult(step2, "skipped", "");

        final ScenarioResult sr1 = mockScenarioResult(scenario1, true, 2000.0, List.of(str1, str2), "FAIL");
        final ScenarioResult sr2 = mockScenarioResult(scenario2, false, 3000.0, null, null);

        final FeatureResult fr = mockFeatureResult(feature, "DISPLAY_NAME", true, 1000.0, List.of(sr1, sr2));

        final FeatureFileParser featureFileParser = mock(FeatureFileParser.class);
        when(featureFileParser.getFeatureComments()).thenReturn("FEATURE COMMENT");
        when(featureFileParser.getScenarioComments(0)).thenReturn("SCENARIO1 COMMENT");
        when(featureFileParser.getScenarioComments(1)).thenReturn("");

        dataRepository.addFeatureResult(TestUtils.createEmptySuite(), fr, featureIri, featureFileParser);
        final String result = TestUtils.repositoryToString(dataRepository);
        assertTrue(result.contains("dcterms:title \"FEATURE NAME\""));
        assertTrue(result.contains("dcterms:description \"FEATURE COMMENT\""));
        assertTrue(result.contains("dcterms:description \"SCENARIO1 COMMENT\""));
        assertTrue(result.contains("earl:outcome earl:failed"));
        assertTrue(result.contains("prov:value earl:passed"));
        assertTrue(result.contains("prov:value earl:untested"));
        assertTrue(result.contains("prov:value earl:inapplicable"));
        assertTrue(result.contains("dcterms:description \"STEP COMMENT\""));
        assertTrue(result.contains("dcterms:description \"\"\"STEP1 LOG\n" +
                "EXCEPTION\nCaused by EXCEPTION2\nSTACK1\n- <js>LAST"));
        assertTrue(result.contains("dcterms:title \"SCENARIO OUTLINE SKIPPED\""));
    }

    @Test
    void addFeatureResultCallOnce() {
        final DataRepository dataRepository = createRepository();
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            conn.add(testCaseIri, SPEC.testScript, featureIri);
        }

        final Feature feature = mock(Feature.class);
        when(feature.getName()).thenReturn("FEATURE NAME");
        final Scenario scenario1 = mockScenario("SCENARIO 1", 1, 0, null);
        final Scenario scenario2 = mockScenario("SCENARIO 2", 10, 1, null);
        final Step step11 = mockStep("*", "callonce setup", 1, true, null);
        final Step step12 = mockStep("Then", "Test1", 2, false, null);
        final StepResult str11 = mockStepResult(step11, "passed", "Time callonce lock: setup\n");
        final StepResult str12 = mockStepResult(step12, "passed", "");
        final ScenarioResult sr1 = mockScenarioResult(scenario1, false, 2000.0, List.of(str11, str12), null);

        final Step step22 = mockStep("Then", "Test2", 3, false, null);
        final StepResult str21 = mockStepResult(step11, "passed", "Time lock acquired, begin\nSetup routine");
        final StepResult str22 = mockStepResult(step22, "passed", "");
        final ScenarioResult sr2 = mockScenarioResult(scenario2, false, 3000.0, List.of(str21, str22), null);

        final FeatureResult fr = mockFeatureResult(feature, "DISPLAY_NAME", true, 1000.0, List.of(sr1, sr2));

        final FeatureFileParser featureFileParser = mock(FeatureFileParser.class);

        dataRepository.addFeatureResult(TestUtils.createEmptySuite(), fr, featureIri, featureFileParser);
        final String result = TestUtils.repositoryToString(dataRepository);
        assertTrue(result.contains("dcterms:description \"\"\"Time callonce lock: setup\nSetup routine"));
        assertTrue(result.contains("dcterms:description \"\"\"Time lock acquired, begin\nSetup routine"));
    }

    @Test
    void addFeatureResultTestFailed() {
        final DataRepository dataRepository = createRepository();
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            conn.add(testCaseIri, SPEC.testScript, featureIri);
        }

        final Feature feature = mock(Feature.class);
        when(feature.getName()).thenReturn("FEATURE NAME");

        final FeatureResult fr = mockFeatureResult(feature, "DISPLAY_NAME", true, 1000.0,
                Collections.emptyList());

        final FeatureFileParser featureFileParser = mock(FeatureFileParser.class);

        dataRepository.addFeatureResult(TestUtils.createEmptySuite(), fr, featureIri, featureFileParser);
        final String result = TestUtils.repositoryToString(dataRepository);
        assertTrue(result.contains("dcterms:title \"FEATURE NAME\""));
        assertFalse(result.contains("dcterms:description"));
        assertTrue(result.contains("earl:outcome earl:untested"));
    }

    @Test
    void addFeatureResultNoTestCase() {
        final DataRepository dataRepository = createRepository();

        final Feature feature = mock(Feature.class);
        when(feature.getName()).thenReturn("FEATURE NAME");
        final Scenario scenario1 = mockScenario("SCENARIO 1", 1, 0, null);

        final ScenarioResult sr1 = mockScenarioResult(scenario1, true, 2000.0,
                Collections.emptyList(), "FAIL\nCANTTELL\n");
        final FeatureResult fr = mockFeatureResult(feature, "DISPLAY_NAME", true, 1000.0, List.of(sr1));

        final FeatureFileParser featureFileParser = mock(FeatureFileParser.class);

        dataRepository.addFeatureResult(TestUtils.createEmptySuite(), fr, featureIri, featureFileParser);
        final String result = TestUtils.repositoryToString(dataRepository);
        assertFalse(result.contains("dcterms:title \"FEATURE NAME\""));
        assertTrue(result.contains("earl:outcome earl:cantTell"));
    }

    @Test
    void addFeatureResultBadRdf() {
        final DataRepository dataRepository = createRepository();
        final Feature feature = mock(Feature.class);
        when(feature.getName()).thenReturn(null);
        final FeatureResult fr = mock(FeatureResult.class);
        when(fr.getDisplayName()).thenReturn("");
        when(fr.getFeature()).thenReturn(feature);
        when(fr.getScenarioResults()).thenThrow(new RuntimeException("FAILED"));

        final FeatureFileParser featureFileParser = mock(FeatureFileParser.class);

        dataRepository.addFeatureResult(TestUtils.createEmptySuite(), fr, featureIri, featureFileParser);
        final String result = TestUtils.repositoryToString(dataRepository);
        assertFalse(result.contains(featureIri.stringValue()));
    }

    @Test
    void createAssertion() {
        final DataRepository dataRepository = createRepository();
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            dataRepository.createAssertion(conn, EARL.passed, new Date(), testCaseIri);
        }
        final String result = TestUtils.repositoryToString(dataRepository);
        assertTrue(result.contains("a earl:Assertion"));
        assertTrue(result.contains("a earl:TestResult"));
        assertTrue(result.contains("earl:test <" + testCaseIri + ">"));
        assertTrue(result.contains("earl:outcome earl:passed"));
    }

    @Test
    void createAssertionNoTestIri() {
        final DataRepository dataRepository = createRepository();
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            dataRepository.createAssertion(conn, EARL.failed, new Date(), null);
        }
        final String result = TestUtils.repositoryToString(dataRepository);
        assertTrue(result.contains("a earl:Assertion"));
        assertTrue(result.contains("a earl:TestResult"));
        assertFalse(result.contains("earl:test <" + testCaseIri + ">"));
        assertTrue(result.contains("earl:outcome earl:failed"));
    }

    @Test
    void createInapplicableAssertion() {
        final DataRepository dataRepository = createRepository();
        final Feature feature = mockUntestedFeature();
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            conn.add(testCaseIri, SPEC.testScript, featureIri);
        }
        dataRepository.createSkippedAssertion(feature, featureIri.stringValue(), EARL.inapplicable);
        final String result = TestUtils.repositoryToString(dataRepository);
        assertTrue(result.contains("a earl:Assertion"));
        assertTrue(result.contains("a earl:TestResult"));
        assertTrue(result.contains("earl:test <" + testCaseIri.stringValue() + ">"));
        assertTrue(result.contains("earl:outcome earl:inapplicable"));
        assertTrue(result.contains("dcterms:title \"FEATURE NAME\""));
    }

    @Test
    void createInapplicableAssertionNoStatements() {
        final DataRepository dataRepository = createRepository();
        final Feature feature = mockUntestedFeature();
        dataRepository.createSkippedAssertion(feature, featureIri.stringValue(), EARL.inapplicable);
        final String result = TestUtils.repositoryToString(dataRepository);
        assertFalse(result.contains("a earl:Assertion"));
    }

    private DataRepository createRepository() {
        final DataRepository dataRepository = new DataRepository();
        dataRepository.postConstruct();
        dataRepository.setAssertor(assertor);
        dataRepository.setTestSubject(testSubject);
        return dataRepository;
    }

    @Test
    void countTestsPassed() {
        final DataRepository dataRepository = createRepository();
        createAssertion(dataRepository, SPEC.MUST, EARL.passed);
        final Map<String, Scores> results = dataRepository.getFeatureScores();
        assertEquals(1, results.get("MUST").getPassed());
    }

    @Test
    void countTestsFailed() {
        final DataRepository dataRepository = createRepository();
        createAssertion(dataRepository, SPEC.MAY, EARL.failed);
        createAssertion(dataRepository, SPEC.MAY, EARL.untested);
        final Map<String, Scores> results = dataRepository.getFeatureScores();
        assertEquals(1, results.get("MAY").getFailed());
        assertEquals(1, results.get("MAY").getUntested());
    }

    @Test
    void countTestsNoOutcome() {
        final DataRepository dataRepository = createRepository();
        createAssertion(dataRepository, SPEC.SHOULD, null);
        final Map<String, Scores> results = dataRepository.getFeatureScores();
        assertEquals(0, results.size());
    }

    @Test
    void getScenarioScores() {
        final DataRepository dataRepository = createRepository();
        createScenarioOutcome(dataRepository, SPEC.MAY, EARL.passed);
        createScenarioOutcome(dataRepository, SPEC.MAY, EARL.failed);
        createScenarioOutcome(dataRepository, SPEC.MUST, EARL.passed);
        final Map<String, Scores> results = dataRepository.getScenarioScores();
        assertEquals(1, results.get("MAY").getPassed());
        assertEquals(1, results.get("MAY").getFailed());
        assertEquals(1, results.get("MUST").getPassed());
        assertEquals(2, results.get("MAY").getTotal());
        assertEquals(1, results.get("MUST").getTotal());
    }

    @Test
    void getScenarioScoresNoOutcome() {
        final DataRepository dataRepository = createRepository();
        createScenarioOutcome(dataRepository, SPEC.SHOULD, null);
        final Map<String, Scores> results = dataRepository.getScenarioScores();
        assertEquals(0, results.size());
    }

    @Test
    void exportWriter() throws Exception {
        final String sample = TestUtils.loadStringFromFile("src/test/resources/turtle-sample.ttl");
        final DataRepository dataRepository = setupMinimalRepository();
        final StringWriter wr = new StringWriter();
        dataRepository.export(wr);
        assertTrue(wr.toString().replaceAll("\\r\\n", "\n").contains(sample));
    }

    @Test
    void exportWriterFailing() throws IOException {
        final DataRepository dataRepository = setupMinimalRepository();
        final File tempFile = File.createTempFile("TestHarness-", ".tmp");
        tempFile.deleteOnExit();
        final Writer wr = Files.newBufferedWriter(tempFile.toPath());
        wr.close();
        assertThrows(Exception.class, () -> dataRepository.export(wr));
    }

    @Test
    void loadTurtle() throws MalformedURLException {
        final DataRepository dataRepository = new DataRepository();
        final URL url = Path.of("src/test/resources/config/config-sample.ttl").normalize().toUri().toURL();
        dataRepository.load(url);
        assertEquals(27, dataRepositorySize(dataRepository));
    }

    @Test
    void loadTurtleBadUrl() {
        final DataRepository dataRepository = new DataRepository();
        assertThrows(TestHarnessInitializationException.class,
                () -> dataRepository.load(new URL("file:/missing.txt"))
        );
    }

    @Test
    void loadTurtleBadData() {
        final DataRepository dataRepository = new DataRepository();
        assertThrows(TestHarnessInitializationException.class,
                () -> dataRepository.load(TestUtils.getFileUrl("src/test/resources/jsonld-sample.json"))
        );
    }

    @Test
    void loadRdfa() throws Exception {
        final DataRepository dataRepository = new DataRepository();
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            conn.setNamespace(DCTERMS.PREFIX, DCTERMS.NAMESPACE);
            conn.setNamespace(SPEC.PREFIX, SPEC.NAMESPACE);
        }
        dataRepository.load(TestUtils.getFileUrl("src/test/resources/rdfa-sample.html"), TestUtils.SAMPLE_BASE);
        assertEquals(4, dataRepositorySize(dataRepository));
        final StringWriter sw = new StringWriter();
        dataRepository.export(sw, Namespaces.SPEC_RELATED_CONTEXT);
        assertFalse(sw.toString().contains("dcterms:title \"TITLE\""));
        assertTrue(sw.toString().contains("<https://example.org/doc> a <http://usefulinc.com/ns/doap#Specification>"));
        assertTrue(sw.toString().contains("spec:requirement <https://example.org#spec1> ."));
        assertTrue(sw.toString().contains("spec:requirementSubject spec:Server"));
    }

    @Test
    void loadRdfaBadUrl() {
        final DataRepository dataRepository = new DataRepository();
        assertThrows(TestHarnessInitializationException.class,
                () -> dataRepository.load(new URL("file:/missing.txt"), TestUtils.SAMPLE_BASE)
        );
    }

    @Test
    void loadRdfaBadData() {
        final DataRepository dataRepository = new DataRepository();
        assertThrows(TestHarnessInitializationException.class,
                () -> dataRepository.load(TestUtils.getFileUrl("src/test/resources/jsonld-sample.json"),
                        TestUtils.SAMPLE_BASE)
        );
    }

    @Test
    void testOverriddenMethods() {
        final DataRepository dataRepository = new DataRepository();
        final File dataDir = new File("/tmp");
        dataRepository.setDataDir(dataDir);
        final File newDataDir = dataRepository.getDataDir();
        assertEquals(dataDir, newDataDir);
        assertFalse(dataRepository.isInitialized());
        dataRepository.init();
        assertTrue(dataRepository.isInitialized());
        assertNotNull(dataRepository.getValueFactory());
        assertTrue(dataRepository.isWritable());
        dataRepository.shutDown();
        assertFalse(dataRepository.isInitialized());
    }

    @Test
    void identifySpecifications() {
        final DataRepository dataRepository = createRepository();
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            conn.add(iri(TestUtils.SAMPLE_NS, "specA"), RDF.type, DOAP.Specification);
            conn.add(iri(TestUtils.SAMPLE_NS, "specB/"), RDF.type, DOAP.Specification);
        }
        dataRepository.identifySpecifications();
        assertTrue(Namespaces.getSpecificationNamespace(iri(TestUtils.SAMPLE_NS, "specA#1")).startsWith("spec"));
        assertTrue(Namespaces.getSpecificationNamespace(iri(TestUtils.SAMPLE_NS, "specB/2")).startsWith("spec"));
        assertTrue(Namespaces.getSpecificationNamespace(iri(TestUtils.SAMPLE_NS, "specB/#3")).startsWith("spec"));
    }

    @Test
    void simplify() {
        final String log = "STEP1 LOG\n" +
                "js failed:\n>>>>?<<<<\n" +
                "org.graalvm.polyglot.PolyglotException: EXCEPTION\n" +
                "Caused by EXCEPTION2\nSTACK1\nSTACK2\n- <js>LAST\nother stuff\nmore";
        assertEquals("STEP1 LOG\n" +
                "EXCEPTION\n" +
                "Caused by EXCEPTION2\n" +
                "STACK1\n" +
                "- <js>LAST", DataRepository.simplify(log));
    }

    @Test
    void simplifyNoCause() {
        final String log = "STEP1 LOG\n" +
                "js failed:\n>>>>?<<<<\n" +
                "org.graalvm.polyglot.PolyglotException: EXCEPTION\n" +
                "STACK1\nSTACK2\n- <js>LAST\nother stuff\nmore";
        assertEquals("STEP1 LOG\n" +
                "EXCEPTION\n" +
                "STACK1\n" +
                "- <js>LAST", DataRepository.simplify(log));
    }

    private DataRepository setupMinimalRepository() {
        final DataRepository dataRepository = new DataRepository();
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            final Statement st = Values.getValueFactory()
                    .createStatement(iri(TestUtils.SAMPLE_NS, TestUtils.BOB), RDF.type, FOAF.Person);
            conn.add(st);
        }
        return dataRepository;
    }

    private void createAssertion(final DataRepository dataRepository, final IRI requirementLevel, final IRI outcome) {
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            conn.add(requirementIri, SPEC.requirementLevel, requirementLevel);
            conn.add(testCaseIri, SPEC.requirementReference, requirementIri);
            if (outcome != null) {
                conn.add(assertionIri, EARL.test, testCaseIri);
                final IRI resultIri = iri(Namespaces.RESULTS_URI, bnode().getID());
                conn.add(assertionIri, EARL.result, resultIri);
                conn.add(resultIri, EARL.outcome, outcome);
            }
        }
    }

    private void createScenarioOutcome(final DataRepository dataRepository, final IRI requirementLevel,
                                       final IRI outcome) {
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            final BNode requirement = bnode();
            conn.add(requirement, SPEC.requirementLevel, requirementLevel);
            final BNode testcase = bnode();
            conn.add(testcase, SPEC.requirementReference, requirement);
            final BNode activity = bnode();
            conn.add(testcase, DCTERMS.hasPart, activity);
            conn.add(activity, RDF.type, PROV.Activity);
            conn.add(activity, DCTERMS.hasPart, bnode());
            if (outcome != null) {
                final BNode result = bnode();
                conn.add(activity, PROV.generated, result);
                conn.add(result, PROV.value, outcome);
            }
        }
    }

    private long dataRepositorySize(final DataRepository dataRepository) {
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            return conn.size();
        }
    }

    private Scenario mockScenario(final String name, final int line, final int index, final List<String> tags) {
        final Scenario scenario = mock(Scenario.class);
        when(scenario.getName()).thenReturn(name);
        when(scenario.getLine()).thenReturn(line);
        if (tags != null) {
            when(scenario.getTags()).thenReturn(
                    tags.stream().map(t -> new Tag(1, "@" + t)).collect(Collectors.toList())
            );
        } else {
            when(scenario.getTags()).thenReturn(null);
        }
        final FeatureSection section = mock(FeatureSection.class);
        when(section.getScenario()).thenReturn(scenario);
        when(section.getIndex()).thenReturn(index);
        when(scenario.getSection()).thenReturn(section);
        return scenario;
    }

    private ScenarioOutline mockScenarioOutline(final String name, final int line, final int index,
                                                final List<String> tags) {
        final ScenarioOutline scenarioOutline = mock(ScenarioOutline.class);
        when(scenarioOutline.getName()).thenReturn(name);
        when(scenarioOutline.getLine()).thenReturn(line);
        if (tags != null) {
            when(scenarioOutline.getTags()).thenReturn(
                    tags.stream().map(t -> new Tag(1, "@" + t)).collect(Collectors.toList())
            );
        }
        final FeatureSection section = mock(FeatureSection.class);
        when(section.getScenarioOutline()).thenReturn(scenarioOutline);
        when(section.getIndex()).thenReturn(index);
        when(scenarioOutline.getSection()).thenReturn(section);
        return scenarioOutline;
    }

    private Step mockStep(final String prefix, final String text, final int line, final boolean isBackground,
                          final List<String> comments) {
        final Step step = mock(Step.class);
        when(step.getPrefix()).thenReturn(prefix);
        when(step.getText()).thenReturn(text);
        when(step.getLine()).thenReturn(line);
        when(step.isBackground()).thenReturn(isBackground);
        when(step.getComments()).thenReturn(comments);
        return step;
    }

    private FeatureResult mockFeatureResult(final Feature feature, final String name, final boolean isFailed,
                                            final double duration, final List<ScenarioResult> results) {
        final FeatureResult fr = mock(FeatureResult.class);
        when(fr.getDisplayName()).thenReturn(name);
        when(fr.getFeature()).thenReturn(feature);
        when(fr.isFailed()).thenReturn(isFailed);
        when(fr.getDurationMillis()).thenReturn(duration);
        if (results != null) {
            when(fr.getScenarioResults()).thenReturn(results);
        }
        return fr;
    }

    private ScenarioResult mockScenarioResult(final Scenario scenario, final boolean isFailed, final double duration,
                                              final List<StepResult> stepResults, final String failLog) {
        final StepResult str = mock(StepResult.class);
        when(str.getStepLog()).thenReturn(failLog);
        final ScenarioResult sr = mock(ScenarioResult.class);
        when(sr.getScenario()).thenReturn(scenario);
        when(sr.isFailed()).thenReturn(isFailed);
        when(sr.getFailedStep()).thenReturn(str);
        when(sr.getStartTime()).thenReturn(123456789L);
        when(sr.getEndTime()).thenReturn(123456789L);
        when(sr.getDurationMillis()).thenReturn(duration);
        if (stepResults != null) {
            when(sr.getStepResults()).thenReturn(stepResults);
        }
        return sr;
    }

    private StepResult mockStepResult(final Step step, final String result, final String log) {
        final StepResult str = mock(StepResult.class);
        when(str.getStep()).thenReturn(step);
        final Result res = mock(Result.class);
        when(res.getStatus()).thenReturn(result);
        when(str.getResult()).thenReturn(res);
        when(str.getStepLog()).thenReturn(log);
        return str;
    }

    private Feature mockUntestedFeature() {
        final Feature feature = mock(Feature.class);
        final Resource resource = mock(Resource.class);
        when(feature.getName()).thenReturn("FEATURE NAME");
        when(feature.getResource()).thenReturn(resource);
        when(resource.getRelativePath()).thenReturn("example/test");
        return feature;
    }
}
