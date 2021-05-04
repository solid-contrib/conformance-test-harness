import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.solid.testharness.TestRunner;
import org.solid.testharness.config.TestSubject;
import org.solid.testharness.reporting.TestSuiteResults;

import javax.inject.Inject;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("solid")
@QuarkusTest
public class TestScenarioRunner {
    @Inject
    TestSubject testSubject;
    @Inject
    TestRunner testRunner;

    @Test
    void testScenario() {
        testSubject.loadTestSubjectConfig();
        testSubject.registerClients();
//        String featurePath = "content-negotiation/content-negotiation-turtle.feature";
        String featurePath = "example/writing-resource/containment.feature";
        String uri = Path.of(featurePath).toAbsolutePath().normalize().toUri().toString();
        TestSuiteResults results = testRunner.runTest(uri);
        assertNotNull(results);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}
