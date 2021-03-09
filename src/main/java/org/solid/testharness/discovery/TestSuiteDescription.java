package org.solid.testharness.discovery;

import org.eclipse.rdf4j.model.IRI;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Representation of a test suite description document parsed from RDF
 */
public class TestSuiteDescription {
    private String name;
    private String description;
    private Map<IRI, TestCaseDescription> testCases = new HashMap<>();

    public void parseDocument(URI uri) {
        // parse document and create test cases
    }

    public TestCaseDescription getTestCaseDescription(IRI identifier) {
        return testCases.get(identifier);
    }
}
