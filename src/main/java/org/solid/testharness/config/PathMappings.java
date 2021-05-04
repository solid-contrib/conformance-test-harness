package org.solid.testharness.config;

import io.quarkus.arc.config.ConfigProperties;
import org.eclipse.rdf4j.model.IRI;

import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;

@ConfigProperties(prefix = "feature")
public class PathMappings {
    private List<Mapping> mappings;

    public void setMappings(List<Mapping> mappings) {
        this.mappings = (mappings != null && mappings.size() == 1 && mappings.get(0) == null) ? null : mappings;
    }
    public List<Mapping> getMappings() {
        return mappings;
    }

    @Override
    public String toString() {
        return mappings != null ? mappings.toString() : null;
    }

    public IRI unmapFeaturePath(String path) {
        Mapping mapping = mappings.stream().filter(m -> path.startsWith(m.path)).findFirst().orElse(null);
        return mapping != null ? iri(path.replace(mapping.path, mapping.prefix)) : iri(path);
    }

    public String mapFeatureIri(IRI featureIri) {
        String location = featureIri.stringValue();
        Mapping mapping = mappings.stream().filter(m -> location.startsWith(m.prefix)).findFirst().orElse(null);
        return mapping != null ? location.replace(mapping.prefix, mapping.path) : location;
    }

    public static class Mapping {
        public String prefix;
        public String path;

        public static Mapping create(String prefix, String path) {
            Mapping mapping = new Mapping();
            mapping.prefix = prefix;
            mapping.path = path;
            return mapping;
        }

        @Override
        public String toString() {
            return String.format("%s => %s", prefix, path);
        }
    }
}
