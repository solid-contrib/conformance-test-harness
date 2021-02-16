import org.solid.testharness.TestRunner;
import com.intuit.karate.Results;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestSuiteRunner {
    @Test
    void testSelected() {
        TestRunner testRunner = new TestRunner();
        Results results = testRunner.runTests();
        assertNotNull(results);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}
