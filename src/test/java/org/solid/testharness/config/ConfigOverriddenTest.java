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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.inject.Inject;
import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ConfigOverriddenTest {
    @Inject
    Config config;

    @BeforeAll
    void setup() {
        config.setTestSubject(iri("https://github.com/solid/conformance-test-harness/testserver2"));
        config.setSubjectsUrl("https://example.org/test-subjects.ttl");
        config.setTestSources(List.of("https://example.org/testsuite.ttl"));
    }

    @Test
    void getTestSubjectChanged() {
        assertEquals("https://github.com/solid/conformance-test-harness/testserver2",
                config.getTestSubject().stringValue()
        );
    }

    @Test
    void getSubjectsUrlChanged() {
        assertEquals("https://example.org/test-subjects.ttl", config.getSubjectsUrl().toString());
    }

    @Test
    void getTestSourcesChanged() {
        assertEquals("[https://example.org/testsuite.ttl]", config.getTestSources().toString());
    }
}
