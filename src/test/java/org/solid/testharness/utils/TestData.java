package org.solid.testharness.utils;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.solid.common.vocab.DCTERMS;
import org.solid.common.vocab.FOAF;
import org.solid.common.vocab.RDF;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static org.eclipse.rdf4j.model.util.Values.iri;

public final class TestData {
    public static final String SAMPLE_BASE = "https://example.org";
    public static final String SAMPLE_NS = SAMPLE_BASE + "/";
    public static final String BOB = "bob";

    public static final String PREFIXES = Namespaces.generateAllTurtlePrefixes() +
            "@prefix ex: <" + SAMPLE_NS + "> .\n";

    public static final String SAMPLE_HTML = "<html xmlns=\"http://www.w3.org/1999/xhtml\" " +
            "xmlns:dc=\"http://purl.org/dc/terms/\">" +
            "<body about=\"doc\">" +
            "   <h1 property=\"dc:title\">TITLE</h1>" +
            "</body></html>";
    public static final String SAMPLE_HTML_TRIPLE = String.format("<%s> <%s> \"%s\" .",
            iri(SAMPLE_NS, "doc"), DCTERMS.title, "TITLE");

    public static final String SAMPLE_TURTLE = String.format("<%s> a <%s> .",
            iri(SAMPLE_NS, BOB), FOAF.Person);
    public static final String SAMPLE_TRIPLE = String.format("<%s> <%s> <%s> .",
            iri(SAMPLE_NS, BOB), RDF.type, FOAF.Person);
    public static final String SAMPLE_JSONLD = String.format("{\"@id\": \"%s\", \"@type\": \"%s\"}",
            iri(SAMPLE_NS, BOB), FOAF.Person);

    public static void insertData(final DataRepository dataRepository, final String turtle) throws IOException {
        insertData(dataRepository, new StringReader(turtle));
    }

    public static void insertData(final DataRepository dataRepository, final Reader reader) throws IOException {
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            conn.add(reader, RDFFormat.TURTLE);
        }
    }

    private TestData() { }
}

