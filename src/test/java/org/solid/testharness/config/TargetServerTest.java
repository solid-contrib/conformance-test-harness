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
package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.*;

import javax.inject.Inject;
import java.net.URL;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class TargetServerTest {
    @Inject
    DataRepository dataRepository;

    @Test
    public void parseTargetServer() throws Exception {
        final URL testFile = TestUtils.getFileUrl("src/test/resources/targetserver-testing-feature.ttl");
        TestData.insertData(dataRepository, testFile);
        final TargetServer targetServer = new TargetServer(iri(TestData.SAMPLE_NS, "testserver"));
        assertAll("targetServer",
                () -> assertNotNull(targetServer.getFeatures()),
                () -> assertEquals(true, targetServer.getFeatures().get("feature1")),
                () -> assertNull(targetServer.getFeatures().get("feature2")),
                () -> assertEquals("https://tester", targetServer.getOrigin()),
                () -> assertEquals(true, targetServer.isSetupRootAcl()),
                () -> assertEquals(4, targetServer.getMaxThreads()),
                () -> assertEquals(false, targetServer.isDisableDPoP())
        );
    }

    @Test
    public void parseTargetServerWithBadThreads() throws Exception {
        final URL testFile = TestUtils.getFileUrl("src/test/resources/targetserver-testing-feature.ttl");
        TestData.insertData(dataRepository, testFile);
        assertThrows(TestHarnessInitializationException.class, () -> new TargetServer(iri(TestData.SAMPLE_NS, "bad")));
    }
}
