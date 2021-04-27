package org.solid.testharness.utils;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.CDI;
import javax.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    public enum ConstructMode {
        SHALLOW,
        DEEP,
        LIST;
    }

    private static final String SERVER_GRAPH = "CONSTRUCT {<%s> ?p ?o} WHERE {<%s> ?p ?o}";
    private static final String SERVER_GRAPH_2 = "CONSTRUCT {<%s> ?p ?o. ?o ?p1 ?o1} WHERE {<%s> ?p ?o. OPTIONAL {?o ?p1 ?o1}}";
    private static final String SERVER_GRAPH_LIST = "CONSTRUCT {" +
            "  <%s> ?p ?o. ?o ?p1 ?o1 . " +
            "  ?listRest rdf:first ?head ; rdf:rest ?tail ." +
            "} WHERE {" +
            "  <%s> ?p ?o." +
            "  OPTIONAL {?o ?p1 ?o1 . ?o rdf:rest* ?listRest . ?listRest rdf:first ?head ; rdf:rest ?tail .}" +
            "}";

    protected DataModelBase(IRI subject) {
        this(subject, ConstructMode.SHALLOW);
    }
    protected DataModelBase(IRI subject, ConstructMode mode) {
        DataRepository dataRepository = CDI.current().select(DataRepository.class).get();
        this.subject = subject;
        String query;
        switch (mode) {
            case DEEP:
                query = SERVER_GRAPH_2;
                break;
            case LIST:
                query = SERVER_GRAPH_LIST;
                break;
            default:
                query = SERVER_GRAPH;
                break;
        }
        model = Repositories.graphQuery(dataRepository, String.format(query, subject, subject), r -> QueryResults.asModel(r));
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

    public String getTypesList() {
        Set<Value> values = model.filter(subject, RDF.TYPE, null).objects();
        return values.stream().map(v -> Namespaces.shorten((IRI)v)).collect(Collectors.joining(" "));
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
                    throw new RuntimeException("Failed to create instance of " + clazz.getName());
                }
            }).collect(Collectors.toList());
        }
        return null;
    }

    protected <T extends DataModelBase> List<T> getModelCollectionList(IRI predicate, Class<T> clazz) {
        Resource node = Models.objectResource(model.filter(subject, predicate, null)).orElse(null);
        if (node != null) {
            List<Value> values = RDFCollections.asValues(model, node, new ArrayList<Value>());
            return values.stream().filter(Value::isIRI).map(v -> {
                try {
                    return clazz.getDeclaredConstructor(IRI.class).newInstance((IRI) v);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    logger.error("Failed to create instance of {}", clazz.getName());
                    throw new RuntimeException("Failed to create instance of " + clazz.getName());
                }
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

    protected BNode getAsBNode(IRI predicate) {
        Set<Value> values = model.filter(subject, predicate, null).objects();
        if (values.size() > 0) {
            Value value = values.iterator().next();
            return value.isBNode() ? (BNode) value : null;
        }
        return null;
    }

    protected String getLiteralAsString(IRI predicate) {
        return getLiteralAsString(subject, predicate);
    }

    protected String getLiteralAsString(Resource subject, IRI predicate) {
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

    protected LocalDate getLiteralAsDate(IRI predicate) {
        Optional<Literal> value = Models.getPropertyLiteral(model, subject, predicate);
        return value.map(Literal::calendarValue).map(v -> LocalDate.of(
                v.getYear(),
                v.getMonth(),
                v.getDay())).orElse(null);
    }

    protected LocalDateTime getLiteralAsDateTime(IRI predicate) {
        Optional<Literal> value = Models.getPropertyLiteral(model, subject, predicate);
        return value.map(Literal::calendarValue).map(v -> LocalDateTime.of(
                v.getYear(),
                v.getMonth(),
                v.getDay(),
                v.getHour(),
                v.getMinute(),
                v.getSecond(),
                v.getMillisecond())).orElse(null);
    }
}
