/*
 * MIT License
 *
 * Copyright (c) 2019 - 2022 W3C Solid Community Group
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

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithParentName;
import org.eclipse.microprofile.config.spi.Converter;
import org.eclipse.rdf4j.model.IRI;
import org.solid.testharness.http.HttpUtils;
import org.solid.testharness.utils.TestHarnessInitializationException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@ConfigMapping(prefix = "mappings")
public interface PathMappings {
    @WithParentName
    List<PathMapping> mappings();

    default String stringValue() {
        return mappings().stream().map(PathMapping::stringValue).collect(Collectors.toList()).toString();
    }

    default String unmapFeaturePath(final String path) {
        URI uri = URI.create(path);
        if (HttpUtils.isHttpProtocol(uri.getScheme())) {
            return uri.toString();
        }
        if (!HttpUtils.isFileProtocol(uri.getScheme())) {
            uri = Path.of(path).toAbsolutePath().normalize().toUri();
        }
        final String finalPath = uri.toString();
        final PathMapping mapping = mappings().stream()
                .filter(m -> finalPath.startsWith(m.path())).findFirst().orElse(null);
        return mapping != null ? finalPath.replace(mapping.path(), mapping.prefix()) : null;
    }

    default URI mapIri(final IRI iri) {
        return mapLocation(iri.stringValue());
    }

    default URL mapUrl(final URL url) {
        try {
            return mapLocation(url.toString()).toURL();
        } catch (MalformedURLException e) {
            throw new TestHarnessInitializationException("Bad URL mapping", e);
        }
    }

    private URI mapLocation(final String location) {
        final PathMapping mapping = mappings().stream()
                .filter(m -> location.startsWith(m.prefix())).findFirst().orElse(null);
        return URI.create(mapping != null ? location.replace(mapping.prefix(), mapping.path()) : location);
    }

    interface PathMapping {
        @WithConverter(PrefixConverter.class)
        String prefix();

        @WithConverter(PathConverter.class)
        String path();

        default String stringValue() {
            return String.format("%s => %s", prefix(), path());
        }
    }

    class PathConverter implements Converter<String> {
        private static final long serialVersionUID = 6105901390387650548L;

        @Override
        public String convert(final String value) {
            final String path;
            if (value.matches("^(https?|file):/.*")) {
                path = value;
            } else {
                path = Path.of(value).toAbsolutePath().normalize().toUri().toString();
            }
            return HttpUtils.ensureNoSlashEnd(path);
        }
    }

    class PrefixConverter implements Converter<String> {
        private static final long serialVersionUID = -5363627210557105464L;

        @Override
        public String convert(final String value) {
            return HttpUtils.ensureNoSlashEnd(value);
        }
    }
}
