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
