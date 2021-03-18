package org.solid.testharness.reporting;

import java.io.File;
import org.solid.common.vocab.EARL;

public class TestResult {
    public TestResult(File resultJson) {
        // map from Json to object
    }
    public String toRdf() {
        // convert to RDF (EARL based)
//        EARL.Assertion
        return null;
    }
}

/*
ModelBuilder builder = new ModelBuilder();

// set some namespaces
builder.setNamespace("ex", "http://example.org/").setNamespace(FOAF.NS);

builder.namedGraph("ex:graph1")      // add a new named graph to the model
       .subject("ex:john")        // add  several statements about resource ex:john
	 .add(FOAF.NAME, "John")  // add the triple (ex:john, foaf:name "John") to the named graph
	 .add(FOAF.AGE, 42)
	 .add(FOAF.MBOX, "john@example.org");

// add a triple to the default graph
builder.defaultGraph().add("ex:graph1", RDF.TYPE, "ex:Graph");

// return the Model object
Model m = builder.build();
 */
