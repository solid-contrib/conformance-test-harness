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
package org.solid.testharness.config;

import io.quarkus.arc.config.ConfigProperties;
import org.eclipse.rdf4j.model.IRI;
import org.solid.testharness.http.HttpUtils;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.validation.constraints.NotNull;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
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

    public URL mapIri(final IRI iri) {
        return toUrl(iri.stringValue());
    }

    public URL mapUrl(final URL url) {
        return toUrl(url.toString());
    }

    private URL toUrl(final String location) {
        final Mapping mapping = mappings.stream().filter(m -> location.startsWith(m.prefix)).findFirst().orElse(null);
        try {
            return new URL(
                    mapping != null ? location.replace(mapping.prefix, mapping.path) : location);
        } catch (MalformedURLException e) {
            throw (TestHarnessInitializationException) new TestHarnessInitializationException("Bad URL mapping: %s",
                    e.toString()).initCause(e);
        }
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
            if (path.matches("^(https?|file):/.*")) {
                this.path = path;
            } else {
                this.path = Path.of(path).toAbsolutePath().normalize().toUri().toString();
            }
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
