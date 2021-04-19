package org.solid.testharness.utils;

public class TestData {
    public static final String SAMPLE_BASE = "http://example.org";

    public static final String SAMPLE_HTML = "<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">" +
            "<body about=\"doc\">" +
            "   <h1 property=\"dc:title\">TITLE</h1>" +
            "</body></html>";
    public static final String SAMPLE_HTML_TRIPLE = "<http://example.org/doc> <http://purl.org/dc/elements/1.1/title> \"TITLE\" .";

    public static final String SAMPLE_JSONLD = "{\"@id\": \"http://example.org/bob\", \"@type\": \"http://xmlns.com/foaf/0.1/Person\"}";
    public static final String SAMPLE_JSONLD_TRIPLE = "<http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .";

    public static final String SAMPLE_TURTLE = "<http://example.org/bob> a <http://xmlns.com/foaf/0.1/Person> .";
    public static final String SAMPLE_TURTLE_TRIPLE = "<http://example.org/bob> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://xmlns.com/foaf/0.1/Person> .";

    public static final String SAMPLE_EXPORTED_TRIPLE = "<http://example.org/bob> a <http://xmlns.com/foaf/0.1/Person> .";
}

