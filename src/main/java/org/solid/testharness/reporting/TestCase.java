package org.solid.testharness.reporting;

import org.eclipse.rdf4j.model.IRI;
import org.solid.common.vocab.DCTERMS;
import org.solid.common.vocab.TD;
import org.solid.testharness.utils.DataModelBase;

import java.util.Map;

import static org.eclipse.rdf4j.model.util.Values.iri;

public class TestCase extends DataModelBase {
    private static Map<IRI, String> statusLookup = Map.of(
            iri(TD.NS, "unreviewed"), "Unreviewed",
            iri(TD.NS, "accepted"), "Accepted"
    );

    public TestCase(IRI subject) {
        super(subject);
    }

    public String getTitle() {
        return getLiteralAsString(DCTERMS.title);
    }

    public String getLevel() {
        return getLiteralAsString(DCTERMS.subject);
    }

    public String getStatus() {
        return statusLookup.get(getAsIri(TD.reviewStatus));
    }

    public String getOutcome() {
        return null;
    }
}
