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
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.repository.util.RepositoryUtil;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.slf4j.Logger;

import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class TestUtils {
    public static URL getFileUrl(final String file) throws MalformedURLException {
        return Path.of(file).normalize().toUri().toURL();
    }

    public static URI getPathUri(final String path) {
        String uri = Path.of(path).toAbsolutePath().normalize().toUri().toString();
        if (uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        return URI.create(uri);
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

    public static void showModelDifferences(final Model modelA, final Model modelB, final Logger logger) {
        if (Models.isomorphic(modelA, modelB)) {
            logger.debug("Models are isomorphic");
        } else {
            logger.debug("Model A = {}, Model B = {}", modelA.size(), modelB.size());
            if (Models.isSubset(modelA, modelB)) {
                logger.debug("Model A is a subset of Model B");
            } else if (Models.isSubset(modelB, modelA)) {
                logger.debug("Model B is a subset of Model A");
            }
            logger.debug("\n== In Model A but not Model B ==\n{}", RepositoryUtil.difference(modelA, modelB)
                    .stream().map(s -> String.format("%s %s %s", s.getSubject(), s.getPredicate(), s.getObject()))
                    .collect(Collectors.joining("\n"))
            );

            logger.debug("\n== In Model B but not Model A ==\n{}", RepositoryUtil.difference(modelB, modelA)
                    .stream().map(s -> String.format("%s %s %s", s.getSubject(), s.getPredicate(), s.getObject()))
                    .collect(Collectors.joining("\n"))
            );
        }
    }

    private TestUtils() { }
}
