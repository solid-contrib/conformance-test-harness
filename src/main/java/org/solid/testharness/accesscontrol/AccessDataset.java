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
package org.solid.testharness.accesscontrol;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.solid.testharness.config.TestSubject;
import org.solid.testharness.http.Client;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;

public interface AccessDataset {
    String READ = "read";
    String WRITE = "write";
    String APPEND = "append";
    String CONTROL = "control";
    String CONTROL_READ = "controlRead";
    String CONTROL_WRITE = "controlWrite";

    TestSubject.AccessControlMode getMode();

    Model getModel();

    void setModel(Model model);

    default String asTurtle() {
        if (getModel() == null) {
            return "";
        }
        final StringWriter sw = new StringWriter();
        final RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, sw);
        rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true)
                .set(BasicWriterSettings.INLINE_BLANK_NODES, true);
        Rio.write(getModel(), rdfWriter);
        return sw.toString();
    }

    default String asSparqlInsert() {
        if (getModel() == null) {
            return "";
        }
        final StringWriter sw = new StringWriter();
        final RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, sw);
        rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true)
                .set(BasicWriterSettings.INLINE_BLANK_NODES, true);
        rdfWriter.startRDF();
        for (Namespace ns: getModel().getNamespaces()) {
            rdfWriter.handleNamespace(ns.getPrefix(), ns.getName());
        }
        sw.append("INSERT DATA {");
        for (Statement st: getModel()) {
            rdfWriter.handleStatement(st);
        }
        rdfWriter.endRDF();
        sw.append("}");
        // modify the prefixes to be SPARQL format
        return sw.toString().replaceAll("@prefix ([^:]+): <([^>]+)> .", "PREFIX $1: <$2>");
    }

    default void parseTurtle(String data, String baseUri) throws IOException {
        final Model model = Rio.parse(new StringReader(data), baseUri, RDFFormat.TURTLE);
        setModel(model);
    }

    default boolean isSubsetOf(AccessDataset otherAccessDataset) {
        if (getModel() == null || otherAccessDataset == null || otherAccessDataset.getModel() == null) {
            return false;
        }
        return Models.isSubset(getModel(), otherAccessDataset.getModel());
    }

    void apply(Client client, URI uri) throws Exception;
}
