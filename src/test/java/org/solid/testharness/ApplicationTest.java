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
package org.solid.testharness;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.solid.testharness.config.Config;
import org.solid.testharness.reporting.TestSuiteResults;
import org.solid.testharness.utils.TestHarnessInitializationException;

import jakarta.inject.Inject;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class ApplicationTest {
    private AutoCloseable closeable;

    @Inject
    Application application;

    @InjectMock
    Config config;
    @InjectMock
    ConformanceTestHarness conformanceTestHarness;

    @Captor
    private ArgumentCaptor<List<String>> captor;

    @BeforeEach
    void setup() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void releaseMocks() throws Exception {
        closeable.close();
    }

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
        verify(config, never()).setOutputDirectory(any());
    }

    @Test
    void outputNotDirectory() throws Exception {
        final Path tmp = Files.createTempFile("test", ".tmp");
        tmp.toFile().deleteOnExit();
        assertEquals(1, application.run("--output", tmp.toString()));
        verify(config, never()).setOutputDirectory(any());
    }

    @Test
    void outputMissing() throws Exception {
        assertEquals(0, application.run("--output", "", "--coverage"));
        final File cwd = Path.of("").toAbsolutePath().toFile();
        verify(config).setOutputDirectory(cwd);
    }

    @Test
    void outputSkipped() throws Exception {
        final TestSuiteResults results = mockResults(1, false);
        when(conformanceTestHarness.runTestSuites(any(), any())).thenReturn(results);
        assertEquals(0, application.run("--skip-reports"));
        verify(config, never()).setOutputDirectory(any());
        verify(conformanceTestHarness, never()).buildReports(any());
    }

    @Test
    void sourceOnce() throws Exception {
        assertEquals(0, application.run("--source", "source1", "--coverage"));
        verify(config).setTestSources(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertTrue(captor.getValue().contains("source1"));
    }

    @Test
    void sourceDouble() throws Exception {
        assertEquals(0, application.run("--source", "source1,source2", "--coverage"));
        verify(config).setTestSources(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertTrue(captor.getValue().contains("source1"));
        assertTrue(captor.getValue().contains("source2"));
    }

    @Test
    void sourceMultiple() throws Exception {
        assertEquals(0, application.run("--source", "source1", "--source", "source2", "--coverage"));
        verify(config).setTestSources(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertTrue(captor.getValue().contains("source1"));
        assertTrue(captor.getValue().contains("source2"));
    }

    @Test
    void sourceBlank() throws Exception {
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
        final TestSuiteResults results = mockResults(1, false);
        when(conformanceTestHarness.runTestSuites(any(), any())).thenReturn(results);
        assertEquals(0, application.run("--target", ""));
        verify(config, never()).setTestSubject(any());
    }

    @Test
    void target() throws Exception {
        final TestSuiteResults results = mockResults(1, false);
        when(conformanceTestHarness.runTestSuites(any(), any())).thenReturn(results);
        when(config.getSubjectsUrl()).thenReturn(URI.create("https://example.org/subjects.ttl").toURL());
        assertEquals(0, application.run("--target", "test"));
        verify(config).setTestSubject(iri("https://example.org/test"));
    }

    @Test
    void targetIri() throws Exception {
        final TestSuiteResults results = mockResults(1, false);
        when(conformanceTestHarness.runTestSuites(any(), any())).thenReturn(results);
        when(config.getSubjectsUrl()).thenReturn(URI.create("https://example.org/subjects.ttl").toURL());
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
        final TestSuiteResults results = mockResults(1, false);
        when(conformanceTestHarness.runTestSuites(any(), any())).thenReturn(results);
        assertEquals(0, application.run("--subjects", "subjectsfile"));
        verify(config).setSubjectsUrl("subjectsfile");
    }

    @Test
    void tolerableBlank() throws Exception {
        assertEquals(1, application.run("--tolerable-failures", ""));
        verify(config).setTolerableFailuresFile("");
    }

    @Test
    void tolerableNotSet() throws Exception {
        assertEquals(0, application.run("--coverage"));
        verify(config, never()).setTolerableFailuresFile(any());
    }

    @Test
    void filtersBlank() throws Exception {
        application.run("--filter", "");
        verify(conformanceTestHarness).runTestSuites(captor.capture(), any());
        assertTrue(captor.getValue().isEmpty());
    }

    @Test
    void filters1() throws Exception {
        application.run("--filter", "filter1");
        verify(conformanceTestHarness).runTestSuites(captor.capture(), any());
        assertEquals(1, captor.getValue().size());
        assertTrue(captor.getValue().contains("filter1"));
    }

    @Test
    void filters2() throws Exception {
        application.run("--filter", "filter1,filter2");
        verify(conformanceTestHarness).runTestSuites(captor.capture(), any());
        assertEquals(2, captor.getValue().size());
        assertTrue(captor.getValue().contains("filter1"));
        assertTrue(captor.getValue().contains("filter2"));
    }

    @Test
    void filtersTwice() throws Exception {
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        application.run("--filter", "filter1", "--filter", "filter2");
        verify(conformanceTestHarness).runTestSuites(captor.capture(), any());
        assertEquals(2, captor.getValue().size());
        assertTrue(captor.getValue().contains("filter1"));
        assertTrue(captor.getValue().contains("filter2"));
    }

    @Test
    void statusesBlank() throws Exception {
        application.run("--status", "");
        verify(conformanceTestHarness).runTestSuites(any(), captor.capture());
        assertTrue(captor.getValue().isEmpty());
    }

    @Test
    void statuses1() throws Exception {
        application.run("--status", "status1");
        verify(conformanceTestHarness).runTestSuites(any(), captor.capture());
        assertEquals(1, captor.getValue().size());
        assertTrue(captor.getValue().contains("status1"));
    }

    @Test
    void statuses2() throws Exception {
        application.run("--status", "status1,status2");
        verify(conformanceTestHarness).runTestSuites(any(), captor.capture());
        assertEquals(2, captor.getValue().size());
        assertTrue(captor.getValue().contains("status1"));
        assertTrue(captor.getValue().contains("status2"));
    }

    @Test
    void statusesTwice() throws Exception {
        application.run("--status", "status1", "--status", "status2");
        verify(conformanceTestHarness).runTestSuites(any(), captor.capture());
        assertEquals(2, captor.getValue().size());
        assertTrue(captor.getValue().contains("status1"));
        assertTrue(captor.getValue().contains("status2"));
    }

    @Test
    void runTestSuitesNoTests() throws Exception {
        when(conformanceTestHarness.runTestSuites(any(), any())).thenReturn(TestSuiteResults.emptyResults());
        assertEquals(0, application.run());
        verify(conformanceTestHarness, never()).cleanUp();
    }

    @Test
    void runTestSuitesNoTearDown() throws Exception {
        final TestSuiteResults results = mockResults(1, false);
        when(conformanceTestHarness.runTestSuites(any(), any())).thenReturn(results);
        assertEquals(0, application.run("--skip-teardown"));
        verify(conformanceTestHarness, never()).cleanUp();
    }

    @Test
    void runTestSuitesWithTearDown() throws Exception {
        final TestSuiteResults results = mockResults(1, false);
        when(conformanceTestHarness.runTestSuites(any(), any())).thenReturn(results);
        assertEquals(0, application.run());
        verify(conformanceTestHarness).cleanUp();
    }

    @Test
    void runTestSuitesFailures() throws Exception {
        final TestSuiteResults results = mockResults(1, true);
        when(conformanceTestHarness.runTestSuites(any(), any())).thenReturn(results);
        assertEquals(1, application.run());
    }

    @Test
    void runTestSuitesFailuresIgnoring() throws Exception {
        final TestSuiteResults results = mockResults(1, true);
        when(conformanceTestHarness.runTestSuites(any(), any())).thenReturn(results);
        assertEquals(0, application.run("--ignore-failures"));
    }

    @Test
    void coverageReportOnly() throws Exception {
        assertEquals(0, application.run("--coverage"));
        verify(conformanceTestHarness).prepareCoverageReport();
    }

    @Test
    void runWithNoOptions() throws Exception {
        final TestSuiteResults results = mockResults(1, false);
        when(conformanceTestHarness.runTestSuites(any(), any())).thenReturn(results);
        assertEquals(0, application.run());
        verify(conformanceTestHarness, never()).prepareCoverageReport();
        verify(conformanceTestHarness).runTestSuites(any(), any());
    }

    @Test
    void runNoException() {
        when(conformanceTestHarness.runTestSuites(any(), any()))
                .thenThrow(new TestHarnessInitializationException("FAIL"));
        assertDoesNotThrow(() -> application.run());
    }

    private TestSuiteResults mockResults(final int features, final boolean failed) {
        final TestSuiteResults results = mock(TestSuiteResults.class);
        when(results.getFeatureTotal()).thenReturn(features);
        when(results.hasFailures()).thenReturn(failed);
        return results;
    }
}
