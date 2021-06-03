/*
 * MIT License
 *
 * Copyright (c) 2021 Solid
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
        testSubject.prepareServer();
        final String featurePath = "example/content-negotiation/content-negotiation-turtle.feature";
//        final String featurePath = "example/writing-resource/containment.feature";
        final String uri = Path.of(featurePath).toAbsolutePath().normalize().toUri().toString();
        final TestSuiteResults results = testRunner.runTest(uri);
        testSubject.tearDownServer();
        assertNotNull(results);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}
