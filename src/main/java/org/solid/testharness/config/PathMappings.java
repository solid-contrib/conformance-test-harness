package org.solid.testharness.config;

import io.quarkus.arc.config.ConfigProperties;
import org.eclipse.rdf4j.model.IRI;

import java.net.URI;
import java.nio.file.Path;
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
        if (path.startsWith("http:") || path.startsWith("https:")) {
            return iri(path);
        }
        String finalPath = path.startsWith("file:") ? path : Path.of(path).toAbsolutePath().normalize().toUri().toString();
        Mapping mapping = mappings.stream().filter(m -> finalPath.startsWith(m.path)).findFirst().orElse(null);
        return mapping != null ? iri(finalPath.replace(mapping.path, mapping.prefix)) : iri(path);
    }

    public URI mapFeatureIri(IRI featureIri) {
        String location = featureIri.stringValue();
        Mapping mapping = mappings.stream().filter(m -> location.startsWith(m.prefix)).findFirst().orElse(null);
        return URI.create(mapping != null ? location.replace(mapping.prefix, mapping.path) : location);
    }

    public static class Mapping {
        private String prefix;
        private String path;

        public String getPrefix() {
            return prefix;
        }

        public String getPath() {
            return path;
        }

        public void setPrefix(String prefix) {
            if (prefix.endsWith("/")) {
                this.prefix = prefix.substring(0, prefix.length() - 1);
            } else {
                this.prefix = prefix;
            }
        }

        public void setPath(String path) {
            this.path = Path.of(path).toAbsolutePath().normalize().toUri().toString();
            if (this.path.endsWith("/")) {
                this.path = this.path.substring(0, this.path.length() - 1);
            }
        }

        public static Mapping create(String prefix, String path) {
            Mapping mapping = new Mapping();
            mapping.setPrefix(prefix);
            mapping.setPath(path);
            return mapping;
        }

        @Override
        public String toString() {
            return String.format("%s => %s", prefix, path);
        }
    }
}
