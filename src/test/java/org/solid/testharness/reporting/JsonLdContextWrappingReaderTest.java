package org.solid.testharness.reporting;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JsonLdContextWrappingReaderTest {
    String context = "\"http://schema.org/\"";
    String prefix = "{\"@context\": " + context + ", \"@graph\":";
    String suffix = "}";

    @Test
    public void nullReader() {
        Reader source = null;
        assertThrows(NullPointerException.class, () -> {
            BufferedReader br = new BufferedReader(new JsonLdContextWrappingReader(source, context));
        });
    }

    @Test
    public void nullContext() {
        Reader source = new StringReader("");
        assertThrows(IllegalArgumentException.class, () -> {
            BufferedReader br = new BufferedReader(new JsonLdContextWrappingReader(source, null));
        });
    }

    @Test
    public void emptyContext() {
        Reader source = new StringReader("");
        assertThrows(IllegalArgumentException.class, () -> {
            BufferedReader br = new BufferedReader(new JsonLdContextWrappingReader(source, " "));
        });
    }

    @Test
    public void emptyReader() {
        Reader source = new StringReader("");
        BufferedReader br = new BufferedReader(new JsonLdContextWrappingReader(source, context));
        String content = br.lines().collect(Collectors.joining());
        assertEquals(prefix + suffix, content);
    }

    @Test
    public void contentReader() {
        Reader source = new StringReader("\"content\"");
        BufferedReader br = new BufferedReader(new JsonLdContextWrappingReader(source, context));
        String content = br.lines().collect(Collectors.joining());
        assertEquals(prefix + "\"content\"" + suffix, content);
    }

}
