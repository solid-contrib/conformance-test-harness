package org.solid.testharness.reporting;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.solid.common.vocab.*;
import org.solid.testharness.utils.DataModelBase;
import org.solid.testharness.utils.Namespaces;

import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;

public class ResultData extends DataModelBase {
    private Assertor assertor;

    public ResultData(IRI subject) {
        super(subject);
        assertor = new Assertor(iri(Namespaces.TEST_HARNESS_URI));
    }

    public String getPrefixes() {
        return Namespaces.generateHtmlPrefixes(List.of(RDF.PREFIX, RDFS.PREFIX, XSD.PREFIX, DCTERMS.PREFIX, DOAP.PREFIX,
                SOLID.PREFIX, SOLID_TEST.PREFIX, EARL.PREFIX, TD.PREFIX));
    }

    public List<SpecificationTestCase> getSpecificationTestCases() {
        return getModelList(DCTERMS.hasPart, SpecificationTestCase.class);
    }

    public String getSpecification() {
        return getIriAsString(DOAP.implements_);
    }

    public Assertor getAssertor() {
        return assertor;
    }

}
