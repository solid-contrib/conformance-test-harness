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
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.TestHarnessInitializationException;
import org.solid.testharness.utils.TestUtils;

import javax.inject.Inject;
import java.net.MalformedURLException;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class TestSubjectSetupTest {
    @InjectMock
    Config config;

    @Inject
    TestSubject testSubject;

    @Test
    void setupMissingTarget() throws MalformedURLException {
        when(config.getSubjectsUrl()).thenReturn(TestUtils.getFileUrl("src/test/resources/config/config-sample.ttl"));
        assertThrows(TestHarnessInitializationException.class, () -> testSubject.loadTestSubjectConfig());
    }

    @Test
    void setupMissingTargetSingleConfig() throws MalformedURLException {
        when(config.getSubjectsUrl())
                .thenReturn(TestUtils.getFileUrl("src/test/resources/config/config-sample-single.ttl"));
        testSubject.loadTestSubjectConfig();
        final TargetServer targetServer = testSubject.getTargetServer();
        assertNotNull(targetServer);
        assertEquals("https://github.com/solid/conformance-test-harness/default", targetServer.getSubject());
    }

    @Test
    void setupTargetMultipleConfig() throws MalformedURLException {
        when(config.getSubjectsUrl()).thenReturn(TestUtils.getFileUrl("src/test/resources/config/config-sample.ttl"));
        when(config.getTestSubject()).thenReturn(iri("https://github.com/solid/conformance-test-harness/testserver"));
        testSubject.loadTestSubjectConfig();
        final TargetServer targetServer = testSubject.getTargetServer();
        assertNotNull(targetServer);
        assertEquals("https://github.com/solid/conformance-test-harness/testserver", targetServer.getSubject());
    }

    @Test
    void setupDifferentTargetSingleConfig() throws MalformedURLException {
        when(config.getSubjectsUrl())
                .thenReturn(TestUtils.getFileUrl("src/test/resources/config/config-sample-single.ttl"));
        when(config.getTestSubject())
                .thenReturn(iri("https://github.com/solid/conformance-test-harness/missing"));
        assertThrows(TestHarnessInitializationException.class, () -> testSubject.loadTestSubjectConfig());
    }

    @Test
    void setupConfigWithoutServer() throws MalformedURLException {
        when(config.getSubjectsUrl()).thenReturn(TestUtils.getFileUrl("src/test/resources/config/harness-sample.ttl"));
        assertThrows(TestHarnessInitializationException.class, () -> testSubject.loadTestSubjectConfig());
    }

    @Test
    void setupBadConfig() throws MalformedURLException {
        when(config.getSubjectsUrl()).thenReturn(TestUtils.getFileUrl("jsonld-sample.json"));
        assertThrows(TestHarnessInitializationException.class, () -> testSubject.loadTestSubjectConfig());
    }

    @Test
    void prepareServerWithoutServer() {
        testSubject.setTargetServer(null);
        assertThrows(TestHarnessInitializationException.class, () -> testSubject.prepareServer());
    }
}
