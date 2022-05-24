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
package org.solid.testharness.reporting;

import io.quarkus.qute.TemplateLocator;
import io.quarkus.qute.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class TestTemplateLocator implements TemplateLocator {
    private static final Logger logger = LoggerFactory.getLogger(TestTemplateLocator.class);

    @Override
    public Optional<TemplateLocation> locate(final String s) {
        if (!s.contains("src/test")) {
            return Optional.empty();
        }
        try {
            final Path found = Paths.get(s);
            logger.debug("Find template {}", found.toAbsolutePath());
            final byte[] content = Files.readAllBytes(found);
            return Optional.of(new TemplateLocation() {
                public Reader read() {
                    return new StringReader(new String(content, StandardCharsets.UTF_8));
                }
                public Optional<Variant> getVariant() {
                    return Optional.empty();
                }
            });
        } catch (IOException var4) {
            logger.warn("Unable to read the template from path: " + s, var4);
            return Optional.empty();
        }
    }

}
