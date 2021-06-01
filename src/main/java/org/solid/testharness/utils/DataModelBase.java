/*
 * MIT License
 *
 * Copyright (c) 2021 Solid
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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

import static java.util.Objects.requireNonNull;

public class DataModelBase {
    private static final Logger logger = LoggerFactory.getLogger(DataModelBase.class);

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
    private static final String SERVER_GRAPH_2 = "CONSTRUCT {<%s> ?p ?o. ?o ?p1 ?o1} " +
            "WHERE {<%s> ?p ?o. OPTIONAL {?o ?p1 ?o1}}";
    private static final String SERVER_GRAPH_LIST = "CONSTRUCT {" +
            "  <%s> ?p ?o. ?o ?p1 ?o1 . " +
            "  ?listRest rdf:first ?head ; rdf:rest ?tail ." +
            "} WHERE {" +
            "  <%s> ?p ?o." +
            "  OPTIONAL {?o ?p1 ?o1 . ?o rdf:rest* ?listRest . ?listRest rdf:first ?head ; rdf:rest ?tail .}" +
            "}";

    protected DataModelBase(final IRI subject) {
        this(subject, ConstructMode.SHALLOW);
    }
    protected DataModelBase(final IRI subject, final ConstructMode mode) {
        requireNonNull(subject, "subject is required");
        final DataRepository dataRepository = CDI.current().select(DataRepository.class).get();
        this.subject = subject;
        final String query;
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
        model = Repositories.graphQuery(dataRepository,
                String.format(query, subject, subject),
                QueryResults::asModel
        );
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
        final Set<Value> values = model.filter(subject, RDF.TYPE, null).objects();
        return values.stream().map(v -> Namespaces.shorten((IRI)v)).collect(Collectors.joining(" "));
    }

    protected String getIriAsString(final IRI predicate) {
        final Set<Value> values = model.filter(subject, predicate, null).objects();
        if (!values.isEmpty()) {
            return values.iterator().next().stringValue();
        }
        return null;
    }

    protected <T extends DataModelBase> List<T> getModelList(final IRI predicate, final Class<T> clazz) {
        final Set<Value> values = model.filter(subject, predicate, null).objects();
        if (!values.isEmpty()) {
            return values.stream().filter(Value::isIRI).map(v -> {
                try {
                    return clazz.getDeclaredConstructor(IRI.class).newInstance((IRI)v);
                } catch (InstantiationException | IllegalAccessException |
                        InvocationTargetException | NoSuchMethodException e) {
                    logger.error("Failed to create instance of {}", clazz.getName());
                    throw (RuntimeException) new RuntimeException(
                            "Failed to create instance of " + clazz.getName()
                    ).initCause(e);
                }
            }).collect(Collectors.toList());
        }
        return null;
    }

    protected <T extends DataModelBase> List<T> getModelCollectionList(final IRI predicate, final Class<T> clazz) {
        final Resource node = Models.objectResource(model.filter(subject, predicate, null)).orElse(null);
        if (node != null) {
            final List<Value> values = RDFCollections.asValues(model, node, new ArrayList<Value>());
            return values.stream().filter(Value::isIRI).map(v -> {
                try {
                    return clazz.getDeclaredConstructor(IRI.class).newInstance((IRI) v);
                } catch (InstantiationException | IllegalAccessException |
                        InvocationTargetException | NoSuchMethodException e) {
                    logger.error("Failed to create instance of {}", clazz.getName());
                    throw (RuntimeException) new RuntimeException(
                            "Failed to create instance of " + clazz.getName()
                    ).initCause(e);
                }
            }).collect(Collectors.toList());
        }
        return null;
    }

    protected IRI getAsIri(final IRI predicate) {
        final Set<Value> values = model.filter(subject, predicate, null).objects();
        if (!values.isEmpty()) {
            final Value value = values.iterator().next();
            return value.isIRI() ? (IRI) value : null;
        }
        return null;
    }

    protected BNode getAsBNode(final IRI predicate) {
        final Set<Value> values = model.filter(subject, predicate, null).objects();
        if (!values.isEmpty()) {
            final Value value = values.iterator().next();
            return value.isBNode() ? (BNode) value : null;
        }
        return null;
    }

    protected String getLiteralAsString(final IRI predicate) {
        return getLiteralAsString(subject, predicate);
    }

    protected String getLiteralAsString(final Resource subject, final IRI predicate) {
        final Optional<Literal> value = Models.getPropertyLiteral(model, subject, predicate);
        return value.map(Value::stringValue).orElse(null);
    }

    protected Set<String> getLiteralsAsStringSet(final IRI predicate) {
        final Set<Literal> value = Models.getPropertyLiterals(model, subject, predicate);
        return value.stream().map(Value::stringValue).collect(Collectors.toSet());
    }

    protected int getLiteralAsInt(final IRI predicate) {
        final Optional<Literal> value = Models.getPropertyLiteral(model, subject, predicate);
        return value.map(Literal::intValue).orElse(0);
    }

    protected boolean getLiteralAsBoolean(final IRI predicate) {
        final Optional<Literal> value = Models.getPropertyLiteral(model, subject, predicate);
        return value.map(Literal::booleanValue).orElse(false);
    }

    protected LocalDate getLiteralAsDate(final IRI predicate) {
        final Optional<Literal> value = Models.getPropertyLiteral(model, subject, predicate);
        return value.map(Literal::calendarValue).map(v -> LocalDate.of(
                v.getYear(),
                v.getMonth(),
                v.getDay())).orElse(null);
    }

    protected LocalDateTime getLiteralAsDateTime(final IRI predicate) {
        final Optional<Literal> value = Models.getPropertyLiteral(model, subject, predicate);
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
