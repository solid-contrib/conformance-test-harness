package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ConfigOverriddenTest {
    @Inject
    Config config;

    @BeforeAll
    void setup() throws MalformedURLException {
        config.setTestSubject(iri("https://github.com/solid/conformance-test-harness/testserver2"));
        config.setConfigUrl(new URL("https://example.org/config.ttl"));
        config.setTestSuiteDescription(new URL("https://example.org/testsuite.ttl"));
    }

    @Test
    void getTestSubjectChanged() {
        assertEquals("https://github.com/solid/conformance-test-harness/testserver2", config.getTestSubject().stringValue());
    }

    @Test
    void getConfigUrlChanged() throws MalformedURLException {
        assertEquals("https://example.org/config.ttl", config.getConfigUrl().toString());
    }

    @Test
    void getTestSuiteDescriptionChanged() throws MalformedURLException {
        assertEquals("https://example.org/testsuite.ttl", config.getTestSuiteDescription().toString());
    }
}
