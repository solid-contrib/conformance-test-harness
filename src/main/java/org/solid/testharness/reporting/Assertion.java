package org.solid.testharness.reporting;

import org.eclipse.rdf4j.model.IRI;
import org.solid.common.vocab.EARL;
import org.solid.testharness.utils.DataModelBase;

import java.util.List;

public class Assertion extends DataModelBase {
    public Assertion(final IRI subject) {
        super(subject, ConstructMode.DEEP);
    }

    public String getAssertedBy() {
        return getIriAsString(EARL.assertedBy);
    }

    public String getTest() {
        return getIriAsString(EARL.test);
    }

    public String getTestSubject() {
        return getIriAsString(EARL.subject);
    }

    public String getMode() {
        return getIriAsString(EARL.mode);
    }

    public TestResult getResult() {
        final List<TestResult> results = getModelList(EARL.result, TestResult.class);
        if (!results.isEmpty()) {
            return results.get(0);
        } else {
            return null;
        }
    }
}
