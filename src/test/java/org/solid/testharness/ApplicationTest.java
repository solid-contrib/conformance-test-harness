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

import javax.inject.Inject;
import java.io.File;
import java.net.URL;
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
    void badCommand() throws Exception {
        assertEquals(1, application.run("--bad"));
    }

    @Test
    void help() throws Exception {
        assertEquals(0, application.run("--help"));
    }

    @Test
    void reportCLash() throws Exception {
        assertEquals(1, application.run("--skip-reports", "--coverage"));
        verify(conformanceTestHarness, never()).prepareCoverageReport();
    }

    @Test
    void outputBad() throws Exception {
        assertEquals(1, application.run("--output", "./missing"));
        assertNull(config.getOutputDirectory());
    }

    @Test
    void outputNotDirectory() throws Exception {
        final Path tmp = Files.createTempFile("test", ".tmp");
        tmp.toFile().deleteOnExit();
        assertEquals(1, application.run("--output", tmp.toString()));
        assertNull(config.getOutputDirectory());
    }

    @Test
    void outputNotWritable() throws Exception {
        final Path tmp = Files.createTempDirectory(null);
        tmp.toFile().setWritable(false);
        tmp.toFile().deleteOnExit();
        assertEquals(1, application.run("--output", tmp.toString()));
        assertNull(config.getOutputDirectory());
    }

    @Test
    void outputNotExecutable() throws Exception {
        final Path tmp = Files.createTempDirectory(null);
        tmp.toFile().setExecutable(false);
        tmp.toFile().deleteOnExit();
        assertEquals(1, application.run("--output", tmp.toString()));
        assertNull(config.getOutputDirectory());
    }

    @Test
    void outputMissing() throws Exception {
        assertEquals(0, application.run("--output", "", "--coverage"));
        final File cwd = Path.of("").toAbsolutePath().toFile();
        verify(config).setOutputDirectory(cwd);
    }

    @Test
    void outputSkipped() throws Exception {
        final TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites(any())).thenReturn(results);
        assertEquals(0, application.run("--skip-reports"));
        verify(config, never()).setOutputDirectory(any());
        verify(conformanceTestHarness, never()).buildReports(any());
    }

    @Test
    void sourceOnce() throws Exception {
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        assertEquals(0, application.run("--source", "source1", "--coverage"));
        verify(config).setTestSources(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertTrue(captor.getValue().contains("source1"));
    }

    @Test
    void sourceDouble() throws Exception {
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        assertEquals(0, application.run("--source", "source1,source2", "--coverage"));
        verify(config).setTestSources(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertTrue(captor.getValue().contains("source1"));
        assertTrue(captor.getValue().contains("source2"));
    }

    @Test
    void sourceMultiple() throws Exception {
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        assertEquals(0, application.run("--source", "source1", "--source", "source2", "--coverage"));
        verify(config).setTestSources(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertTrue(captor.getValue().contains("source1"));
        assertTrue(captor.getValue().contains("source2"));
    }

    @Test
    void sourceBlank() throws Exception {
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        assertEquals(0, application.run("--source", "", "--coverage"));
        verify(config).setTestSources(captor.capture());
        assertEquals(0, captor.getValue().size());
    }

    @Test
    void sourceNotSet() throws Exception {
        assertEquals(0, application.run("--coverage"));
        verify(config, never()).setTestSources(any());
    }

    @Test
    void targetBlank() throws Exception {
        final TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites(any())).thenReturn(results);
        assertEquals(0, application.run("--target", ""));
        verify(config, never()).setTestSubject(any());
    }

    @Test
    void target() throws Exception {
        final TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites(any())).thenReturn(results);
        when(config.getSubjectsUrl()).thenReturn(new URL("https://example.org/subjects.ttl"));
        assertEquals(0, application.run("--target", "test"));
        verify(config).setTestSubject(iri("https://example.org/test"));
    }

    @Test
    void targetIri() throws Exception {
        final TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites(any())).thenReturn(results);
        when(config.getSubjectsUrl()).thenReturn(new URL("https://example.org/subjects.ttl"));
        assertEquals(0, application.run("--target", "https://example.org/test"));
        verify(config).setTestSubject(iri("https://example.org/test"));
    }

    @Test
    void subjectsBlank() throws Exception {
        assertEquals(1, application.run("--subjects", ""));
        verify(config).setSubjectsUrl("");
    }

    @Test
    void subjectsNotSet() throws Exception {
        assertEquals(0, application.run("--coverage"));
        verify(config, never()).setSubjectsUrl(any());
    }

    @Test
    void subjectsSet() throws Exception {
        final TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites(any())).thenReturn(results);
        assertEquals(0, application.run("--subjects", "subjectsfile"));
        verify(config).setSubjectsUrl("subjectsfile");
    }

    @Test
    void filtersBlank() throws Exception {
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        application.run("--filter", "");
        verify(conformanceTestHarness).runTestSuites(captor.capture());
        assertTrue(captor.getValue().isEmpty());
    }

    @Test
    void filters1() throws Exception {
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        application.run("--filter", "filter1");
        verify(conformanceTestHarness).runTestSuites(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertTrue(captor.getValue().contains("filter1"));
    }

    @Test
    void filters2() throws Exception {
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        application.run("--filter", "filter1,filter2");
        verify(conformanceTestHarness).runTestSuites(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertTrue(captor.getValue().contains("filter1"));
        assertTrue(captor.getValue().contains("filter2"));
    }

    @Test
    void filtersTwice() throws Exception {
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        application.run("--filter", "filter1", "--filter", "filter2");
        verify(conformanceTestHarness).runTestSuites(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertTrue(captor.getValue().contains("filter1"));
        assertTrue(captor.getValue().contains("filter2"));
    }

    @Test
    void runTestSuitesNoResults() throws Exception {
        when(conformanceTestHarness.runTestSuites(any())).thenReturn(null);
        assertEquals(1, application.run());
        verify(conformanceTestHarness).cleanUp();
    }

    @Test
    void runTestSuitesNoTearDown() throws Exception {
        when(conformanceTestHarness.runTestSuites(any())).thenReturn(null);
        assertEquals(1, application.run("--skip-teardown"));
        verify(conformanceTestHarness, never()).cleanUp();
    }

    @Test
    void runTestSuitesFailures() throws Exception {
        final TestSuiteResults results = mockResults(1);
        when(conformanceTestHarness.runTestSuites(any())).thenReturn(results);
        assertEquals(1, application.run());
    }

    @Test
    void runTestSuitesFailuresIgnoring() throws Exception {
        final TestSuiteResults results = mockResults(1);
        when(conformanceTestHarness.runTestSuites(any())).thenReturn(results);
        assertEquals(0, application.run("--ignore-failures"));
    }

    @Test
    void coverageReportOnly() throws Exception {
        assertEquals(0, application.run("--coverage"));
        verify(conformanceTestHarness).prepareCoverageReport();
    }

    @Test
    void runWithNoOptions() throws Exception {
        final TestSuiteResults results = mockResults(0);
        when(conformanceTestHarness.runTestSuites(any())).thenReturn(results);
        assertEquals(0, application.run());
        verify(conformanceTestHarness, never()).prepareCoverageReport();
        verify(conformanceTestHarness).runTestSuites(any());
    }

    private TestSuiteResults mockResults(final int failures) {
        final Results results = mock(Results.class);
        when(results.getFailCount()).thenReturn(failures);
        return new TestSuiteResults(results);
    }
}
