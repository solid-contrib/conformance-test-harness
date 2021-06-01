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
import org.solid.testharness.utils.AbstractDataModelTests;

import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SpecificationTestCaseTest extends AbstractDataModelTests {
    @Override
    public String getTestFile() {
        return "src/test/resources/specificationtestcase-testing-feature.ttl";
    }

    @Test
    void getTitle() {
        final SpecificationTestCase specificationTestCase = new SpecificationTestCase(iri(NS, "test1"));
        assertEquals("Title", specificationTestCase.getTitle());
    }

    @Test
    void getDescription() {
        final SpecificationTestCase specificationTestCase = new SpecificationTestCase(iri(NS, "test1"));
        assertEquals("Description", specificationTestCase.getDescription());
    }

    @Test
    void getSpecificationReference() {
        final SpecificationTestCase specificationTestCase = new SpecificationTestCase(iri(NS, "test1"));
        assertEquals(NS + "spec", specificationTestCase.getSpecificationReference());
    }

    @Test
    void getTestCases() {
        final SpecificationTestCase specificationTestCase = new SpecificationTestCase(iri(NS, "test1"));
        final List<TestCase> testCases = specificationTestCase.getTestCases();
        assertNotNull(testCases);
        assertEquals(1, testCases.size());
    }

    @Test
    void getEmptyTestCases() {
        final SpecificationTestCase specificationTestCase = new SpecificationTestCase(iri(NS, "test2"));
        final List<TestCase> testCases = specificationTestCase.getTestCases();
        assertNull(testCases);
    }
}
