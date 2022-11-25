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

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.solid.testharness.ConformanceTestHarness;
import org.solid.testharness.config.Config;
import org.solid.testharness.reporting.TestSuiteResults;
import org.solid.testharness.utils.DataRepository;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("solid")
@QuarkusTest
class TestSuiteRunner {
    @Inject
    Config config;
    @Inject
    DataRepository dataRepository;
    @Inject
    ConformanceTestHarness conformanceTestHarness;

    @BeforeEach
    void setup() {
        config.setOutputDirectory(new File("target"));
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            conn.clear();
        }
    }

    // If you run this and get the error "inStream parameter is null" then run "mvn generate-sources"

    @Test
    void testSuite() throws IOException {
        conformanceTestHarness.initialize();
        config.logConfigSettings(Config.RunMode.TEST);
        final List<String> filters = List.of(
//                "web-access-control",
//                "storage"
//                "default-AWC"
        );
        final TestSuiteResults results = conformanceTestHarness.runTestSuites(filters, null);
        assertNotNull(results);
        conformanceTestHarness.buildReports(Config.RunMode.TEST);
        if (results.getFeatureTotal() > 0) {
            conformanceTestHarness.cleanUp();
        }
        assertFalse(results.hasFailures(), results.getErrorMessages());
    }

    @Test
    @Disabled("not used in normal test runs")
    void testSuiteCoverage() throws Exception {
        conformanceTestHarness.initialize();
        config.logConfigSettings(Config.RunMode.COVERAGE);
        conformanceTestHarness.prepareCoverageReport();
        assertDoesNotThrow(() -> conformanceTestHarness.buildReports(Config.RunMode.COVERAGE));
    }
}
