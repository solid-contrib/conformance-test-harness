package org.solid.testharness.utils;

import com.intuit.karate.Suite;
import com.intuit.karate.core.*;
import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class DataRepositoryTest {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.utils.DataRepositoryTest");

    @Inject
    DataRepository repository;

    @Test
    @Disabled
    void addFeatureResult() {
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
        when(str2.getStepLog()).thenReturn("STEP2 LOG");
        when(sr1.getStepResults()).thenReturn(List.of(str1, str2));

        repository.setupNamespaces("http://example.org/");
        repository.setTestSubject(iri("http://example.org/test"));
        repository.addFeatureResult(suite, fr);
        StringWriter sw = new StringWriter();
        repository.export(sw);
        logger.debug(sw.toString());
        assertTrue(sw.toString().contains("test-harness:DISPLAY_NAME"));
        assertTrue(sw.toString().contains("dcterms:title \"FEATURE NAME\""));
    }

    @Test
    @Disabled
    void exportWriter() {
        StringWriter wr = new StringWriter();
        try (RepositoryConnection conn = repository.getConnection()) {
            Statement st = Values.getValueFactory().createStatement(iri("http://example.org/bob"), RDF.TYPE, FOAF.PERSON);
            conn.add(st);
            repository.export(wr);
            assertTrue(wr.toString().contains("<http://example.org/bob> a <http://xmlns.com/foaf/0.1/Person> ."));
        }
    }

    @Test
    @Disabled
    void exportStream() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (RepositoryConnection conn = repository.getConnection()) {
            Statement st = Values.getValueFactory().createStatement(iri("http://example.org/bob"), RDF.TYPE, FOAF.PERSON);
            conn.add(st);
            repository.export(os);
            assertTrue(os.toString().contains("<http://example.org/bob> a <http://xmlns.com/foaf/0.1/Person> ."));
        }
    }

    @Test
    void losingBlankNodes() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("blank-node-example.ttl");
        Model model = Rio.parse(is, RDFFormat.TURTLE);
        assertEquals(43, model.size());
        StringWriter sw = new StringWriter();
        RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, sw);
        rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true).set(BasicWriterSettings.INLINE_BLANK_NODES, true);
        Rio.write(model, rdfWriter);
        logger.debug("blank-node-example.ttl\n{}", sw.toString());
    }

    @Test
    void losingBlankNodes2() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("formatted-blank-node-example.ttl");
        Model model = Rio.parse(is, RDFFormat.TURTLE);
        assertEquals(43, model.size());
        StringWriter sw = new StringWriter();
        RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, sw);
        rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true).set(BasicWriterSettings.INLINE_BLANK_NODES, true);
        Rio.write(model, rdfWriter);
        logger.debug("blank-node-example.ttl\n{}", sw.toString());
    }
}
