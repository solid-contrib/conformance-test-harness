package org.solid.testharness.utils;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.solid.common.vocab.*;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Namespaces {
    private static Map<String, Namespace> namespaces;

    public static final String TEST_HARNESS_URI = "https://github.com/solid/conformance-test-harness/";
    public static final String TEST_HARNESS_PREFIX = "test-harness";
    public static final String TURTLE_PREFIX_FORMAT = "@prefix %s: <%s> .\n";
    public static final String HTML_PREFIX_FORMAT = "%s: %s";

    private Namespaces() {
    }

    public static String shorten(@NotNull IRI iri) {
        String term = iri.stringValue();
        for (Namespace namespace : namespaces.values()) {
            if (term.startsWith(namespace.namespace)) {
                return term.replace(namespace.namespace, namespace.prefix + ":");
            }
        };
        return term;
    }

    public static String generateTurtlePrefixes(List<String> prefixes) {
        if (prefixes == null || prefixes.isEmpty()) {
            return "";
        }
        return prefixes.stream().filter(p -> namespaces.containsKey(p)).map(p -> String.format(TURTLE_PREFIX_FORMAT, p, namespaces.get(p).namespace)).collect(Collectors.joining());
    }

    public static String generateHtmlPrefixes(List<String> prefixes) {
        if (prefixes == null || prefixes.isEmpty()) {
            return "";
        }
        return prefixes.stream().filter(p -> namespaces.containsKey(p)).map(p -> String.format(HTML_PREFIX_FORMAT, p, namespaces.get(p).namespace)).collect(Collectors.joining(" "));
    }

    public static void addToRepository(Repository repository) {
        try (RepositoryConnection conn = repository.getConnection()) {
            namespaces.forEach((k, v) -> conn.setNamespace(k, v.namespace));
        }
    }

    static {
        namespaces = new HashMap<>();
        namespaces.put(TEST_HARNESS_PREFIX, new Namespace(TEST_HARNESS_PREFIX, TEST_HARNESS_URI));
        namespaces.put(EARL.PREFIX, new Namespace(EARL.PREFIX, EARL.NAMESPACE));
        namespaces.put(DOAP.PREFIX, new Namespace(DOAP.PREFIX, DOAP.NAMESPACE));
        namespaces.put(SOLID.PREFIX, new Namespace(SOLID.PREFIX, SOLID.NAMESPACE));
        namespaces.put(SOLID_TEST.PREFIX, new Namespace(SOLID_TEST.PREFIX, SOLID_TEST.NAMESPACE));
        namespaces.put(DCTERMS.PREFIX, new Namespace(DCTERMS.PREFIX, DCTERMS.NAMESPACE));
        namespaces.put(XSD.PREFIX, new Namespace(XSD.PREFIX, XSD.NAMESPACE));
        namespaces.put(TD.PREFIX, new Namespace(TD.PREFIX, TD.NAMESPACE));
    }

    private static class Namespace {
        String prefix;
        String namespace;
        Namespace(String prefix, String namespace) {
            this.prefix = prefix;
            this.namespace = namespace;
        }
    }
}
