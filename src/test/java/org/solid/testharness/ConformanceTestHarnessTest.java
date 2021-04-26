package org.solid.testharness;

import com.intuit.karate.Results;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.testharness.config.TargetServer;
import org.solid.testharness.config.TestHarnessConfig;
import org.solid.testharness.discovery.TestSuiteDescription;
import org.solid.testharness.reporting.ReportGenerator;
import org.solid.testharness.reporting.TestSuiteResults;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class ConformanceTestHarnessTest {
    @Inject
    ConformanceTestHarness conformanceTestHarness;

    @InjectMock
    TestHarnessConfig testHarnessConfig;
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
        when(testHarnessConfig.getOutputDirectory()).thenReturn(tmp.toFile());
    }

    @Test
    void initialize() {
        conformanceTestHarness.initialize();
    }

    @Test
    void createCoverageReportNoTests() {
        when(testSuiteDescription.locateTestCases(any(), any())).thenReturn(Collections.emptyList());
        assertTrue(conformanceTestHarness.createCoverageReport());
        assertTrue(Files.notExists(tmp.resolve("coverage.html")));
    }

    @Test
    void createCoverageReportInitError() {
        when(testSuiteDescription.locateTestCases(any(), any())).thenThrow(TestHarnessInitializationException.class);
        assertFalse(conformanceTestHarness.createCoverageReport());
        assertTrue(Files.notExists(tmp.resolve("coverage.html")));
    }

    @Test
    void createCoverageReportWriteFail() {
        tmp.toFile().setWritable(false);
        when(testSuiteDescription.locateTestCases(any(), any())).thenReturn(List.of("feature"));
        assertFalse(conformanceTestHarness.createCoverageReport());
        assertTrue(Files.notExists(tmp.resolve("coverage.html")));
    }

    @Test
    void createCoverageReportPass() {
        when(testSuiteDescription.locateTestCases(any(), any())).thenReturn(List.of("feature"));
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
        when(testSuiteDescription.locateTestCases(any(), any())).thenReturn(Collections.emptyList());
        assertNull(conformanceTestHarness.runTestSuites());
        assertTrue(Files.notExists(tmp.resolve("report.html")));
    }

    @Test
    void runTestSuiteInitErrpr() {
        mockTargetServer();
        when(testSuiteDescription.getSupportedTestCases(any())).thenThrow(TestHarnessInitializationException.class);
        assertNull(conformanceTestHarness.runTestSuites());
        assertTrue(Files.notExists(tmp.resolve("report.html")));
    }

    @Test
    void runTestSuitePass() {
        mockTargetServer();
        when(testSuiteDescription.getSupportedTestCases(any())).thenReturn(Collections.emptyList());
        when(testSuiteDescription.locateTestCases(any(), any())).thenReturn(List.of("test"));
        TestSuiteResults results = mockResults(1);
        when(testRunner.runTests(any(), anyInt())).thenReturn(results);
        assertEquals(1, conformanceTestHarness.runTestSuites().getFailCount());
        assertTrue(Files.exists(tmp.resolve("report.html")));
    }

    @Test
    void runTestSuiteWriteFail() {
        tmp.toFile().setWritable(false);
        mockTargetServer();
        when(testSuiteDescription.getSupportedTestCases(any())).thenReturn(Collections.emptyList());
        when(testSuiteDescription.locateTestCases(any(), any())).thenReturn(List.of("test"));
        TestSuiteResults results = mockResults(1);
        when(testRunner.runTests(any(), anyInt())).thenReturn(results);
        assertEquals(1, conformanceTestHarness.runTestSuites().getFailCount());
        assertTrue(Files.notExists(tmp.resolve("report.html")));
    }

    private void mockTargetServer() {
        TargetServer targetServer = mock(TargetServer.class);
        when(targetServer.getFeatures()).thenReturn(Map.of("feature", false));
        when(targetServer.getMaxThreads()).thenReturn(1);
        when(testHarnessConfig.getTargetServer()).thenReturn(targetServer);
    }

    private TestSuiteResults mockResults(int failures) {
        Results results = mock(Results.class);
        when(results.getFailCount()).thenReturn(failures);
        return new TestSuiteResults(results);
    }
}
