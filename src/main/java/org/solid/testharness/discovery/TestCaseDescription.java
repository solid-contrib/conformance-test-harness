package org.solid.testharness.discovery;

import org.eclipse.rdf4j.model.IRI;
import org.solid.testharness.reporting.TestResult;

/**
 * Test case description created by parsing RDF in the Test Suite Description Document.
 * After running the tests, the results can be added to this ready for report processing.
 */
public class TestCaseDescription {
    private String name;
    private String description;
    private TestCase testCase;
    private TestResult testResult;
    private IRI identifier;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TestCase getTestCase() {
        return testCase;
    }

    public void setTestCase(TestCase testCase) {
        this.testCase = testCase;
    }

    public TestResult getTestResult() {
        return testResult;
    }

    public void setTestResult(TestResult testResult) {
        this.testResult = testResult;
    }

    public IRI getIdentifier() {
        return identifier;
    }

    public void setIdentifier(IRI identifier) {
        this.identifier = identifier;
    }
}
