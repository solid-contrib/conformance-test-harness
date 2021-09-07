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

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.solid.common.vocab.DCTERMS;
import org.solid.common.vocab.FOAF;
import org.solid.common.vocab.RDF;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

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
            conn.add(reader, SAMPLE_BASE, RDFFormat.TURTLE);
        }
    }

    public static void insertData(final DataRepository dataRepository, final URL url) throws IOException {
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            conn.add(url, SAMPLE_BASE, RDFFormat.TURTLE);
        }
    }

    private TestData() { }
}

