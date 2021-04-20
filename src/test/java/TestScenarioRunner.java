import com.intuit.karate.Results;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.TestRunner;

import javax.inject.Inject;

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
//        String featurePath = "classpath:content-negotiation/content-negotiation-turtle.feature";
        String featurePath = "classpath:writing-resource/containment.feature";
        Results results = testRunner.runTest(featurePath);
        assertNotNull(results);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}
