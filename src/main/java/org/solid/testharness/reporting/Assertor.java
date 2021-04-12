package org.solid.testharness.reporting;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.solid.common.vocab.DOAP;
import org.solid.testharness.utils.DataModelBase;

import java.time.LocalDate;

public class Assertor extends DataModelBase {
    public Assertor(IRI subject) {
        super(subject, ConstructMode.DEEP);
    }

    public String getSoftwareName() {
        return getLiteralAsString(DOAP.name);
    }

    public String getDescription() {
        return getLiteralAsString(DOAP.description);
    }

    public LocalDate getCreatedDate() {
        return getLiteralAsDate(DOAP.created);
    }

    public String getDeveloper() {
        return getIriAsString(DOAP.developer);
    }

    public String getHomepage() {
        return getIriAsString(DOAP.homepage);
    }

    public String getRevision() {
        BNode release = getAsBNode(DOAP.release);
        if (release == null) {
            return null;
        }
        return getLiteralAsString(release, DOAP.revision);
    }
}
