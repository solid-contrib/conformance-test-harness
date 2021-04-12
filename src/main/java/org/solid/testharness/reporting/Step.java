package org.solid.testharness.reporting;

import org.eclipse.rdf4j.model.IRI;
import org.solid.common.vocab.DCTERMS;
import org.solid.common.vocab.EARL;
import org.solid.testharness.utils.DataModelBase;

public class Step extends DataModelBase {
    public Step(IRI subject) {
        super(subject);
    }

    public String getTitle() {
        return getLiteralAsString(DCTERMS.title);
    }
    public String getOutcome() {
        return getIriAsString(EARL.outcome);
    }
    public String getInfo() {
        return getLiteralAsString(EARL.info);
    }
}
