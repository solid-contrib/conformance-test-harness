package org.solid.testharness.utils;

import com.intuit.karate.Suite;
import com.intuit.karate.core.FeatureResult;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;

public class DataRepository extends SailRepository {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.utils.DataRepository");

    private static DataRepository INSTANCE;

    private DataRepository() {
        super(new MemoryStore());
    }
    private IRI assertor;

    public synchronized static DataRepository getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DataRepository();
        }
        return INSTANCE;
    }

    public void loadTurtle(URL url) {
        try (RepositoryConnection conn = getConnection()) {
            try {
                conn.add(url, RDFFormat.TURTLE);
            } catch (IOException e) {
                logger.error("Failed to read " + url, e);
            }
        } catch (RDF4JException e) {
            logger.error("Failed to parse data from " + url, e);
        }
    }

    public void loadTurtle(File file) {
        try (RepositoryConnection conn = getConnection()) {
            try {
                conn.add(file, RDFFormat.TURTLE);
            } catch (IOException e) {
                logger.error("Failed to read " + file, e);
            }
        } catch (RDF4JException e) {
            logger.error("Failed to parse data from " + file, e);
        }
    }

    public void addFeatureResult(Suite suite, FeatureResult fr) {
        try (RepositoryConnection conn = getConnection()) {
//            Resource head = bnode();
//            Statement st = Values.getValueFactory().createStatement(iri("http://example.org/bob"), RDF.TYPE, DOAP.NAME);
//            conn.add(st);

        } catch (RDF4JException e) {
            logger.error("Failed to load feature result", e);
        }
/*

    <TestSuiteDescriptionDoc> a earl:TestCriterion, earl:TestCase ;
        earl:assertions [...]

        // Assertion
        //   assertedBy Assertor  use earl:Software too
        //   subject    TestSubject use earl:Software too
        //   test       TestCriterion (a testable statement)
        //              TestRequirement subclass of TestCriterion (a higher level requirements tested by one of more subtests)  Feature, Scenario
        //              TestCase subclass of TestCriterion (an atomic test)                                                     Step
        //   result     TestResult
        //   mode       TestMode

suite
  startTime
  env
  featuresFound
  features
    Feature


fr
  feature Feature
    resource
    name

  scenarioResults[]
    stepResults[] StepResult
      step Step
      result Result
        status
        skipped
      stepLog

    scenario Scenario
      name - title
      steps[]

    startTime
    endTime
    durationNanos
 */
        // Assertion
        //   assertedBy Assertor  use earl:Software too
        //   subject    TestSubject use earl:Software too
        //   test       TestCriterion (a testable statement)
        //              TestRequirement subclass of TestCriterion (a higher level requirements tested by one of more subtests)  Feature, Scenario
        //              TestCase subclass of TestCriterion (an atomic test)                                                     Step
        //   result     TestResult
        //   mode       TestMode

    /*
    TestCriterion
        dct:title       Human readable title for the test criterion.
        dct:description Human readable description of the test criterion.
        dct:hasPart     Relationship to other test criteria that are part of this criterion.
        dct:isPartOf    Relationship to other test criteria of which this criterion is a part of.

     TestResult: info, output, pointer?
        dct:title
        dct:description
        dct:date

    TestOutcome  passed, failed, inapplicable, untested

     */
    }

    public void export(Writer wr) {
        try (RepositoryConnection conn = getConnection()) {
            RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, wr);
            rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true).set(BasicWriterSettings.INLINE_BLANK_NODES, true);
            conn.export(rdfWriter);
        } catch (RDF4JException e) {
            logger.error("Failed to generate test suite result report", e);
        }
    }

    public void export(OutputStream os) {
        try (RepositoryConnection conn = getConnection()) {
            RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, os);
            rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true).set(BasicWriterSettings.INLINE_BLANK_NODES, true);
            conn.export(rdfWriter);
        } catch (RDF4JException e) {
            logger.error("Failed to generate test suite result report", e);
        }
    }
}
