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

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.solid.testharness.http.HttpUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class TestUtils {
    public static URL getFileUrl(final String file) throws MalformedURLException {
        return Path.of(file).normalize().toUri().toURL();
    }

    public static URI getPathUri(final String path) {
        final String uri = Path.of(path).toAbsolutePath().normalize().toUri().toString();
        return URI.create(HttpUtils.ensureNoSlashEnd(uri));
    }

    public static HttpResponse<Void> mockVoidResponse(final int status) {
        return mockVoidResponse(status, Collections.emptyMap());
    }

    public static HttpResponse<Void> mockVoidResponse(final int status, final Map<String, List<String>> headers) {
        final HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(status);
        final HttpHeaders mockHeaders = HttpHeaders.of(headers, (k, v) -> true);
        when(mockResponse.headers()).thenReturn(mockHeaders);
        return mockResponse;
    }

    public static HttpResponse<String> mockStringResponse(final int status, final String body) {
        return mockStringResponse(status, body, Collections.emptyMap());
    }

    public static HttpResponse<String> mockStringResponse(final int status, final String body,
                                                          final Map<String, List<String>> headers) {
        final HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(status);
        when(mockResponse.body()).thenReturn(body);
        final HttpHeaders mockHeaders = HttpHeaders.of(headers, (k, v) -> true);
        when(mockResponse.headers()).thenReturn(mockHeaders);
        return mockResponse;
    }

    public static String toTurtle(final Model model) {
        Namespaces.addToModel(model);
        final StringWriter sw = new StringWriter();
        final RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, sw);
        rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true)
                .set(BasicWriterSettings.INLINE_BLANK_NODES, true);
        Rio.write(model, rdfWriter);
        return sw.toString();
    }

    public static String toTriples(final Model model) {
        Namespaces.addToModel(model);
        final StringWriter sw = new StringWriter();
        final RDFWriter rdfWriter = Rio.createWriter(RDFFormat.NTRIPLES, sw);
        rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true)
                .set(BasicWriterSettings.INLINE_BLANK_NODES, true);
        Rio.write(model, rdfWriter);
        return sw.toString();
    }

    public static Model loadTurtleFromFile(final String file) throws IOException {
        final InputStream is = Files.newInputStream(Path.of(file));
        final Model model = Rio.parse(is, RDFFormat.TURTLE);
        return model;
    }

    public static Model loadTurtleFromString(final String data) throws IOException {
        final Model model = Rio.parse(new StringReader(data), RDFFormat.TURTLE);
        return model;
    }

    public static String loadStringFromFile(final String file) throws IOException {
        return Files.readString(Path.of(file));
    }

    private TestUtils() { }
}
