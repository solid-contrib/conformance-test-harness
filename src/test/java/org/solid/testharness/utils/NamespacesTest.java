package org.solid.testharness.utils;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class NamespacesTest {
    @Test
    void addToRepository() {
        DataRepository dataRepository = new DataRepository();
        assertNotNull(dataRepository);
        dataRepository.postConstruct();
        StringReader reader = new StringReader("<https://github.com/solid/conformance-test-harness/> a <http://www.w3.org/ns/earl#Software> .");
        dataRepository.loadTurtle(reader);
        StringWriter sw = new StringWriter();
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
        assertEquals("http://example.org#Unknown", Namespaces.shorten(iri("http://example.org#Unknown")));
    }
}
