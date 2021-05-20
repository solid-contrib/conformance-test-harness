package org.solid.testharness.utils;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.solid.common.vocab.DOAP;
import org.solid.common.vocab.EARL;

import java.io.StringReader;
import java.io.StringWriter;
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
        TestData.insertData(dataRepository, reader);
        final StringWriter sw = new StringWriter();
        dataRepository.export(sw);
        assertTrue(sw.toString().contains("earl:Software"));
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
        assertEquals(10, prefixes.split("\n").length);
        assertTrue(prefixes.startsWith("@prefix "));
    }

    @Test
    void buildTurtlePrefixesNullOrEmpty() {
        final List<String> empty = null;
        assertEquals("", Namespaces.generateTurtlePrefixes(empty));
        assertEquals("", Namespaces.generateTurtlePrefixes(List.of()));
    }

    @Test
    void buildTurtlePrefixes1() {
        assertEquals("@prefix earl: <http://www.w3.org/ns/earl#> .\n",
                Namespaces.generateTurtlePrefixes(List.of(EARL.PREFIX))
        );
    }

    @Test
    void buildTurtlePrefixes2() {
        assertEquals("@prefix earl: <http://www.w3.org/ns/earl#> .\n@prefix doap: <http://usefulinc.com/ns/doap#> .\n",
                Namespaces.generateTurtlePrefixes(List.of(EARL.PREFIX, DOAP.PREFIX))
        );
    }

    @Test
    void buildHtmlPrefixesNullOrEmpty() {
        final List<String> empty = null;
        assertEquals("", Namespaces.generateHtmlPrefixes(empty));
        assertEquals("", Namespaces.generateHtmlPrefixes(List.of()));
    }

    @Test
    void buildHtmlPrefixes1() {
        assertEquals("earl: http://www.w3.org/ns/earl#", Namespaces.generateHtmlPrefixes(List.of(EARL.PREFIX)));
    }

    @Test
    void buildHtmlPrefixes2() {
        assertEquals("earl: http://www.w3.org/ns/earl# doap: http://usefulinc.com/ns/doap#",
                Namespaces.generateHtmlPrefixes(List.of(EARL.PREFIX, DOAP.PREFIX))
        );
    }
}
