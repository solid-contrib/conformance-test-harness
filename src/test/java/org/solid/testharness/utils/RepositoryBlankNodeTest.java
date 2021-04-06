package org.solid.testharness.utils;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RepositoryBlankNodeTest {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.utils.RepositoryBlankNodeTest");

    @Test
    void losingBlankNodes() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("blank-node-example.ttl");
        Model model = Rio.parse(is, RDFFormat.TURTLE);
        assertEquals(43, model.size());
        StringWriter sw = new StringWriter();
        RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, sw);
        rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true).set(BasicWriterSettings.INLINE_BLANK_NODES, true);
        Rio.write(model, rdfWriter);
        logger.debug("blank-node-example.ttl\n{}", sw.toString());
    }

    @Test
    void losingBlankNodes2() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("formatted-blank-node-example.ttl");
        Model model = Rio.parse(is, RDFFormat.TURTLE);
        assertEquals(43, model.size());
        StringWriter sw = new StringWriter();
        RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, sw);
        rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true).set(BasicWriterSettings.INLINE_BLANK_NODES, true);
        Rio.write(model, rdfWriter);
        logger.debug("blank-node-example.ttl\n{}", sw.toString());
    }
}
