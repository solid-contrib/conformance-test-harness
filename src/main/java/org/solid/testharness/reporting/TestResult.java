package org.solid.testharness.reporting;

import org.eclipse.rdf4j.model.IRI;
import org.solid.common.vocab.DCTERMS;
import org.solid.common.vocab.EARL;
import org.solid.testharness.utils.DataModelBase;

import java.time.LocalDateTime;

public class TestResult extends DataModelBase  {
    public TestResult(final IRI subject) {
        super(subject, ConstructMode.DEEP);
    }

    public String getOutcome() {
        return getIriAsString(EARL.outcome);
    }

    public LocalDateTime getDate() {
        return getLiteralAsDateTime(DCTERMS.date);
    }

}
