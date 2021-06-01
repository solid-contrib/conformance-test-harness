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
package org.solid.testharness.reporting;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.solid.common.vocab.EARL;
import org.solid.testharness.utils.AbstractDataModelTests;
import org.solid.testharness.utils.Namespaces;
import org.solid.testharness.utils.TestData;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class AssertionTest extends AbstractDataModelTests {
    @Override
    public String getTestFile() {
        return "src/test/resources/assertiontest-testing-feature.ttl";
    }

    @Test
    void getAssertedBy() {
        final Assertion assertion = new Assertion(iri(NS, "assertion1"));
        assertEquals(Namespaces.TEST_HARNESS_URI, assertion.getAssertedBy());
    }

    @Test
    void getTest() {
        final Assertion assertion = new Assertion(iri(NS, "assertion1"));
        assertEquals(TestData.SAMPLE_BASE + "/test", assertion.getTest());
    }

    @Test
    void getTestSubject() {
        final Assertion assertion = new Assertion(iri(NS, "assertion1"));
        assertEquals(Namespaces.TEST_HARNESS_URI + "testserver", assertion.getTestSubject());
    }

    @Test
    void getMode() {
        final Assertion assertion = new Assertion(iri(NS, "assertion1"));
        assertEquals(EARL.automatic.stringValue(), assertion.getMode());
    }

    @Test
    void getResult() {
        final Assertion assertion = new Assertion(iri(NS, "assertion1"));
        assertNotNull(assertion.getResult());
    }

    @Test
    void getNoResult() {
        final Assertion assertion = new Assertion(iri(NS, "assertion2"));
        assertNull(assertion.getResult());
    }
}
