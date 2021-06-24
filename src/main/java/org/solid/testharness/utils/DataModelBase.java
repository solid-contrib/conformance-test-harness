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

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.util.Connections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.RDF;

import javax.enterprise.inject.spi.CDI;
import javax.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
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
        INC_REFS,
        DEEP,
        DEEP_WITH_LISTS;
    }

    protected DataModelBase(final IRI subject) {
        this(subject, ConstructMode.SHALLOW);
    }
    protected DataModelBase(final IRI subject, final ConstructMode mode) {
        requireNonNull(subject, "subject is required");
        final DataRepository dataRepository = CDI.current().select(DataRepository.class).get();
        this.subject = subject;
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            model = QueryResults.asModel(conn.getStatements(subject, null, null));
            final List<Statement> additions = new ArrayList<>();
            if (mode == ConstructMode.DEEP || mode == ConstructMode.DEEP_WITH_LISTS) {
                // add triples referenced by the objects found so far
                model.objects().stream()
                        .filter(Value::isResource)
                        .map(Resource.class::cast)
                        .forEach(o -> additions.addAll(Iterations.asList(conn.getStatements(o, null, null))));
            }
            if (mode == ConstructMode.DEEP_WITH_LISTS) {
                // add any RDF lists referenced by objects found so far
                model.objects().stream()
                        .filter(Value::isResource)
                        .map(o -> conn.getStatements((Resource) o, RDF.first, null))
                        .filter(RepositoryResult::hasNext)
                        .map(RepositoryResult::next)
                        .map(Statement::getSubject)
                        .forEach(s -> additions.addAll(Connections.getRDFCollection(conn, s, new LinkedHashModel())));
            }
            if (mode == ConstructMode.INC_REFS) {
                // add triples that reference the subject
                additions.addAll(Iterations.asList(conn.getStatements(null, null, subject)));
            }
            model.addAll(additions);
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

    public String getTypesList() {
        final Set<Value> values = model.filter(subject, RDF.type, null).objects();
        return values.stream().map(v -> Namespaces.shorten((IRI)v)).collect(Collectors.joining(" "));
    }

    public String getAnchor() {
        return subject.getLocalName();
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
            return getModelList(clazz, values);
        }
        return null;
    }

    protected <T extends DataModelBase> List<T> getModelCollectionList(final IRI predicate, final Class<T> clazz) {
        final Resource node = Models.objectResource(model.filter(subject, predicate, null)).orElse(null);
        if (node != null) {
            final List<Value> values = RDFCollections.asValues(model, node, new ArrayList<>());
            return getModelList(clazz, values);
        }
        return null;
    }

    protected <T extends DataModelBase> List<T> getModelListByObject(final IRI predicate, final Class<T> clazz) {
        final Set<Value> values = model.filter(null, predicate, subject).subjects()
                .stream().map(o -> (Value) o).collect(Collectors.toSet());
        if (!values.isEmpty()) {
            return getModelList(clazz, values);
        }
        return null;
    }

    private <T extends DataModelBase> List<T> getModelList(final Class<T> clazz, final Collection<Value> values) {
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

    protected ZonedDateTime getLiteralAsDateTime(final IRI predicate) {
        final Optional<Literal> value = Models.getPropertyLiteral(model, subject, predicate);
        return value.map(Literal::stringValue).map(v -> ZonedDateTime.parse(v)).orElse(null);
    }
}
