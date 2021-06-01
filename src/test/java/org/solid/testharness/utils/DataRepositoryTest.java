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
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class DataRepositoryTest {
    private static final Logger logger = LoggerFactory.getLogger(DataRepositoryTest.class);

    private static final IRI assertor = iri(TestData.SAMPLE_NS, "testharness");
    private static final IRI testSubject = iri(TestData.SAMPLE_NS, "test");
    private static final IRI featureIri = iri(TestData.SAMPLE_NS, "feature");

    @Test
    void addFeatureResult() throws Exception {
        final DataRepository dataRepository = new DataRepository();
        dataRepository.postConstruct();
        dataRepository.setAssertor(assertor);
        dataRepository.setTestSubject(testSubject);
        final Suite suite = Suite.forTempUse();
        final Feature feature = mock(Feature.class);
        when(feature.getName()).thenReturn("FEATURE NAME");
        final Scenario scenario1 = mock(Scenario.class);
        when(scenario1.getName()).thenReturn("SCENARIO 1");
        final Scenario scenario2 = mock(Scenario.class);
        when(scenario2.getName()).thenReturn("SCENARIO 2");
        final Step step1 = mock(Step.class);
        when(step1.getPrefix()).thenReturn("When");
        when(step1.getText()).thenReturn("method GET");
        final Step step2 = mock(Step.class);
        when(step2.getPrefix()).thenReturn("Then");
        when(step2.getText()).thenReturn("Status 200");
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
        when(sr1.getDurationMillis()).thenReturn(2000.0);

        final ScenarioResult sr2 = mock(ScenarioResult.class);
        when(sr2.getScenario()).thenReturn(scenario2);
        when(sr2.isFailed()).thenReturn(false);
        when(sr2.getDurationMillis()).thenReturn(3000.0);

        when(fr.getScenarioResults()).thenReturn(List.of(sr1, sr2));

        final StepResult str1 = mock(StepResult.class);
        when(str1.getStep()).thenReturn(step1);
        when(str1.getResult()).thenReturn(res1);
        when(str1.getStepLog()).thenReturn("STEP1 LOG");
        final StepResult str2 = mock(StepResult.class);
        when(str2.getStep()).thenReturn(step2);
        when(str2.getResult()).thenReturn(res2);
        when(str2.getStepLog()).thenReturn("");
        when(sr1.getStepResults()).thenReturn(List.of(str1, str2));

        dataRepository.addFeatureResult(suite, fr, featureIri);
        final StringWriter sw = new StringWriter();
        dataRepository.export(sw);
        assertTrue(sw.toString().contains(featureIri.stringValue()));
        assertTrue(sw.toString().contains("dcterms:title \"FEATURE NAME\""));
    }

    @Test
    void addFeatureResultTestFailed() throws Exception {
        final DataRepository dataRepository = new DataRepository();
        dataRepository.postConstruct();
        dataRepository.setAssertor(assertor);
        dataRepository.setTestSubject(testSubject);
        final Suite suite = Suite.forTempUse();
        final Feature feature = mock(Feature.class);
        when(feature.getName()).thenReturn("FEATURE NAME");
        final FeatureResult fr = mock(FeatureResult.class);
        when(fr.getDisplayName()).thenReturn("DISPLAY_NAME");
        when(fr.getFeature()).thenReturn(feature);
        when(fr.isFailed()).thenReturn(false);
        when(fr.getDurationMillis()).thenReturn(1000.0);
        when(fr.getScenarioResults()).thenReturn(null);

        dataRepository.addFeatureResult(suite, fr, featureIri);
        final StringWriter sw = new StringWriter();
        dataRepository.export(sw);
        assertTrue(sw.toString().contains(featureIri.stringValue()));
        assertTrue(sw.toString().contains("dcterms:title \"FEATURE NAME\""));
    }

    @Test
    void addFeatureResultBadRdf() throws Exception {
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

        dataRepository.addFeatureResult(suite, fr, featureIri);
        final StringWriter sw = new StringWriter();
        dataRepository.export(sw);
        assertFalse(sw.toString().contains(featureIri.stringValue()));
    }

    @Test
    void exportWriter() throws Exception {
        final DataRepository dataRepository = setupRepository();
        final StringWriter wr = new StringWriter();
        dataRepository.export(wr);
        assertTrue(wr.toString().contains(TestData.SAMPLE_TURTLE));
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
    void exportStream() throws Exception {
        final DataRepository dataRepository = setupRepository();
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        dataRepository.export(os);
        assertTrue(os.toString().contains(TestData.SAMPLE_TURTLE));
    }

    @Test
    void exportStreamFailing() throws IOException {
        final DataRepository dataRepository = setupRepository();
        final File tempFile = File.createTempFile("TestHarness-", ".tmp");
        tempFile.deleteOnExit();
        final OutputStream os = Files.newOutputStream(tempFile.toPath());
        os.close();
        assertThrows(Exception.class, () -> dataRepository.export(os));
    }

    @Test
    void loadTurtle() throws MalformedURLException {
        final DataRepository dataRepository = new DataRepository();
        final URL url = Path.of("src/test/resources/config-sample.ttl").normalize().toUri().toURL();
        dataRepository.loadTurtle(url);
        assertEquals(33, dataRepositorySize(dataRepository));
    }

    @Test
    void loadTurtleBadUrl() {
        final DataRepository dataRepository = new DataRepository();
        assertThrows(TestHarnessInitializationException.class,
                () -> dataRepository.loadTurtle(new URL("file:/missing.txt"))
        );
    }

    @Test
    void loadTurtleBadData() {
        final DataRepository dataRepository = new DataRepository();
        assertThrows(TestHarnessInitializationException.class,
                () -> dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/jsonld-sample.json"))
        );
    }

    @Test
    void loadRdfa() throws MalformedURLException {
        final DataRepository dataRepository = new DataRepository();
        dataRepository.loadRdfa(TestUtils.getFileUrl("src/test/resources/rdfa-sample.html"), TestData.SAMPLE_BASE);
        assertEquals(1, dataRepositorySize(dataRepository));
    }

    @Test
    void loadRdfaBadUrl() {
        final DataRepository dataRepository = new DataRepository();
        assertThrows(TestHarnessInitializationException.class,
                () -> dataRepository.loadRdfa(new URL("file:/missing.txt"), TestData.SAMPLE_BASE)
        );
    }

    @Test
    void loadRdfaBadData() {
        final DataRepository dataRepository = new DataRepository();
        assertThrows(TestHarnessInitializationException.class,
                () -> dataRepository.loadRdfa(TestUtils.getFileUrl("src/test/resources/jsonld-sample.json"),
                        TestData.SAMPLE_BASE)
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
                    .createStatement(iri(TestData.SAMPLE_NS, "bob"), RDF.TYPE, FOAF.PERSON);
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
