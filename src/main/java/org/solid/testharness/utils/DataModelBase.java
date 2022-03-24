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
import org.eclipse.rdf4j.model.datatypes.XMLDateTime;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.util.Connections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.RDF;
import org.solid.common.vocab.RDFS;

import javax.enterprise.inject.spi.CDI;
import javax.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class DataModelBase {
    private static final Logger logger = LoggerFactory.getLogger(DataModelBase.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    @NotNull
    protected Model model;
    @NotNull
    protected IRI subject;

    public enum ConstructMode {
        SHALLOW,
        INC_REFS,
        DEEP,
        DEEP_WITH_LISTS
    }

    protected DataModelBase(final IRI subject) {
        this(subject, ConstructMode.SHALLOW);
    }
    protected DataModelBase(final IRI subject, final ConstructMode mode) {
        requireNonNull(subject, "subject is required");
        final DataRepository dataRepository = CDI.current().select(DataRepository.class).get();
        this.subject = subject;
        try (
                RepositoryConnection conn = dataRepository.getConnection();
                var statements = conn.getStatements(subject, null, null)
        ) {
            model = QueryResults.asModel(statements);
            final List<Resource> resources = model.objects().stream()
                    .filter(Value::isResource)
                    .map(Resource.class::cast)
                    .collect(Collectors.toList());
            final List<Statement> additions = new ArrayList<>();
            if (mode == ConstructMode.DEEP || mode == ConstructMode.DEEP_WITH_LISTS) {
                // add triples referenced by the objects found so far
                resources.forEach(o -> {
                    try (var statements2 = conn.getStatements(o, null, null)) {
                        additions.addAll(Iterations.asList(statements2));
                    }
                });
            }
            if (mode == ConstructMode.DEEP_WITH_LISTS) {
                // add any RDF lists referenced by objects found so far
                resources.stream()
                        .map(o -> {
                            try (var statements2 = conn.getStatements(o, RDF.first, null)) {
                                if (statements2.hasNext()) {
                                    return statements2.next();
                                } else {
                                    return null;
                                }
                            } // jacoco will not show full coverage for this try-with-resources line
                        })
                        .filter(Objects::nonNull)
                        .map(Statement::getSubject)
                        .forEach(s -> additions.addAll(Connections.getRDFCollection(conn, s, new LinkedHashModel())));
            }
            if (mode == ConstructMode.INC_REFS) {
                // add triples that reference the subject
                try (var statements2 = conn.getStatements(null, null, subject)) {
                    additions.addAll(Iterations.asList(statements2));
                }
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

    public Model getModel() {
        return model;
    }

    public String getTypesList() {
        final Set<Value> values = model.filter(subject, RDF.type, null).objects();
        return values.stream().map(v -> Namespaces.shorten((IRI)v)).collect(Collectors.joining(" "));
    }

    public String getAnchor() {
        return subject.getLocalName();
    }

    public List<String> getComments() {
        return getLiteralsAsStringList(RDFS.comment);
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
                throw new RuntimeException( "Failed to create instance of " + clazz.getName(), e);
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

    protected List<String> getLiteralsAsStringList(final IRI predicate) {
        final Set<Literal> value = Models.getPropertyLiterals(model, subject, predicate);
        return value.stream().map(Value::stringValue).collect(Collectors.toList());
    }

    protected int getLiteralAsInt(final IRI predicate) {
        final Optional<Literal> value = Models.getPropertyLiteral(model, subject, predicate);
        return value.map(Literal::intValue).orElse(0);
    }

    protected boolean getLiteralAsBoolean(final IRI predicate) {
        final Optional<Literal> value = Models.getPropertyLiteral(model, subject, predicate);
        return value.map(Literal::booleanValue).orElse(false);
    }

    protected XMLDateTime getLiteralAsDateTime(final IRI predicate) {
        return getLiteralAsDateTime(subject, predicate);
    }

    protected XMLDateTime getLiteralAsDateTime(final Resource subject, final IRI predicate) {
        final Optional<Literal> value = Models.getPropertyLiteral(model, subject, predicate);
        if (value.isEmpty()) return null;
        final ZonedDateTime dateTime;
        if (value.get().getDatatype().equals(XSD.DATE)) {
            dateTime = LocalDate.from(value.get().temporalAccessorValue()).atStartOfDay(ZoneId.of("Z"));
        } else {
            dateTime = ZonedDateTime.from(value.get().temporalAccessorValue());
        }
        return new XMLDateTime(dateTime.format(formatter));
    }
}
