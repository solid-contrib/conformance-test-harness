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
import java.nio.file.Path;
import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class DataRepositoryTest {
    private static final Logger logger = LoggerFactory.getLogger(DataRepositoryTest.class);

    private static final IRI assertor = iri("http://example.org/testharness");
    private static final IRI testSubject = iri("http://example.org/test");
    private static final IRI featureIri = iri("http://example.org/feature");

    @Test
    void addFeatureResult() throws Exception {
        DataRepository dataRepository = new DataRepository();
        dataRepository.postConstruct();
        dataRepository.setAssertor(assertor);
        dataRepository.setTestSubject(testSubject);
        Suite suite = Suite.forTempUse();
        Feature feature = mock(Feature.class);
        when(feature.getName()).thenReturn("FEATURE NAME");
        Scenario scenario1 = mock(Scenario.class);
        when(scenario1.getName()).thenReturn("SCENARIO 1");
        Scenario scenario2 = mock(Scenario.class);
        when(scenario2.getName()).thenReturn("SCENARIO 2");
        Step step1 = mock(Step.class);
        when(step1.getPrefix()).thenReturn("When");
        when(step1.getText()).thenReturn("method GET");
        Step step2 = mock(Step.class);
        when(step2.getPrefix()).thenReturn("Then");
        when(step2.getText()).thenReturn("Status 200");
        Result res1 = mock(Result.class);
        when(res1.getStatus()).thenReturn("passed");
        Result res2 = mock(Result.class);
        when(res2.getStatus()).thenReturn("skipped");

        FeatureResult fr = mock(FeatureResult.class);
        when(fr.getDisplayName()).thenReturn("DISPLAY_NAME");
        when(fr.getFeature()).thenReturn(feature);
        when(fr.isFailed()).thenReturn(true);
        when(fr.getDurationMillis()).thenReturn(1000.0);

        ScenarioResult sr1 = mock(ScenarioResult.class);
        when(sr1.getScenario()).thenReturn(scenario1);
        when(sr1.isFailed()).thenReturn(true);
        when(sr1.getDurationMillis()).thenReturn(2000.0);

        ScenarioResult sr2 = mock(ScenarioResult.class);
        when(sr2.getScenario()).thenReturn(scenario2);
        when(sr2.isFailed()).thenReturn(false);
        when(sr2.getDurationMillis()).thenReturn(3000.0);

        when(fr.getScenarioResults()).thenReturn(List.of(sr1, sr2));

        StepResult str1 = mock(StepResult.class);
        when(str1.getStep()).thenReturn(step1);
        when(str1.getResult()).thenReturn(res1);
        when(str1.getStepLog()).thenReturn("STEP1 LOG");
        StepResult str2 = mock(StepResult.class);
        when(str2.getStep()).thenReturn(step2);
        when(str2.getResult()).thenReturn(res2);
        when(str2.getStepLog()).thenReturn("");
        when(sr1.getStepResults()).thenReturn(List.of(str1, str2));

        dataRepository.addFeatureResult(suite, fr, featureIri);
        StringWriter sw = new StringWriter();
        dataRepository.export(sw);
        assertTrue(sw.toString().contains(featureIri.stringValue()));
        assertTrue(sw.toString().contains("dcterms:title \"FEATURE NAME\""));
    }

    @Test
    void addFeatureResultTestFailed() throws Exception {
        DataRepository dataRepository = new DataRepository();
        dataRepository.postConstruct();
        dataRepository.setAssertor(assertor);
        dataRepository.setTestSubject(testSubject);
        Suite suite = Suite.forTempUse();
        Feature feature = mock(Feature.class);
        when(feature.getName()).thenReturn("FEATURE NAME");
        FeatureResult fr = mock(FeatureResult.class);
        when(fr.getDisplayName()).thenReturn("DISPLAY_NAME");
        when(fr.getFeature()).thenReturn(feature);
        when(fr.isFailed()).thenReturn(false);
        when(fr.getDurationMillis()).thenReturn(1000.0);
        when(fr.getScenarioResults()).thenReturn(null);

        dataRepository.addFeatureResult(suite, fr, featureIri);
        StringWriter sw = new StringWriter();
        dataRepository.export(sw);
        assertTrue(sw.toString().contains(featureIri.stringValue()));
        assertTrue(sw.toString().contains("dcterms:title \"FEATURE NAME\""));
    }

    @Test
    void addFeatureResultBadRdf() throws Exception {
        DataRepository dataRepository = new DataRepository();
        dataRepository.postConstruct();
        dataRepository.setAssertor(assertor);
        dataRepository.setTestSubject(testSubject);
        Suite suite = Suite.forTempUse();
        Feature feature = mock(Feature.class);
        when(feature.getName()).thenReturn(null);
        FeatureResult fr = mock(FeatureResult.class);
        when(fr.getDisplayName()).thenReturn("");
        when(fr.getFeature()).thenReturn(feature);

        dataRepository.addFeatureResult(suite, fr, featureIri);
        StringWriter sw = new StringWriter();
        dataRepository.export(sw);
        assertFalse(sw.toString().contains(featureIri.stringValue()));
    }

    @Test
    void exportWriter() throws Exception {
        DataRepository dataRepository = setupRepository();
        StringWriter wr = new StringWriter();
        dataRepository.export(wr);
        assertTrue(wr.toString().contains(TestData.SAMPLE_EXPORTED_TRIPLE));
    }

    @Test
    void exportWriterFailing() throws IOException {
        DataRepository dataRepository = setupRepository();
        File tempFile = File.createTempFile("TestHarness-", ".tmp");
        tempFile.deleteOnExit();
        Writer wr = new FileWriter(tempFile);
        wr.close();
        assertThrows(Exception.class, () -> dataRepository.export(wr));
    }

    @Test
    void exportStream() throws Exception {
        DataRepository dataRepository = setupRepository();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        dataRepository.export(os);
        assertTrue(os.toString().contains(TestData.SAMPLE_EXPORTED_TRIPLE));
    }

    @Test
    void exportStreamFailing() throws IOException {
        DataRepository dataRepository = setupRepository();
        File tempFile = File.createTempFile("TestHarness-", ".tmp");
        tempFile.deleteOnExit();
        OutputStream os = new FileOutputStream(tempFile);
        os.close();
        assertThrows(Exception.class, () -> dataRepository.export(os));
    }

    @Test
    void loadTurtle() throws MalformedURLException {
        DataRepository dataRepository = new DataRepository();
        URL url = Path.of("src/test/resources/config-sample.ttl").normalize().toUri().toURL();
        dataRepository.loadTurtle(url);
        assertEquals(55, dataRepositorySize(dataRepository));
    }

    @Test
    void loadTurtleBadUrl() {
        DataRepository dataRepository = new DataRepository();
        assertThrows(TestHarnessInitializationException.class, () -> dataRepository.loadTurtle(new URL("file:/missing.txt")));
    }

    @Test
    void loadTurtleBadData() {
        DataRepository dataRepository = new DataRepository();
        assertThrows(TestHarnessInitializationException.class, () -> dataRepository.loadTurtle(TestUtils.getFileUrl("src/test/resources/inrupt-alice.json")));
    }

    @Test
    void loadRdfa() throws MalformedURLException {
        DataRepository dataRepository = new DataRepository();
        dataRepository.loadRdfa(TestUtils.getFileUrl("src/test/resources/rdfa-sample.html"), TestData.SAMPLE_BASE);
        assertEquals(1, dataRepositorySize(dataRepository));
    }

    @Test
    void loadRdfaBadUrl() {
        DataRepository dataRepository = new DataRepository();
        assertThrows(TestHarnessInitializationException.class, () -> dataRepository.loadRdfa(new URL("file:/missing.txt"), TestData.SAMPLE_BASE));
    }

    @Test
    void loadRdfaBadData() {
        DataRepository dataRepository = new DataRepository();
        assertThrows(TestHarnessInitializationException.class, () -> dataRepository.loadRdfa(TestUtils.getFileUrl("src/test/resources/jsonld-sample.json"), TestData.SAMPLE_BASE));
    }

    @Test
    void testOverriddenMethods() {
        DataRepository dataRepository = new DataRepository();
        File dataDir = new File("/tmp");
        dataRepository.setDataDir(dataDir);
        File newDataDir = dataRepository.getDataDir();
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
        DataRepository dataRepository = new DataRepository();
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            Statement st = Values.getValueFactory().createStatement(iri(TestData.SAMPLE_BASE + "/bob"), RDF.TYPE, FOAF.PERSON);
            conn.add(st);
        }
        return dataRepository;
    }

    private long dataRepositorySize(DataRepository dataRepository) {
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            return conn.size();
        }
    }

    private Reader getBadReader() {
        Reader reader = Reader.nullReader();
        try {
            reader.close();
        } catch (IOException e) {
            logger.error("Failed to close a null reader in tests");
        }
        return reader;
    }
}
