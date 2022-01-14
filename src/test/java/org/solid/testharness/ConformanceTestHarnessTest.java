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
import com.intuit.karate.core.Feature;
import com.intuit.karate.core.Tag;
import com.intuit.karate.resource.Resource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.testharness.config.Config;
import org.solid.testharness.config.TargetServer;
import org.solid.testharness.config.TestSubject;
import org.solid.testharness.discovery.TestSuiteDescription;
import org.solid.testharness.http.AuthManager;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.reporting.ReportGenerator;
import org.solid.testharness.reporting.TestSuiteResults;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.Namespaces;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class ConformanceTestHarnessTest {
    @Inject
    ConformanceTestHarness conformanceTestHarness;

    @InjectMock
    DataRepository dataRepository;
    @InjectMock
    Config config;
    @InjectMock
    TestSubject testSubject;
    @InjectMock
    TestSuiteDescription testSuiteDescription;
    @InjectMock
    ReportGenerator reportGenerator; // this appears to be unused but is required for buildReports to run correctly
    @InjectMock
    TestRunner testRunner;
    @InjectMock
    AuthManager authManager;

    Path tmp;

    @BeforeEach
    void setup() throws IOException {
        tmp = Files.createTempDirectory(null);
        tmp.toFile().deleteOnExit();
        when(config.getOutputDirectory()).thenReturn(tmp.toFile());
    }

    @Test
    void initialize() throws Exception {
        final Repository repository = new SailRepository(new MemoryStore());
        Namespaces.addToRepository(repository);
        when(dataRepository.getConnection()).thenReturn(repository.getConnection());
        conformanceTestHarness.initialize();
        verify(dataRepository).setAssertor(any());
        final StringWriter wr = new StringWriter();
        final RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, wr);
        try (RepositoryConnection conn = repository.getConnection()) {
            rdfWriter.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, true)
                    .set(BasicWriterSettings.INLINE_BLANK_NODES, true);
            conn.export(rdfWriter);
        }
        final String result = wr.toString();
        assertTrue(result.contains("a earl:Software"));
        assertTrue(result.contains("doap:name"));
        assertTrue(result.contains("doap:description"));
        assertTrue(result.contains("doap:created"));
        assertTrue(result.contains("doap:developer"));
        assertTrue(result.contains("doap:homepage"));
        assertTrue(result.contains("doap:revision"));
    }

    @Test
    void prepareCoverageReport() {
        assertDoesNotThrow(() -> conformanceTestHarness.prepareCoverageReport());
    }

    @Test
    void prepareCoverageReportInitError() {
        doThrow(new TestHarnessInitializationException("FAIL"))
                .when(testSuiteDescription).prepareTestCases(Config.RunMode.COVERAGE);
        assertThrows(TestHarnessInitializationException.class, () -> conformanceTestHarness.prepareCoverageReport());
    }

    @Test
    void runTestSuiteNoTestsNullFeaturePaths() {
        mockTargetServer();
        when(testSuiteDescription.getFeaturePaths()).thenReturn(null);
        final TestSuiteResults results = conformanceTestHarness.runTestSuites(null, null);
        assertNotNull(results);
        assertEquals(0, results.getFeatureTotal());
    }

    @Test
    void runTestSuiteNoTestsNullFilters() {
        mockTargetServer();
        when(testSuiteDescription.getFeaturePaths()).thenReturn(Collections.emptyList());
        final TestSuiteResults results = conformanceTestHarness.runTestSuites(null, null);
        assertNotNull(results);
        assertEquals(0, results.getFeatureTotal());
    }

    @Test
    void runTestSuiteNoTestsEmptyFilter() {
        mockTargetServer();
        when(testSuiteDescription.getFeaturePaths()).thenReturn(Collections.emptyList());
        final TestSuiteResults results = conformanceTestHarness.runTestSuites(Collections.emptyList(), null);
        assertNotNull(results);
        assertEquals(0, results.getFeatureTotal());
    }

    @Test
    void runTestSuiteWithRegistration() throws Exception {
        mockTargetServer();
        when(config.getUserRegistrationEndpoint()).thenReturn(URI.create("https://example.org/register"));
        when(testSuiteDescription.getFeaturePaths()).thenReturn(List.of("feature"));

        final TestSuiteResults results = mockResults(1);
        when(testRunner.runTests(any(), anyInt(), any(), anyBoolean())).thenReturn(results);
        assertEquals(1, conformanceTestHarness.runTestSuites(null, null).getFailCount());
        verify(authManager).registerUser(HttpConstants.ALICE);
        verify(authManager).registerUser(HttpConstants.BOB);
    }

    @Test
    void runTestSuiteInitError() {
        mockTargetServer();
        doThrow(new TestHarnessInitializationException("FAIL"))
                .when(testSubject).loadTestSubjectConfig();
        assertThrows(TestHarnessInitializationException.class, () -> conformanceTestHarness.runTestSuites(null, null));
    }

    @Test
    void runTestSuitePass() {
        mockTargetServer();
        when(testSuiteDescription.getFeaturePaths()).thenReturn(List.of("feature"));
        final TestSuiteResults results = mockResults(1);
        when(testRunner.runTests(any(), anyInt(), any(), anyBoolean())).thenReturn(results);
        assertEquals(1, conformanceTestHarness.runTestSuites(null, null).getFailCount());
    }

    @Test
    void runTestSuiteNullSkips() {
        mockTargetServerWithSkips(null);
        when(testSuiteDescription.getFeaturePaths()).thenReturn(List.of("feature"));
        final Results results = mock(Results.class);
        final TestSuiteResults tsResults = new TestSuiteResults(results);
        when(testRunner.runTests(any(), anyInt(), any(), anyBoolean())).thenReturn(tsResults);
        assertEquals(0, conformanceTestHarness.runTestSuites(null, null).getFailCount());
        verify(results, never()).getSuite();
    }

    @Test
    void runTestSuiteEmptySkips() {
        mockTargetServerWithSkips(Collections.emptyList());
        when(testSuiteDescription.getFeaturePaths()).thenReturn(List.of("feature"));
        final Results results = mock(Results.class);
        final TestSuiteResults tsResults = new TestSuiteResults(results);
        when(testRunner.runTests(any(), anyInt(), any(), anyBoolean())).thenReturn(tsResults);
        assertEquals(0, conformanceTestHarness.runTestSuites(null, null).getFailCount());
        verify(results, never()).getSuite();
    }

    @Test
    void runTestSuiteWithSkips() {
        mockTargetServerWithSkips(List.of("skip"));
        when(testSuiteDescription.getFeaturePaths()).thenReturn(List.of("feature"));
        final Results results = mock(Results.class);
        final Feature feature1 = mock(Feature.class);
        when(feature1.getTags()).thenReturn(null);
        final Feature feature2 = mock(Feature.class);
        when(feature2.getTags()).thenReturn(Collections.emptyList());
        final Feature feature3 = mock(Feature.class);
        when(feature3.getTags()).thenReturn(List.of(new Tag(1, "@skip")));
        final Resource resource = mock(Resource.class);
        when(feature3.getResource()).thenReturn(resource);
        when(resource.getRelativePath()).thenReturn("example/test");
        final TestSuiteResults tsResults = mock(TestSuiteResults.class);
        when(tsResults.getFeatures()).thenReturn(List.of(feature1, feature2, feature3));
        when(testRunner.runTests(any(), anyInt(), any(), anyBoolean())).thenReturn(tsResults);
        assertEquals(0, conformanceTestHarness.runTestSuites(null, null).getFailCount());
        verify(results, never()).getSuite();
        verify(dataRepository, times(1)).createUntestedAssertion(any(), any());
    }

    @Test
    void buildReportsWriteFail() {
        tmp.toFile().setWritable(false);
        conformanceTestHarness.buildReports(Config.RunMode.COVERAGE);
        assertTrue(Files.notExists(tmp.resolve("coverage.html")));
        assertTrue(Files.notExists(tmp.resolve("report.html")));
        assertTrue(Files.notExists(tmp.resolve("report.ttl")));
    }

    @Test
    void buildReportsCoverageReportPass() {
        conformanceTestHarness.buildReports(Config.RunMode.COVERAGE);
        assertTrue(Files.exists(tmp.resolve("coverage.html")));
    }

    @Test
    void buildReportsResultsReportPass() {
        conformanceTestHarness.buildReports(Config.RunMode.TEST);
        assertTrue(Files.exists(tmp.resolve("report.html")));
        assertTrue(Files.exists(tmp.resolve("report.ttl")));
    }

    @Test
    void runSingleTestInitError() {
        mockTargetServer();
        doThrow(new TestHarnessInitializationException("FAIL")).when(testSubject).prepareServer();
        assertNull(conformanceTestHarness.runSingleTest(null));
    }

    @Test
    void runSingleTestPass() {
        mockTargetServer();
        when(config.getWebIds())
                .thenReturn(Map.of(HttpConstants.ALICE, "https://alice.target.example.org/profile/card#me"));
        final TestSuiteResults results = mockResults(1);
        when(testRunner.runTests(any(), anyInt(), any(), anyBoolean())).thenReturn(results);
        assertEquals(1, conformanceTestHarness.runSingleTest("test").getFailCount());
        assertNotNull(conformanceTestHarness.getClients());
        assertEquals(1, conformanceTestHarness.getClients().size());
    }

    @Test
    void getClientsNull() {
        mockTargetServer();
        final TestSuiteResults results = mockResults(1);
        when(testRunner.runTests(any(), anyInt(), any(), anyBoolean())).thenReturn(results);
        assertEquals(1, conformanceTestHarness.runSingleTest("test").getFailCount());
        assertNotNull(conformanceTestHarness.getClients());
        assertEquals(0, conformanceTestHarness.getClients().size());
    }

    @Test
    void runSingleTestRegisterUsersException() throws Exception {
        mockTargetServer();
        when(config.getUserRegistrationEndpoint()).thenReturn(URI.create("https://example.org/register"));
        doThrow(new Exception("FAIL")).when(authManager).registerUser(any());
        assertNull(conformanceTestHarness.runSingleTest("test"));
    }

    @Test
    void runSingleTestRegisterClientsException() throws Exception {
        mockTargetServer();
        when(config.getWebIds())
                .thenReturn(Map.of(HttpConstants.ALICE, "https://alice.target.example.org/profile/card#me"));
        when(authManager.authenticate(any(), anyBoolean())).thenThrow(TestHarnessInitializationException.class);
        assertNull(conformanceTestHarness.runSingleTest("test"));
    }

    @Test
    void cleanUp() {
        conformanceTestHarness.cleanUp();
        verify(testSubject).tearDownServer();
    }

    private void mockTargetServer() {
        mockTargetServerWithSkips(null);
    }

    private void mockTargetServerWithSkips(final List<String> skipTags) {
        final TargetServer targetServer = mock(TargetServer.class);
        when(targetServer.getFeatures()).thenReturn(List.of("feature"));
        when(targetServer.getSkipTags()).thenReturn(skipTags);
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
