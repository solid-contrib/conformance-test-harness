import com.intuit.karate.Results;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.TestRunner;
import org.solid.testharness.config.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("solid")
public class TestSuiteRunner {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.TestSuiteRunner");

    @Test
    void testSuite() {
        Bootstrap bootstrap = Bootstrap.getInstance();
        TestRunner testRunner = bootstrap.getTestRunner();
        Results results = testRunner.runTests();
        assertNotNull(results);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}
