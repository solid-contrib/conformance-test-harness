package org.solid.testharness.reporting;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.solid.common.vocab.RDF;
import org.solid.common.vocab.TD;
import org.solid.testharness.utils.DataRepository;

import javax.enterprise.inject.spi.CDI;
import java.util.ArrayList;
import java.util.List;

public class ResultData {
    private DataRepository dataRepository;
    private String specification = "Solid Spec";
    private List<SpecificationTestCase> specificationTestCases = new ArrayList<>();

    public ResultData() {
        dataRepository = CDI.current().select(DataRepository.class).get();
    }

    public List<SpecificationTestCase> getSpecificationTestCases() {
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            RepositoryResult<Statement> statements = conn.getStatements(null, RDF.type, TD.SpecificationTestCase);
            statements.forEach(s -> {
                SpecificationTestCase specificationTestCase = new SpecificationTestCase((IRI) s.getSubject());
                specificationTestCases.add(specificationTestCase);
            });
        }
        return specificationTestCases;
    }

    public String getSpecification() {
        return specification;
    }
}
