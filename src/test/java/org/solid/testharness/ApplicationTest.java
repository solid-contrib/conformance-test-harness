package org.solid.testharness;

import com.intuit.karate.Results;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.testharness.config.TestHarnessConfig;
import org.solid.testharness.reporting.TestSuiteResults;
import org.solid.testharness.utils.Namespaces;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class ApplicationTest {
    @Inject
    Application application;
    @Inject
    TestHarnessConfig testHarnessConfig;

    @InjectMock
    ConformanceTestHarness conformanceTestHarness;

    @BeforeEach
    void setup() {
        testHarnessConfig.setOutputDirectory(null);
    }

    @Test
    void badCommand() {
        assertEquals(1, application.run("--bad"));
    }

    @Test
    void help() {
        assertEquals(1, application.run("--help"));
    }

    @Test
    void outputBad() {
        assertEquals(1, application.run("--output", "./missing"));
        assertNull(testHarnessConfig.getOutputDirectory());
    }

    @Test
    void outputNotDirectory() throws IOException {
        Path tmp = Files.createTempFile("test", ".tmp");
        tmp.toFile().deleteOnExit();
        assertEquals(1, application.run("--output", tmp.toString()));
        assertNull(testHarnessConfig.getOutputDirectory());
    }

    @Test
    void outputNotWritable() throws IOException {
        Path tmp = Files.createTempDirectory(null);
        tmp.toFile().setWritable(false);
        tmp.toFile().deleteOnExit();
        assertEquals(1, application.run("--output", tmp.toString()));
        assertNull(testHarnessConfig.getOutputDirectory());
    }

    @Test
    void outputNotExecutable() throws IOException {
        Path tmp = Files.createTempDirectory(null);
        tmp.toFile().setExecutable(false);
        tmp.toFile().deleteOnExit();
        assertEquals(1, application.run("--output", tmp.toString()));
        assertNull(testHarnessConfig.getOutputDirectory());
    }

    @Test
    void outputMissing() {
        when(conformanceTestHarness.createCoverageReport()).thenReturn(true);
        assertEquals(0, application.run("--output", "", "--coverage"));
        File cwd = Path.of("").toAbsolutePath().toFile();
        assertEquals(cwd, testHarnessConfig.getOutputDirectory());
    }

    @Test
    void suiteBadUrl() {
        assertEquals(1, application.run("--suite", "http://domain:-100/invalid"));
    }

    @Test
    void suiteFile() throws MalformedURLException {
        when(conformanceTestHarness.createCoverageReport()).thenReturn(true);
        assertEquals(0, application.run("--suite", "suitefile", "--coverage"));
        URL url = Path.of("suitefile").toAbsolutePath().normalize().toUri().toURL();
        assertEquals(url, testHarnessConfig.getTestSuiteDescription());
    }

    @Test
    void suiteUrl() throws MalformedURLException {
        when(conformanceTestHarness.createCoverageReport()).thenReturn(true);
        assertEquals(0, application.run("--suite", "file://test/file", "--coverage"));
        URL url = new URL("file://test/file");
        assertEquals(url, testHarnessConfig.getTestSuiteDescription());
    }

    @Test
    void suiteUrl2() throws MalformedURLException {
        when(conformanceTestHarness.createCoverageReport()).thenReturn(true);
        assertEquals(0, application.run("--suite", "http://example.org", "--coverage"));
        URL url = new URL("http://example.org");
        assertEquals(url, testHarnessConfig.getTestSuiteDescription());
    }

    @Test
    void suiteUrl3() throws MalformedURLException {
        when(conformanceTestHarness.createCoverageReport()).thenReturn(true);
        assertEquals(0, application.run("--suite", "https://example.org", "--coverage"));
        URL url = new URL("https://example.org");
        assertEquals(url, testHarnessConfig.getTestSuiteDescription());
    }

    @Test
    void suiteBlank() {
        assertEquals(1, application.run("--suite", ""));
    }

    @Test
    void targetBlank() {
        TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites()).thenReturn(results);
        assertEquals(0, application.run("--target", ""));
        assertEquals(iri(Namespaces.TEST_HARNESS_URI, "testserver"), testHarnessConfig.getTestSubject());
    }

    @Test
    void target() {
        TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites()).thenReturn(results);
        assertEquals(0, application.run("--target", "test"));
        assertEquals(iri(Namespaces.TEST_HARNESS_URI, "test"), testHarnessConfig.getTestSubject());
    }

    @Test
    void targetIri() {
        TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites()).thenReturn(results);
        assertEquals(0, application.run("--target", "http://example.org/test"));
        assertEquals(iri("http://example.org/test"), testHarnessConfig.getTestSubject());
    }

    @Test
    void configBlank() {
        assertEquals(1, application.run("--config", ""));
    }

    @Test
    void configFile() throws MalformedURLException {
        TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites()).thenReturn(results);
        assertEquals(0, application.run("--config", "configfile"));
        URL url = Path.of("configfile").toAbsolutePath().normalize().toUri().toURL();
        assertEquals(url, testHarnessConfig.getConfigUrl());
    }

    @Test
    void configUrl() throws MalformedURLException {
        TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites()).thenReturn(results);
        assertEquals(0, application.run("--config", "file://test/file"));
        URL url = new URL("file://test/file");
        assertEquals(url, testHarnessConfig.getConfigUrl());
    }

    @Test
    void configUrl2() throws MalformedURLException {
        TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites()).thenReturn(results);
        assertEquals(0, application.run("--config", "http://example.org"));
        URL url = new URL("http://example.org");
        assertEquals(url, testHarnessConfig.getConfigUrl());
    }

    @Test
    void configUrl3() throws MalformedURLException {
        TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites()).thenReturn(results);
        assertEquals(0, application.run("--config", "https://example.org"));
        URL url = new URL("https://example.org");
        assertEquals(url, testHarnessConfig.getConfigUrl());
    }

    @Test
    void runTestSuitesNoResults() {
        when(conformanceTestHarness.runTestSuites()).thenReturn(null);
        assertEquals(1, application.run());
    }

    @Test
    void runTestSuitesFailures() {
        TestSuiteResults results = mockResults(1);
        when(conformanceTestHarness.runTestSuites()).thenReturn(results);
        assertEquals(1, application.run());
    }

    @Test
    void coverageReport() {
        when(conformanceTestHarness.createCoverageReport()).thenReturn(true);
        assertEquals(0, application.run("--coverage"));
    }

    @Test
    void coverageReportBadData() {
        when(conformanceTestHarness.createCoverageReport()).thenReturn(false);
        assertEquals(1, application.run("--coverage"));
    }

    private TestSuiteResults mockResults(int failures) {
        Results results = mock(Results.class);
        when(results.getFailCount()).thenReturn(failures);
        return new TestSuiteResults(results);
    }
}
