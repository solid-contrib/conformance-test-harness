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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.solid.testharness.config.Config;
import org.solid.testharness.reporting.TestSuiteResults;
import org.solid.testharness.utils.Namespaces;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class ApplicationTest {
    @Inject
    Application application;

    @InjectMock
    Config config;
    @InjectMock
    ConformanceTestHarness conformanceTestHarness;

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
        verify(config).setOutputDirectory(cwd);
    }

    @Test
    void sourceOnce() {
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        when(conformanceTestHarness.createCoverageReport()).thenReturn(true);
        assertEquals(0, application.run("--source", "source1", "--coverage"));
        verify(config).setTestSources(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertTrue(captor.getValue().contains("source1"));
    }

    @Test
    void sourceDouble() {
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        when(conformanceTestHarness.createCoverageReport()).thenReturn(true);
        assertEquals(0, application.run("--source", "source1,source2", "--coverage"));
        verify(config).setTestSources(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertTrue(captor.getValue().contains("source1"));
        assertTrue(captor.getValue().contains("source2"));
    }

    @Test
    void sourceMultiple() {
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        when(conformanceTestHarness.createCoverageReport()).thenReturn(true);
        assertEquals(0, application.run("--source", "source1", "--source", "source2", "--coverage"));
        verify(config).setTestSources(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertTrue(captor.getValue().contains("source1"));
        assertTrue(captor.getValue().contains("source2"));
    }

    @Test
    void sourceBlank() {
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        when(conformanceTestHarness.createCoverageReport()).thenReturn(false);
        assertEquals(1, application.run("--source", "", "--coverage"));
        verify(config).setTestSources(captor.capture());
        assertEquals(0, captor.getValue().size());
    }

    @Test
    void sourceNotSet() {
        when(conformanceTestHarness.createCoverageReport()).thenReturn(false);
        assertEquals(1, application.run("--coverage"));
        verify(config, never()).setTestSources(any());
    }

    @Test
    void targetBlank() {
        final TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.createCoverageReport()).thenReturn(true);
        when(conformanceTestHarness.runTestSuites(any())).thenReturn(results);
        assertEquals(0, application.run("--target", ""));
        verify(config, never()).setTestSubject(any());
    }

    @Test
    void target() {
        final TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites(any())).thenReturn(results);
        assertEquals(0, application.run("--tests", "--target", "test"));
        verify(config).setTestSubject(iri(Namespaces.TEST_HARNESS_URI, "test"));
    }

    @Test
    void targetIri() {
        final TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites(any())).thenReturn(results);
        assertEquals(0, application.run("--tests", "--target", "http://example.org/test"));
        verify(config).setTestSubject(iri("http://example.org/test"));
    }

    @Test
    void subjectsBlank() {
        assertEquals(1, application.run("--subjects", ""));
        verify(config).setSubjectsUrl("");
    }

    @Test
    void subjectsNotSet() {
        when(conformanceTestHarness.createCoverageReport()).thenReturn(false);
        assertEquals(1, application.run("--coverage"));
        verify(config, never()).setSubjectsUrl(any());
    }

    @Test
    void subjectsSet() {
        final TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites(any())).thenReturn(results);
        assertEquals(0, application.run("--tests", "--subjects", "subjectsfile"));
        verify(config).setSubjectsUrl("subjectsfile");
    }

    @Test
    void filtersBlank() {
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        application.run("--tests", "--filter", "");
        verify(conformanceTestHarness).runTestSuites(captor.capture());
        assertTrue(captor.getValue().isEmpty());
    }

    @Test
    void filters1() {
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        application.run("--tests", "--filter", "filter1");
        verify(conformanceTestHarness).runTestSuites(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertTrue(captor.getValue().contains("filter1"));
    }

    @Test
    void filters2() {
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        application.run("--tests", "--filter", "filter1,filter2");
        verify(conformanceTestHarness).runTestSuites(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertTrue(captor.getValue().contains("filter1"));
        assertTrue(captor.getValue().contains("filter2"));
    }

    @Test
    void filtersTwice() {
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        application.run("--tests", "--filter", "filter1", "--filter", "filter2");
        verify(conformanceTestHarness).runTestSuites(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertTrue(captor.getValue().contains("filter1"));
        assertTrue(captor.getValue().contains("filter2"));
    }

    @Test
    void runTestSuitesNoResults() {
        when(conformanceTestHarness.runTestSuites(any())).thenReturn(null);
        assertEquals(1, application.run("--tests"));
    }

    @Test
    void runTestSuitesFailures() {
        final TestSuiteResults results = mockResults(1);
        when(conformanceTestHarness.runTestSuites(any())).thenReturn(results);
        assertEquals(1, application.run("--tests"));
    }

    @Test
    void coverageReportOnly() {
        when(conformanceTestHarness.createCoverageReport()).thenReturn(true);
        assertEquals(0, application.run("--coverage"));
    }

    @Test
    void coverageAndTestReportNoOptions() {
        when(conformanceTestHarness.createCoverageReport()).thenReturn(true);
        final TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites(any())).thenReturn(results);
        assertEquals(0, application.run());
        verify(conformanceTestHarness).createCoverageReport();
        verify(conformanceTestHarness).runTestSuites(any());
    }

    @Test
    void coverageAndTestReportBothOptions() {
        when(conformanceTestHarness.createCoverageReport()).thenReturn(true);
        final TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites(any())).thenReturn(results);
        assertEquals(0, application.run("--coverage", "--tests"));
        verify(conformanceTestHarness).createCoverageReport();
        verify(conformanceTestHarness).runTestSuites(any());
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
