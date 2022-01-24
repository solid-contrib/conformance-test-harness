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

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.junit.jupiter.api.Test;
import org.solid.common.vocab.DOAP;
import org.solid.common.vocab.EARL;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class NamespacesTest {
    @Test
    void addToRepository() throws Exception {
        final DataRepository dataRepository = new DataRepository();
        assertNotNull(dataRepository);
        dataRepository.postConstruct();
        final StringReader reader = new StringReader(
                "<https://github.com/solid/conformance-test-harness/> a <http://www.w3.org/ns/earl#Software> ."
        );
        TestUtils.insertData(dataRepository, reader);
        assertTrue(TestUtils.repositoryToString(dataRepository).contains("earl:Software"));
    }

    @Test
    void addToModel() {
        final Model model = new LinkedHashModel();
        assertEquals(0, model.getNamespaces().size());
        Namespaces.addToModel(model);
        assertNotEquals(0, model.getNamespaces().size());
    }

    @Test
    void shortenNull() {
        assertThrows(NullPointerException.class, () -> Namespaces.shorten(null));
    }

    @Test
    void shorten() {
        assertEquals("td:Testcase", Namespaces.shorten(iri("http://www.w3.org/2006/03/test-description#Testcase")));
    }

    @Test
    void shortenUnknown() {
        assertEquals("https://example.org#Unknown", Namespaces.shorten(iri("https://example.org#Unknown")));
    }

    @Test
    void buildAllTurtlePrefixes() {
        final String prefixes = Namespaces.generateAllTurtlePrefixes();
        assertEquals(14, prefixes.split("\n").length);
        assertTrue(prefixes.startsWith("prefix "));
    }

    @Test
    void buildTurtlePrefixesNullOrEmpty() {
        final List<String> empty = null;
        assertEquals("", Namespaces.generateTurtlePrefixes(empty));
        assertEquals("", Namespaces.generateTurtlePrefixes(Collections.emptyList()));
    }

    @Test
    void buildTurtlePrefixes1() {
        assertEquals("prefix earl: <http://www.w3.org/ns/earl#>\n",
                Namespaces.generateTurtlePrefixes(List.of(EARL.PREFIX))
        );
    }

    @Test
    void buildTurtlePrefixes2() {
        assertEquals("prefix earl: <http://www.w3.org/ns/earl#>\nprefix doap: <http://usefulinc.com/ns/doap#>\n",
                Namespaces.generateTurtlePrefixes(List.of(EARL.PREFIX, DOAP.PREFIX))
        );
    }

    @Test
    void buildHtmlPrefixesNullOrEmpty() {
        final List<String> empty = null;
        assertEquals("", Namespaces.generateRdfaPrefixes(empty));
        assertEquals("", Namespaces.generateRdfaPrefixes(Collections.emptyList()));
    }

    @Test
    void buildHtmlPrefixes1() {
        assertEquals("earl: http://www.w3.org/ns/earl#", Namespaces.generateRdfaPrefixes(List.of(EARL.PREFIX)));
    }

    @Test
    void buildHtmlPrefixes2() {
        assertEquals("earl: http://www.w3.org/ns/earl# doap: http://usefulinc.com/ns/doap#",
                Namespaces.generateRdfaPrefixes(List.of(EARL.PREFIX, DOAP.PREFIX))
        );
    }
}
