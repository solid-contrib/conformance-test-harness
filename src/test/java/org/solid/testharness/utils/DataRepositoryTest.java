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

import com.intuit.karate.Suite;
import com.intuit.karate.core.*;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Test;
import org.solid.common.vocab.DCTERMS;
import org.solid.common.vocab.FOAF;
import org.solid.common.vocab.RDF;
import org.solid.common.vocab.SPEC;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

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

    @Test
    void addFeatureResult() {
        final DataRepository dataRepository = new DataRepository();
        dataRepository.postConstruct();
        dataRepository.setAssertor(assertor);
        dataRepository.setTestSubject(testSubject);
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            conn.add(testCaseIri, SPEC.testScript, featureIri);
        }

        final Suite suite = Suite.forTempUse();
        final Feature feature = mock(Feature.class);
        when(feature.getName()).thenReturn("FEATURE NAME");
        final Scenario scenario1 = mockScenario("SCENARIO 1", 1, 0);
        final Scenario scenario2 = mockScenario("SCENARIO 2", 10, 1);
        final Step step1 = mockStep("When", "method GET", 1, true, List.of("STEP COMMENT"));
        final Step step2 = mockStep("Then", "Status 200", 2, false, null);
        final StepResult str1 = mockStepResult(step1, "passed", "STEP1 LOG\n" +
                "js failed:\n>>>>?<<<<\n" +
                "org.graalvm.polyglot.PolyglotException: EXCEPTION\n" +
                "Caused by EXCEPTION2\nSTACK1\nSTACK2\n- <js>LAST\nother stuff");
        final StepResult str2 = mockStepResult(step2, "skipped", "");

        final ScenarioResult sr1 = mockScenarioResult(scenario1, true, 2000.0, List.of(str1, str2));
        final ScenarioResult sr2 = mockScenarioResult(scenario2, false, 3000.0, null);

        final FeatureResult fr = mockFeatureResult(feature, "DISPLAY_NAME", true, 1000.0, List.of(sr1, sr2));

        final FeatureFileParser featureFileParser = mock(FeatureFileParser.class);
        when(featureFileParser.getFeatureComments()).thenReturn("FEATURE COMMENT");
        when(featureFileParser.getScenarioComments(0)).thenReturn("SCENARIO1 COMMENT");
        when(featureFileParser.getScenarioComments(1)).thenReturn("");

        dataRepository.addFeatureResult(suite, fr, featureIri, featureFileParser);
        final String result = TestUtils.repositoryToString(dataRepository);
        assertTrue(result.contains("dcterms:title \"FEATURE NAME\""));
        assertTrue(result.contains("dcterms:description \"FEATURE COMMENT\""));
        assertTrue(result.contains("dcterms:description \"SCENARIO1 COMMENT\""));
        assertTrue(result.contains("earl:outcome earl:failed"));
        assertTrue(result.contains("prov:value earl:passed"));
        assertTrue(result.contains("dcterms:description \"STEP COMMENT\""));
        assertTrue(result.contains("dcterms:description \"\"\"STEP1 LOG\n" +
                "EXCEPTION\nCaused by EXCEPTION2\nSTACK1\n- <js>LAST"));
    }

    @Test
    void addFeatureResultTestFailed() {
        final DataRepository dataRepository = new DataRepository();
        dataRepository.postConstruct();
        dataRepository.setAssertor(assertor);
        dataRepository.setTestSubject(testSubject);
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            conn.add(testCaseIri, SPEC.testScript, featureIri);
        }

        final Suite suite = Suite.forTempUse();
        final Feature feature = mock(Feature.class);
        when(feature.getName()).thenReturn("FEATURE NAME");

        final FeatureResult fr = mockFeatureResult(feature, "DISPLAY_NAME", true, 1000.0,
                Collections.emptyList());

        final FeatureFileParser featureFileParser = mock(FeatureFileParser.class);

        dataRepository.addFeatureResult(suite, fr, featureIri, featureFileParser);
        final String result = TestUtils.repositoryToString(dataRepository);
        assertTrue(result.contains("dcterms:title \"FEATURE NAME\""));
        assertFalse(result.contains("dcterms:description"));
        assertTrue(result.contains("earl:outcome earl:failed"));
    }

    @Test
    void addFeatureResultNoTestCase() {
        final DataRepository dataRepository = new DataRepository();
        dataRepository.postConstruct();
        dataRepository.setAssertor(assertor);
        dataRepository.setTestSubject(testSubject);

        final Suite suite = Suite.forTempUse();
        final Feature feature = mock(Feature.class);
        when(feature.getName()).thenReturn("FEATURE NAME");
        final Scenario scenario1 = mockScenario("SCENARIO 1", 1, 0);

        final ScenarioResult sr1 = mockScenarioResult(scenario1, true, 2000.0, Collections.emptyList());
        final FeatureResult fr = mockFeatureResult(feature, "DISPLAY_NAME", true, 1000.0, List.of(sr1));

        final FeatureFileParser featureFileParser = mock(FeatureFileParser.class);

        dataRepository.addFeatureResult(suite, fr, featureIri, featureFileParser);
        final String result = TestUtils.repositoryToString(dataRepository);
        assertFalse(result.contains("dcterms:title \"FEATURE NAME\""));
        assertTrue(result.contains("earl:outcome earl:failed"));
    }

    @Test
    void addFeatureResultBadRdf() {
        final DataRepository dataRepository = new DataRepository();
        dataRepository.postConstruct();
        dataRepository.setAssertor(assertor);
        dataRepository.setTestSubject(testSubject);
        final Suite suite = Suite.forTempUse();
        final Feature feature = mock(Feature.class);
        when(feature.getName()).thenReturn(null);
        final FeatureResult fr = mock(FeatureResult.class);
        when(fr.getDisplayName()).thenReturn("");
        when(fr.getFeature()).thenReturn(feature);
        when(fr.getScenarioResults()).thenThrow(new RuntimeException("FAILED"));

        final FeatureFileParser featureFileParser = mock(FeatureFileParser.class);

        dataRepository.addFeatureResult(suite, fr, featureIri, featureFileParser);
        final String result = TestUtils.repositoryToString(dataRepository);
        assertFalse(result.contains(featureIri.stringValue()));
    }

    @Test
    void exportWriter() throws Exception {
        final String sample = TestUtils.loadStringFromFile("src/test/resources/turtle-sample.ttl");
        final DataRepository dataRepository = setupRepository();
        final StringWriter wr = new StringWriter();
        dataRepository.export(wr);
        assertTrue(wr.toString().contains(sample));
    }

    @Test
    void exportWriterFailing() throws IOException {
        final DataRepository dataRepository = setupRepository();
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
        assertEquals(25, dataRepositorySize(dataRepository));
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
        assertEquals(5, dataRepositorySize(dataRepository));
        final StringWriter sw = new StringWriter();
        dataRepository.export(sw);
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
        dataRepository.initialize();
        assertTrue(dataRepository.isInitialized());
        assertNotNull(dataRepository.getValueFactory());
        assertTrue(dataRepository.isWritable());
        dataRepository.shutDown();
        assertFalse(dataRepository.isInitialized());
    }

    private DataRepository setupRepository() {
        final DataRepository dataRepository = new DataRepository();
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            final Statement st = Values.getValueFactory()
                    .createStatement(iri(TestUtils.SAMPLE_NS, TestUtils.BOB), RDF.type, FOAF.Person);
            conn.add(st);
        }
        return dataRepository;
    }

    private long dataRepositorySize(final DataRepository dataRepository) {
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            return conn.size();
        }
    }

    private Scenario mockScenario(final String name, final int line, final int index) {
        final Scenario scenario = mock(Scenario.class);
        when(scenario.getName()).thenReturn(name);
        when(scenario.getLine()).thenReturn(line);
        final FeatureSection section = mock(FeatureSection.class);
        when(section.getIndex()).thenReturn(index);
        when(scenario.getSection()).thenReturn(section);
        return scenario;
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
                                              final List<StepResult> stepResults) {
        final ScenarioResult sr = mock(ScenarioResult.class);
        when(sr.getScenario()).thenReturn(scenario);
        when(sr.isFailed()).thenReturn(isFailed);
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
}
