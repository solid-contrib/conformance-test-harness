package org.solid.testharness.reporting;

import org.eclipse.rdf4j.model.IRI;
import org.solid.common.vocab.DCTERMS;
import org.solid.common.vocab.TD;
import org.solid.testharness.utils.DataModelBase;

import java.util.List;

public class SpecificationTestCase extends DataModelBase {
    public SpecificationTestCase(final IRI subject) {
        super(subject);
    }

    public String getTitle() {
        return getLiteralAsString(DCTERMS.title);
    }

    public String getDescription() {
        return getLiteralAsString(DCTERMS.description);
    }

    public String getSpecificationReference() {
        return getIriAsString(TD.specificationReference);
    }

    public List<TestCase> getTestCases() {
        return getModelList(DCTERMS.hasPart, TestCase.class);
    }

    public String getAnchor() {
        return subject.getLocalName();
    }
}
