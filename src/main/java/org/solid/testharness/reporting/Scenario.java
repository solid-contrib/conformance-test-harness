package org.solid.testharness.reporting;

import org.eclipse.rdf4j.model.IRI;
import org.solid.common.vocab.DCTERMS;
import org.solid.common.vocab.EARL;
import org.solid.testharness.utils.DataModelBase;

import java.util.List;

public class Scenario extends DataModelBase {
    public Scenario(final IRI subject) {
        super(subject, ConstructMode.LIST);
    }

    public String getTitle() {
        return getLiteralAsString(DCTERMS.title);
    }

    public String getParent() {
        return getIriAsString(DCTERMS.isPartOf);
    }

    public Assertion getAssertion() {
        final List<Assertion> assertions = getModelList(EARL.assertions, Assertion.class);
        if (assertions != null) {
            return assertions.get(0);
        } else {
            return null;
        }
    }

    public List<Step> getSteps() {
        return getModelCollectionList(EARL.steps, Step.class);
    }
}
