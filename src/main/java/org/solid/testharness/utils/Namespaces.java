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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.solid.common.vocab.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.solid.testharness.api.RDFModel.iri;

public final class Namespaces {
    private static final Map<String, Namespace> namespaceMap;
    static final Map<String, String> specNamespacesMap = new HashMap<>();

    public static final String TESTS_REPO_URI = "https://github.com/solid/specification-tests/";
    public static final String RESULTS_UUID = UUID.randomUUID().toString();
    public static final String RESULTS_URI = TESTS_REPO_URI + RESULTS_UUID + "#";
    public static final String TEST_HARNESS_URI = "https://github.com/solid/conformance-test-harness/";
    public static final String TEST_HARNESS_PREFIX = "test-harness";
    public static final String SPECIFICATION_TESTS_IRI = Namespaces.RESULTS_URI + "tests";
    public static final String TURTLE_PREFIX_FORMAT = "prefix %s: <%s>\n";
    public static final String RDFA_PREFIX_FORMAT = "%s: %s";
    public static final IRI SPEC_RELATED_CONTEXT = iri("https://github.com/solid/specification-tests/specifications");
    public static final String SCHEMA_PREFIX = "schema";
    public static final String SCHEMA_NS = "http://schema.org/";

    public static String shorten(final IRI iri) {
        final String term = iri.stringValue();
        for (Namespace namespace : namespaceMap.values()) {
            if (term.startsWith(namespace.iri)) {
                return term.replace(namespace.iri, namespace.prefix + ":");
            }
        }
        return term;
    }

    public static String generateAllTurtlePrefixes() {
        return generateTurtlePrefixes(namespaceMap.keySet());
    }

    public static String generateTurtlePrefixes(final Collection<String> prefixes) {
        if (prefixes == null || prefixes.isEmpty()) {
            return "";
        }
        return prefixes.stream()
                .filter(namespaceMap::containsKey)
                .map(p -> String.format(TURTLE_PREFIX_FORMAT, p, namespaceMap.get(p).iri))
                .collect(Collectors.joining());
    }

    public static String generateRdfaPrefixes(final Collection<String> prefixes) {
        if (prefixes == null || prefixes.isEmpty()) {
            return "";
        }
        return prefixes.stream()
                .filter(namespaceMap::containsKey)
                .map(p -> String.format(RDFA_PREFIX_FORMAT, p, namespaceMap.get(p).iri))
                .collect(Collectors.joining(" "));
    }

    public static void addToRepository(final Repository repository) {
        try (RepositoryConnection conn = repository.getConnection()) {
            namespaceMap.forEach((k, v) -> conn.setNamespace(k, v.iri));
        }
    }

    public static void addToModel(final Model model) {
        namespaceMap.forEach((k, v) -> model.setNamespace(k, v.iri));
    }

    public static void clearSpecificationNamespaces() {
        specNamespacesMap.clear();
    }

    public static void addSpecification(final IRI iri) {
        specNamespacesMap.put(iri.stringValue(), "spec" + specNamespacesMap.size());
    }

    public static String getSpecificationNamespace(final IRI iri) {
        return specNamespacesMap.get(iri.getNamespace().replaceFirst("#?$", ""));
    }

    static {
        namespaceMap = new HashMap<>();
        namespaceMap.put(TEST_HARNESS_PREFIX, new Namespace(TEST_HARNESS_PREFIX, TEST_HARNESS_URI));
        namespaceMap.put(SPEC.PREFIX, new Namespace(SPEC.PREFIX, SPEC.NAMESPACE));
        namespaceMap.put(EARL.PREFIX, new Namespace(EARL.PREFIX, EARL.NAMESPACE));
        namespaceMap.put(DOAP.PREFIX, new Namespace(DOAP.PREFIX, DOAP.NAMESPACE));
        namespaceMap.put(SOLID.PREFIX, new Namespace(SOLID.PREFIX, SOLID.NAMESPACE));
        namespaceMap.put(SOLID_TEST.PREFIX, new Namespace(SOLID_TEST.PREFIX, SOLID_TEST.NAMESPACE));
        namespaceMap.put(DCTERMS.PREFIX, new Namespace(DCTERMS.PREFIX, DCTERMS.NAMESPACE));
        namespaceMap.put(XSD.PREFIX, new Namespace(XSD.PREFIX, XSD.NAMESPACE));
        namespaceMap.put(TD.PREFIX, new Namespace(TD.PREFIX, TD.NAMESPACE));
        namespaceMap.put(PROV.PREFIX, new Namespace(PROV.PREFIX, PROV.NAMESPACE));
        namespaceMap.put(RDF.PREFIX, new Namespace(RDF.PREFIX, RDF.NAMESPACE));
        namespaceMap.put(RDFS.PREFIX, new Namespace(RDFS.PREFIX, RDFS.NAMESPACE));
        namespaceMap.put(OWL.PREFIX, new Namespace(OWL.PREFIX, OWL.NAMESPACE));
        namespaceMap.put(SCHEMA_PREFIX, new Namespace(SCHEMA_PREFIX, SCHEMA_NS));
    }

    private static class Namespace {
        String prefix;
        String iri;
        Namespace(final String prefix, final String iri) {
            this.prefix = prefix;
            this.iri = iri;
        }
    }

    private Namespaces() { }
}
