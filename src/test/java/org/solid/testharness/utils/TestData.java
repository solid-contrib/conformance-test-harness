package org.solid.testharness.utils;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public final class TestData {
    public static final String SAMPLE_BASE = "http://example.org";
    public static final String PREFIXES = "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
            "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
            "@prefix dcterms: <http://purl.org/dc/terms/> .\n" +
            "@prefix earl: <http://www.w3.org/ns/earl#> .\n" +
            "@prefix td: <http://www.w3.org/2006/03/test-description#> .\n" +
            "@prefix doap: <http://usefulinc.com/ns/doap#> .\n" +
            "@prefix ex: <http://example.org/> .\n" +
            "@prefix " + Namespaces.TEST_HARNESS_PREFIX + ": <" + Namespaces.TEST_HARNESS_URI + ">.\n";

    public static final String SAMPLE_HTML = "<html xmlns=\"http://www.w3.org/1999/xhtml\" " +
            "xmlns:dc=\"http://purl.org/dc/elements/1.1/\">" +
            "<body about=\"doc\">" +
            "   <h1 property=\"dc:title\">TITLE</h1>" +
            "</body></html>";
    public static final String SAMPLE_HTML_TRIPLE = "<http://example.org/doc> " +
            "<http://purl.org/dc/elements/1.1/title> \"TITLE\" .";

    public static final String SAMPLE_JSONLD = "{\"@id\": \"http://example.org/bob\", \"@type\": " +
            "\"http://xmlns.com/foaf/0.1/Person\"}";
    public static final String SAMPLE_JSONLD_TRIPLE = "<http://example.org/bob> " +
            "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .";

    public static final String SAMPLE_TURTLE = "<http://example.org/bob> a <http://xmlns.com/foaf/0.1/Person> .";
    public static final String SAMPLE_TURTLE_TRIPLE = "<http://example.org/bob> " +
            "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .";

    public static final String SAMPLE_EXPORTED_TRIPLE = "<http://example.org/bob> a " +
            "<http://xmlns.com/foaf/0.1/Person> .";

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

