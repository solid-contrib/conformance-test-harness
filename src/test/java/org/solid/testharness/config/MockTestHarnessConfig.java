package org.solid.testharness.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import java.io.File;
import java.io.IOException;

@Alternative
@ApplicationScoped
public class MockTestHarnessConfig extends TestHarnessConfig {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.config.MockTestHarnessConfig");

    @PostConstruct
    public void initialize() throws IOException {
        super.credentialsDirectory = new File("src/test/resources").getCanonicalFile();
        logger.debug("Mock initialization");
    }
}
