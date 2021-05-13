package org.solid.testharness.config;

import io.quarkus.arc.config.ConfigProperties;
import org.eclipse.rdf4j.model.IRI;
import org.solid.testharness.http.HttpUtils;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.eclipse.rdf4j.model.util.Values.iri;

@ConfigProperties(prefix = "feature")
public class PathMappings {
    private List<Mapping> mappings = Collections.emptyList();

    public void setMappings(@NotNull final List<Mapping> mappings) {
        requireNonNull(mappings, "mappings must not be null");
        if (mappings.size() == 1 && mappings.get(0) == null) {
            this.mappings = Collections.emptyList();
        } else {
            this.mappings = mappings;
        }
    }

    public List<Mapping> getMappings() {
        return mappings;
    }

    @Override
    public String toString() {
        return mappings.toString();
    }

    public IRI unmapFeaturePath(final String path) {
        URI uri = URI.create(path);
        if (HttpUtils.isHttpProtocol(uri.getScheme())) {
            return iri(uri.toString());
        }
        if (!HttpUtils.isFileProtocol(uri.getScheme())) {
            uri = Path.of(path).toAbsolutePath().normalize().toUri();
        }
        final String finalPath = uri.toString();
        final Mapping mapping = mappings.stream().filter(m -> finalPath.startsWith(m.path)).findFirst().orElse(null);
        return mapping != null ? iri(finalPath.replace(mapping.path, mapping.prefix)) : null;
    }

    public URI mapFeatureIri(final IRI featureIri) {
        final String location = featureIri.stringValue();
        final Mapping mapping = mappings.stream().filter(m -> location.startsWith(m.prefix)).findFirst().orElse(null);
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

        public void setPrefix(final String prefix) {
            if (prefix.endsWith("/")) {
                this.prefix = prefix.substring(0, prefix.length() - 1);
            } else {
                this.prefix = prefix;
            }
        }

        public void setPath(final String path) {
            this.path = Path.of(path).toAbsolutePath().normalize().toUri().toString();
            if (this.path.endsWith("/")) {
                this.path = this.path.substring(0, this.path.length() - 1);
            }
        }

        public static Mapping create(final String prefix, final String path) {
            final Mapping mapping = new Mapping();
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
