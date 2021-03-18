package org.solid.testharness.utils;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataRepositoryTest {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.utils.DataRepositoryTest");

    @Test
    void exportWriter() {
        StringWriter wr = new StringWriter();
        DataRepository repository = DataRepository.getInstance();
        try (RepositoryConnection conn = repository.getConnection()) {
            Statement st = Values.getValueFactory().createStatement(iri("http://example.org/bob"), RDF.TYPE, FOAF.PERSON);
            conn.add(st);
            repository.export(wr);
            assertTrue(wr.toString().contains("<http://example.org/bob> a <http://xmlns.com/foaf/0.1/Person> ."));
        }
    }

    @Test
    void exportStream() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        DataRepository repository = DataRepository.getInstance();
        try (RepositoryConnection conn = repository.getConnection()) {
            Statement st = Values.getValueFactory().createStatement(iri("http://example.org/bob"), RDF.TYPE, FOAF.PERSON);
            conn.add(st);
            repository.export(os);
            assertTrue(os.toString().contains("<http://example.org/bob> a <http://xmlns.com/foaf/0.1/Person> ."));
        }
    }
}
