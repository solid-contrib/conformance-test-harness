package org.solid.testharness.utils;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.CDI;
import javax.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DataModelBase {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.utils.DataModelBase");

    @NotNull
    protected Model model;
    @NotNull
    protected IRI subject;

    private static final String SERVER_GRAPH = "CONSTRUCT {<%s> ?p ?o. ?o ?p1 ?o1} WHERE {<%s> ?p ?o. OPTIONAL {?o ?p1 ?o1}}";

    public DataModelBase(IRI subject) {
        DataRepository dataRepository = CDI.current().select(DataRepository.class).get();
        this.subject = subject;
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            model = Repositories.graphQuery(dataRepository, String.format(SERVER_GRAPH, subject, subject), r -> QueryResults.asModel(r));
        }
    }

    public IRI getSubjectIri() {
        return subject;
    }

    public String getSubject() {
        return subject.stringValue();
    }

    public int size() {
        return model.size();
    }

    protected String getIriAsString(IRI predicate) {
        Set<Value> values = model.filter(subject, predicate, null).objects();
        if (values.size() > 0) {
            return values.iterator().next().stringValue();
        }
        return null;
    }

    protected <T extends DataModelBase> List<T> getModelList(IRI predicate, Class<T> clazz) {
        Set<Value> values = model.filter(subject, predicate, null).objects();
        if (values.size() > 0) {
            return values.stream().filter(Value::isIRI).map(v -> {
                try {
                    return clazz.getDeclaredConstructor(IRI.class).newInstance((IRI)v);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    logger.error("Failed to create instance of {}", clazz.getName());
                }
                return null;
            }).collect(Collectors.toList());
        }
        return null;
    }

    protected IRI getAsIri(IRI predicate) {
        Set<Value> values = model.filter(subject, predicate, null).objects();
        if (values.size() > 0) {
            Value value = values.iterator().next();
            return value.isIRI() ? (IRI) value : null;
        }
        return null;
    }


    protected String getLiteralAsString(IRI predicate) {
        Optional<Literal> value = Models.getPropertyLiteral(model, subject, predicate);
        return value.map(Value::stringValue).orElse(null);
    }

    protected Set<String> getLiteralsAsStringSet(IRI predicate) {
        Set<Literal> value = Models.getPropertyLiterals(model, subject, predicate);
        return value.stream().map(Value::stringValue).collect(Collectors.toSet());
    }

    protected int getLiteralAsInt(IRI predicate) {
        Optional<Literal> value = Models.getPropertyLiteral(model, subject, predicate);
        return value.map(Literal::intValue).orElse(0);
    }

    protected boolean getLiteralAsBoolean(IRI predicate) {
        Optional<Literal> value = Models.getPropertyLiteral(model, subject, predicate);
        return value.map(Literal::booleanValue).orElse(false);
    }
}
