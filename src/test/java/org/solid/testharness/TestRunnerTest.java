package org.solid.testharness;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.solid.testharness.reporting.TestSuiteResults;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TestRunnerTest {

    @Inject
    TestRunner testRunner;

    @Test
    void runTests() {
        TestSuiteResults results = testRunner.runTests(List.of("src/test/resources/test.feature"), 1);
        assertNotNull(results);
        assertEquals(1, results.getFailCount());
    }

    @Test
    void runTestsEmpty() {
        TestSuiteResults results = testRunner.runTests(Collections.emptyList(), 10);
        assertNotNull(results);
        assertEquals(0, results.getFailCount());
    }

    @Test
    void runTestsNoThreads() {
        TestSuiteResults results = testRunner.runTests(Collections.emptyList(), 0);
        assertNotNull(results);
        assertEquals(0, results.getFailCount());
    }

    @Test
    void runTestsNoList() {
        TestSuiteResults results = testRunner.runTests(null, 1);
        assertNotNull(results);
        assertEquals(0, results.getFailCount());
    }

    @Test
    void runTestsMissing() {
        assertThrows(RuntimeException.class, () -> testRunner.runTests(List.of("missing.feature"), 1));
    }

    @Test
    void runTest() {
        TestSuiteResults results = testRunner.runTest("src/test/resources/test.feature");
        assertNotNull(results);
        assertEquals(1, results.getFailCount());
    }

    @Test
    void runTestEmpty() {
        assertThrows(IllegalArgumentException.class, () -> testRunner.runTest(null));
    }

    @Test
    void runTestBlank() {
        assertThrows(IllegalArgumentException.class, () -> testRunner.runTest(""));
    }

    @Test
    void runTestMissing() {
        assertThrows(RuntimeException.class, () -> testRunner.runTest("missing.feature"));
    }
}
