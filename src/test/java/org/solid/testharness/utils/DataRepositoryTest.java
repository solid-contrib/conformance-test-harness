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
        final Scenario scenario1 = mock(Scenario.class);
        when(scenario1.getName()).thenReturn("SCENARIO 1");
        when(scenario1.getLine()).thenReturn(1);
        final Scenario scenario2 = mock(Scenario.class);
        when(scenario2.getName()).thenReturn("SCENARIO 2");
        when(scenario2.getLine()).thenReturn(10);
        final Step step1 = mock(Step.class);
        when(step1.getPrefix()).thenReturn("When");
        when(step1.getText()).thenReturn("method GET");
        when(step1.getLine()).thenReturn(1);
        when(step1.isBackground()).thenReturn(true);
        final Step step2 = mock(Step.class);
        when(step2.getPrefix()).thenReturn("Then");
        when(step2.getText()).thenReturn("Status 200");
        when(step2.getLine()).thenReturn(2);
        final Result res1 = mock(Result.class);
        when(res1.getStatus()).thenReturn("passed");
        final Result res2 = mock(Result.class);
        when(res2.getStatus()).thenReturn("skipped");

        final FeatureResult fr = mock(FeatureResult.class);
        when(fr.getDisplayName()).thenReturn("DISPLAY_NAME");
        when(fr.getFeature()).thenReturn(feature);
        when(fr.isFailed()).thenReturn(true);
        when(fr.getDurationMillis()).thenReturn(1000.0);

        final ScenarioResult sr1 = mock(ScenarioResult.class);
        when(sr1.getScenario()).thenReturn(scenario1);
        when(sr1.isFailed()).thenReturn(true);
        when(sr1.getStartTime()).thenReturn(123456789L);
        when(sr1.getEndTime()).thenReturn(123456789L);
        when(sr1.getDurationMillis()).thenReturn(2000.0);

        final ScenarioResult sr2 = mock(ScenarioResult.class);
        when(sr2.getScenario()).thenReturn(scenario2);
        when(sr2.isFailed()).thenReturn(false);
        when(sr1.getStartTime()).thenReturn(123456789L);
        when(sr1.getEndTime()).thenReturn(123456789L);
        when(sr2.getDurationMillis()).thenReturn(3000.0);

        when(fr.getScenarioResults()).thenReturn(List.of(sr1, sr2));

        final StepResult str1 = mock(StepResult.class);
        when(str1.getStep()).thenReturn(step1);
        when(str1.getResult()).thenReturn(res1);
        when(str1.getStepLog()).thenReturn("STEP1 LOG\n" +
                "js failed:\n>>>>?<<<<\n" +
                "org.graalvm.polyglot.PolyglotException: EXCEPTION\n" +
                "Caused by EXCEPTION2\nSTACK1\nSTACK2\n- <js>LAST\nother stuff");
        final StepResult str2 = mock(StepResult.class);
        when(str2.getStep()).thenReturn(step2);
        when(str2.getResult()).thenReturn(res2);
        when(str2.getStepLog()).thenReturn("");
        when(sr1.getStepResults()).thenReturn(List.of(str1, str2));

        dataRepository.addFeatureResult(suite, fr, featureIri);
        final String result = TestUtils.repositoryToString(dataRepository);
        assertTrue(result.contains("dcterms:title \"FEATURE NAME\""));
        assertTrue(result.contains("earl:outcome earl:failed"));
        assertTrue(result.contains("prov:value earl:passed"));
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
        final FeatureResult fr = mock(FeatureResult.class);
        when(fr.getDisplayName()).thenReturn("DISPLAY_NAME");
        when(fr.getFeature()).thenReturn(feature);
        when(fr.isFailed()).thenReturn(true);
        when(fr.getDurationMillis()).thenReturn(1000.0);
        when(fr.getScenarioResults()).thenReturn(Collections.emptyList());

        dataRepository.addFeatureResult(suite, fr, featureIri);
        final String result = TestUtils.repositoryToString(dataRepository);
        assertTrue(result.contains("dcterms:title \"FEATURE NAME\""));
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
        final Scenario scenario1 = mock(Scenario.class);
        when(scenario1.getName()).thenReturn("SCENARIO 1");
        when(scenario1.getLine()).thenReturn(1);

        final FeatureResult fr = mock(FeatureResult.class);
        when(fr.getDisplayName()).thenReturn("DISPLAY_NAME");
        when(fr.getFeature()).thenReturn(feature);
        when(fr.isFailed()).thenReturn(true);
        when(fr.getDurationMillis()).thenReturn(1000.0);

        final ScenarioResult sr1 = mock(ScenarioResult.class);
        when(sr1.getScenario()).thenReturn(scenario1);
        when(sr1.isFailed()).thenReturn(true);
        when(sr1.getStartTime()).thenReturn(123456789L);
        when(sr1.getEndTime()).thenReturn(123456789L);
        when(sr1.getDurationMillis()).thenReturn(2000.0);

        when(fr.getScenarioResults()).thenReturn(List.of(sr1));
        when(sr1.getStepResults()).thenReturn(Collections.emptyList());

        dataRepository.addFeatureResult(suite, fr, featureIri);
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

        dataRepository.addFeatureResult(suite, fr, featureIri);
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
        assertEquals(28, dataRepositorySize(dataRepository));
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
}
