import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.solid.testharness.ConformanceTestHarness;
import org.solid.testharness.config.Config;
import org.solid.testharness.reporting.TestSuiteResults;
import org.solid.testharness.utils.DataRepository;

import javax.inject.Inject;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@Tag("solid")
@QuarkusTest
public class TestSuiteRunner {
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

    @Test
    void testSuite() {
        final TestSuiteResults results = conformanceTestHarness.runTestSuites();
        assertNotNull(results);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }

    @Test
    void testSuiteCoverage() throws Exception {
        conformanceTestHarness.initialize();
        assertTrue(conformanceTestHarness.createCoverageReport());
    }
}
