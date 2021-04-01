import com.intuit.karate.Results;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.TestRunner;

import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("solid")
@QuarkusTest
public class TestSuiteRunner {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.TestSuiteRunner");

    @Inject
    TestRunner testRunner;

    @Test
    void testSuite() throws FileNotFoundException {
        List<String> featurePaths = testRunner.discoverTests();
        Results results = testRunner.runTests(featurePaths);
        assertNotNull(results);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}
