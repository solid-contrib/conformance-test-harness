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
class SpecificationTest extends AbstractDataModelTests {
    @Override
    public String getTestFile() {
        return "src/test/resources/reporting/specification-testing-feature.ttl";
    }

    @Test
    void getSpecificationRequirements() {
        final Specification specification = new Specification(iri(NS, "specification1"));
        final List<SpecificationRequirement> specificationRequirements = specification.getSpecificationRequirements();
        assertEquals(3, specificationRequirements.size());
        assertEquals("https://example.org/specification1#spec1", specificationRequirements.get(0).getSubject());
    }

    @Test
    void getSpecificationRequirementsNull() {
        final Specification specification = new Specification(iri(NS, "specification2"));
        assertNull(specification.getSpecificationRequirements());
        assertEquals(0, specification.countRequirements());
        assertEquals(0, specification.countPassed());
        assertEquals(0, specification.countFailed());
    }

    @Test
    void countRequirements() {
        final Specification specification = new Specification(iri(NS, "specification1"));
        assertEquals(3, specification.countRequirements());
    }

    @Test
    void countFailed0() {
        final Specification specification = new Specification(iri(NS, "specificationPass"));
        assertEquals(0, specification.countFailed());
    }

    @Test
    void countFailed() {
        final Specification specification = new Specification(iri(NS, "specificationFail"));
        assertEquals(1, specification.countFailed());
    }

    @Test
    void countPassed0() {
        final Specification specification = new Specification(iri(NS, "specificationFail"));
        assertEquals(0, specification.countPassed());
    }

    @Test
    void countPassed() {
        final Specification specification = new Specification(iri(NS, "specificationPass"));
        assertEquals(1, specification.countPassed());
    }
}
