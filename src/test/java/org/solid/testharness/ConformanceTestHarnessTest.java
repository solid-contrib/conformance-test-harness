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
import org.mockito.ArgumentCaptor;
import org.solid.testharness.config.Config;
import org.solid.testharness.config.TargetServer;
import org.solid.testharness.config.TestSubject;
import org.solid.testharness.discovery.TestSuiteDescription;
import org.solid.testharness.reporting.ReportGenerator;
import org.solid.testharness.reporting.TestSuiteResults;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class ConformanceTestHarnessTest {
    @Inject
    ConformanceTestHarness conformanceTestHarness;
    @Inject
    DataRepository dataRepository;

    @InjectMock
    Config config;
    @InjectMock
    TestSubject testSubject;
    @InjectMock
    TestSuiteDescription testSuiteDescription;
    @InjectMock
    ReportGenerator reportGenerator;
    @InjectMock
    TestRunner testRunner;

    Path tmp;

    @BeforeEach
    void setup() throws IOException {
        tmp = Files.createTempDirectory(null);
        tmp.toFile().deleteOnExit();
        when(config.getOutputDirectory()).thenReturn(tmp.toFile());
    }

    @Test
    void initialize() throws Exception {
        conformanceTestHarness.initialize();
        final StringWriter sw = new StringWriter();
        dataRepository.export(sw);
        final String result = sw.toString();
        assertTrue(result.contains("a earl:Software"));
        assertTrue(result.contains("doap:name"));
        assertTrue(result.contains("doap:description"));
        assertTrue(result.contains("doap:created"));
        assertTrue(result.contains("doap:developer"));
        assertTrue(result.contains("doap:homepage"));
        assertTrue(result.contains("doap:revision"));
    }

    @Test
    void createCoverageReportNoTests() {
        when(testSuiteDescription.locateTestCases(any())).thenReturn(Collections.emptyList());
        assertTrue(conformanceTestHarness.createCoverageReport());
        assertTrue(Files.notExists(tmp.resolve("coverage.html")));
    }

    @Test
    void createCoverageReportInitError() {
        when(testSuiteDescription.locateTestCases(any())).thenThrow(TestHarnessInitializationException.class);
        assertFalse(conformanceTestHarness.createCoverageReport());
        assertTrue(Files.notExists(tmp.resolve("coverage.html")));
    }

    @Test
    void createCoverageReportWriteFail() {
        tmp.toFile().setWritable(false);
        when(testSuiteDescription.locateTestCases(any())).thenReturn(List.of("feature"));
        assertFalse(conformanceTestHarness.createCoverageReport());
        assertTrue(Files.notExists(tmp.resolve("coverage.html")));
    }

    @Test
    void createCoverageReportPass() {
        when(testSuiteDescription.locateTestCases(any())).thenReturn(List.of("feature"));
        assertTrue(conformanceTestHarness.createCoverageReport());
        assertTrue(Files.exists(tmp.resolve("coverage.html")));
    }

    @Test
    void createCoverageReportFalse() {
        assertTrue(conformanceTestHarness.createCoverageReport());
    }

    @Test
    void runTestSuiteNoTests() {
        mockTargetServer();
        when(testSuiteDescription.getSupportedTestCases(any())).thenReturn(Collections.emptyList());
        when(testSuiteDescription.locateTestCases(any())).thenReturn(Collections.emptyList());
        assertNull(conformanceTestHarness.runTestSuites());
        assertTrue(Files.notExists(tmp.resolve("report.html")));
    }

    @Test
    void runTestSuiteNoTestsNullFilter() {
        mockTargetServer();
        when(testSuiteDescription.getSupportedTestCases(any())).thenReturn(List.of(iri("file:/group1/tests")));
        when(testSuiteDescription.locateTestCases(any())).thenReturn(Collections.emptyList());
        assertNull(conformanceTestHarness.runTestSuites(null));
        assertTrue(Files.notExists(tmp.resolve("report.html")));
    }

    @Test
    void runTestSuiteNoTestsEmptyFilter() {
        mockTargetServer();
        when(testSuiteDescription.getSupportedTestCases(any())).thenReturn(List.of(iri("file:/group1/tests")));
        when(testSuiteDescription.locateTestCases(any())).thenReturn(Collections.emptyList());
        assertNull(conformanceTestHarness.runTestSuites(Collections.emptyList()));
        assertTrue(Files.notExists(tmp.resolve("report.html")));
    }

    @Test
    void runTestSuiteWithRegistration() {
        mockTargetServer();
        when(config.getUserRegistrationEndpoint()).thenReturn(URI.create("https://example.org/register"));
        when(testSuiteDescription.getSupportedTestCases(any())).thenReturn(Collections.emptyList());
        when(testSuiteDescription.locateTestCases(any())).thenReturn(List.of("test"));

        final TestSuiteResults results = mockResults(1);
        when(testRunner.runTests(any(), anyInt())).thenReturn(results);
        assertEquals(1, conformanceTestHarness.runTestSuites().getFailCount());
        assertTrue(Files.exists(tmp.resolve("report.html")));
        verify(testSubject).registerUsers();
    }

    @Test
    void runTestSuiteInitError() {
        mockTargetServer();
        when(testSuiteDescription.getSupportedTestCases(any())).thenThrow(TestHarnessInitializationException.class);
        assertNull(conformanceTestHarness.runTestSuites());
        assertTrue(Files.notExists(tmp.resolve("report.html")));
    }

    @Test
    void runTestSuitePass() {
        mockTargetServer();
        when(testSuiteDescription.getSupportedTestCases(any())).thenReturn(Collections.emptyList());
        when(testSuiteDescription.locateTestCases(any())).thenReturn(List.of("test"));
        final TestSuiteResults results = mockResults(1);
        when(testRunner.runTests(any(), anyInt())).thenReturn(results);
        assertEquals(1, conformanceTestHarness.runTestSuites().getFailCount());
        assertTrue(Files.exists(tmp.resolve("report.html")));
        verify(testSubject).tearDownServer();
    }

    @Test
    void runTestSuiteNoTearDown() {
        mockTargetServer();
        when(config.isSkipTearDown()).thenReturn(true);
        when(testSuiteDescription.getSupportedTestCases(any())).thenReturn(Collections.emptyList());
        when(testSuiteDescription.locateTestCases(any())).thenReturn(List.of("test"));
        final TestSuiteResults results = mockResults(1);
        when(testRunner.runTests(any(), anyInt())).thenReturn(results);
        assertEquals(1, conformanceTestHarness.runTestSuites().getFailCount());
        assertTrue(Files.exists(tmp.resolve("report.html")));
        verify(testSubject, never()).tearDownServer();
    }

    @Test
    void runTestSuiteOneFilter() {
        mockTargetServer();
        when(testSuiteDescription.getSupportedTestCases(any()))
                .thenReturn(List.of(iri("file:/group1/tests"), iri("file:/group2/tests")));
        when(testSuiteDescription.locateTestCases(any())).thenReturn(List.of("test"));
        final TestSuiteResults results = mockResults(1);
        when(testRunner.runTests(any(), anyInt())).thenReturn(results);

        assertEquals(1, conformanceTestHarness.runTestSuites(List.of("group1")).getFailCount());
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(testSuiteDescription).locateTestCases(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(iri("file:/group1/tests"), captor.getValue().get(0));
        assertTrue(Files.exists(tmp.resolve("report.html")));
    }

    @Test
    void runTestSuiteTwoFilters() {
        mockTargetServer();
        when(testSuiteDescription.getSupportedTestCases(any()))
                .thenReturn(List.of(iri("file:/group1/tests"), iri("file:/group2/tests"), iri("file:/example/group3")));
        when(testSuiteDescription.locateTestCases(any())).thenReturn(List.of("test"));
        final TestSuiteResults results = mockResults(1);
        when(testRunner.runTests(any(), anyInt())).thenReturn(results);

        assertEquals(1, conformanceTestHarness.runTestSuites(List.of("group1", "group2")).getFailCount());
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(testSuiteDescription).locateTestCases(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertEquals(iri("file:/group1/tests"), captor.getValue().get(0));
        assertEquals(iri("file:/group2/tests"), captor.getValue().get(1));
        assertTrue(Files.exists(tmp.resolve("report.html")));
    }

    @Test
    void runTestSuiteFilterExcludeAll() {
        mockTargetServer();
        when(testSuiteDescription.getSupportedTestCases(any()))
                .thenReturn(List.of(iri("file:/group1/tests"), iri("file:/group2/tests")));
        when(testSuiteDescription.locateTestCases(any())).thenReturn(List.of("test"));
        final TestSuiteResults results = mockResults(1);
        when(testRunner.runTests(any(), anyInt())).thenReturn(results);

        assertEquals(1, conformanceTestHarness.runTestSuites(List.of("missing")).getFailCount());
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(testSuiteDescription).locateTestCases(captor.capture());
        assertTrue(captor.getValue().isEmpty());
        assertTrue(Files.exists(tmp.resolve("report.html")));
    }

    @Test
    void runTestSuiteWriteFail() {
        tmp.toFile().setWritable(false);
        mockTargetServer();
        when(testSuiteDescription.getSupportedTestCases(any())).thenReturn(Collections.emptyList());
        when(testSuiteDescription.locateTestCases(any())).thenReturn(List.of("test"));
        final TestSuiteResults results = mockResults(1);
        when(testRunner.runTests(any(), anyInt())).thenReturn(results);
        assertEquals(1, conformanceTestHarness.runTestSuites().getFailCount());
        assertTrue(Files.notExists(tmp.resolve("report.html")));
    }

    private void mockTargetServer() {
        final TargetServer targetServer = mock(TargetServer.class);
        when(targetServer.getFeatures()).thenReturn(Map.of("feature", false));
        when(testSubject.getTargetServer()).thenReturn(targetServer);
    }

    private TestSuiteResults mockResults(final int failures) {
        final Results results = mock(Results.class);
        when(results.getFeaturesPassed()).thenReturn(10);
        when(results.getFeaturesFailed()).thenReturn(0);
        when(results.toKarateJson()).thenReturn(Map.of("featuresSkipped", 0));
        when(results.getScenariosPassed()).thenReturn(20);
        when(results.getScenariosFailed()).thenReturn(failures);
        when(results.getElapsedTime()).thenReturn(1000d);
        when(results.getTimeTakenMillis()).thenReturn(1000d);
        when(results.getFailCount()).thenReturn(failures);
        return new TestSuiteResults(results);
    }
}
