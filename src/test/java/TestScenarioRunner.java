import com.intuit.karate.Results;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.TestRunner;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("solid")
@QuarkusTest
public class TestScenarioRunner {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.TestScenarioRunner");

    @Inject
    TestRunner testRunner;

    @Test
    void testScenario() {
        Results results = testRunner.runTests(List.of("classpath:content-negotiation/content-negotiation-turtle.feature"));
        assertNotNull(results);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}
