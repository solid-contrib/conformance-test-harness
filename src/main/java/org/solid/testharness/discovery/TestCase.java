package org.solid.testharness.discovery;

import org.eclipse.rdf4j.model.IRI;

import java.net.URI;

/**
 * Representation of a test case and it's location
 */
public class TestCase {
    private URI uri;
    private IRI identifier;

    public TestCase(URI uri, IRI identifier) {
        this.uri = uri;
        this.identifier = identifier;
        // handle remote or local
    }
}
