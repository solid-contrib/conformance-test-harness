package org.solid.testharness.discovery;

import org.eclipse.rdf4j.model.IRI;

import java.net.URI;

/**
 * Representation of a test case and it's location.
 */
public class TestCase {
    private URI uri;
    private IRI identifier;

    public TestCase(final URI uri, final IRI identifier) {
        this.uri = uri;
        this.identifier = identifier;
        // handle remote or local
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(final URI uri) {
        this.uri = uri;
    }

    public IRI getIdentifier() {
        return identifier;
    }

    public void setIdentifier(final IRI identifier) {
        this.identifier = identifier;
    }
}
