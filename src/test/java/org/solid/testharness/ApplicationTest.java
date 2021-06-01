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
package org.solid.testharness;

import com.intuit.karate.Results;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.testharness.config.Config;
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
import static org.mockito.Mockito.*;

@QuarkusTest
class ApplicationTest {
    @Inject
    Application application;
    @Inject
    Config config;

    @InjectMock
    ConformanceTestHarness conformanceTestHarness;

    @BeforeEach
    void setup() {
        config.setOutputDirectory(null);
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
        assertNull(config.getOutputDirectory());
    }

    @Test
    void outputNotDirectory() throws IOException {
        final Path tmp = Files.createTempFile("test", ".tmp");
        tmp.toFile().deleteOnExit();
        assertEquals(1, application.run("--output", tmp.toString()));
        assertNull(config.getOutputDirectory());
    }

    @Test
    void outputNotWritable() throws IOException {
        final Path tmp = Files.createTempDirectory(null);
        tmp.toFile().setWritable(false);
        tmp.toFile().deleteOnExit();
        assertEquals(1, application.run("--output", tmp.toString()));
        assertNull(config.getOutputDirectory());
    }

    @Test
    void outputNotExecutable() throws IOException {
        final Path tmp = Files.createTempDirectory(null);
        tmp.toFile().setExecutable(false);
        tmp.toFile().deleteOnExit();
        assertEquals(1, application.run("--output", tmp.toString()));
        assertNull(config.getOutputDirectory());
    }

    @Test
    void outputMissing() {
        when(conformanceTestHarness.createCoverageReport()).thenReturn(true);
        assertEquals(0, application.run("--output", "", "--coverage"));
        final File cwd = Path.of("").toAbsolutePath().toFile();
        assertEquals(cwd, config.getOutputDirectory());
    }

    @Test
    void suiteBadUrl() {
        assertEquals(1, application.run("--suite", "http://domain:-100/invalid"));
    }

    @Test
    void suiteFile() throws MalformedURLException {
        when(conformanceTestHarness.createCoverageReport()).thenReturn(true);
        assertEquals(0, application.run("--suite", "suitefile", "--coverage"));
        final URL url = Path.of("suitefile").toAbsolutePath().normalize().toUri().toURL();
        assertEquals(url, config.getTestSuiteDescription());
    }

    @Test
    void suiteUrl() throws MalformedURLException {
        when(conformanceTestHarness.createCoverageReport()).thenReturn(true);
        assertEquals(0, application.run("--suite", "file://test/file", "--coverage"));
        final URL url = new URL("file://test/file");
        assertEquals(url, config.getTestSuiteDescription());
    }

    @Test
    void suiteUrl2() throws MalformedURLException {
        when(conformanceTestHarness.createCoverageReport()).thenReturn(true);
        assertEquals(0, application.run("--suite", "http://example.org", "--coverage"));
        final URL url = new URL("http://example.org");
        assertEquals(url, config.getTestSuiteDescription());
    }

    @Test
    void suiteUrl3() throws MalformedURLException {
        when(conformanceTestHarness.createCoverageReport()).thenReturn(true);
        assertEquals(0, application.run("--suite", "https://example.org", "--coverage"));
        final URL url = new URL("https://example.org");
        assertEquals(url, config.getTestSuiteDescription());
    }

    @Test
    void suiteBlank() {
        assertEquals(1, application.run("--suite", ""));
    }

    @Test
    void targetBlank() {
        final TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites()).thenReturn(results);
        assertEquals(0, application.run("--target", ""));
        assertEquals(iri(Namespaces.TEST_HARNESS_URI, "testserver"), config.getTestSubject());
    }

    @Test
    void target() {
        final TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites()).thenReturn(results);
        assertEquals(0, application.run("--target", "test"));
        assertEquals(iri(Namespaces.TEST_HARNESS_URI, "test"), config.getTestSubject());
    }

    @Test
    void targetIri() {
        final TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites()).thenReturn(results);
        assertEquals(0, application.run("--target", "http://example.org/test"));
        assertEquals(iri("http://example.org/test"), config.getTestSubject());
    }

    @Test
    void configBlank() {
        assertEquals(1, application.run("--config", ""));
    }

    @Test
    void configFile() throws MalformedURLException {
        final TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites()).thenReturn(results);
        assertEquals(0, application.run("--config", "configfile"));
        final URL url = Path.of("configfile").toAbsolutePath().normalize().toUri().toURL();
        assertEquals(url, config.getConfigUrl());
    }

    @Test
    void configUrl() throws MalformedURLException {
        final TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites()).thenReturn(results);
        assertEquals(0, application.run("--config", "file://test/file"));
        final URL url = new URL("file://test/file");
        assertEquals(url, config.getConfigUrl());
    }

    @Test
    void configUrl2() throws MalformedURLException {
        final TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites()).thenReturn(results);
        assertEquals(0, application.run("--config", "http://example.org"));
        final URL url = new URL("http://example.org");
        assertEquals(url, config.getConfigUrl());
    }

    @Test
    void configUrl3() throws MalformedURLException {
        final TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites()).thenReturn(results);
        assertEquals(0, application.run("--config", "https://example.org"));
        final URL url = new URL("https://example.org");
        assertEquals(url, config.getConfigUrl());
    }

    @Test
    void runTestSuitesNoResults() {
        when(conformanceTestHarness.runTestSuites()).thenReturn(null);
        assertEquals(1, application.run());
    }

    @Test
    void runTestSuitesFailures() {
        final TestSuiteResults results = mockResults(1);
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

    private TestSuiteResults mockResults(final int failures) {
        final Results results = mock(Results.class);
        when(results.getFailCount()).thenReturn(failures);
        return new TestSuiteResults(results);
    }
}
