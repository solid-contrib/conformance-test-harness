import com.intuit.karate.Results;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.TestRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("solid")
public class TestSuiteRunner {
    private static final Logger logger = LoggerFactory.getLogger("TestSuiteRunner");

    @Test
    void testSuite() {
        TestRunner testRunner = new TestRunner();
        Results results = testRunner.runTests();
        assertNotNull(results);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}
