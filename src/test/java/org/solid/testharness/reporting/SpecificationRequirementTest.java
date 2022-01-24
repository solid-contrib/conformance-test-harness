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
import org.solid.common.vocab.SPEC;
import org.solid.testharness.utils.AbstractDataModelTests;

import java.util.List;
import java.util.Locale;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SpecificationRequirementTest extends AbstractDataModelTests {
    private static final String SPEC_NS = NS + "specification#";

    @Override
    public String getTestFile() {
        return "src/test/resources/reporting/specification-requirement-testing-feature.ttl";
    }

    @Test
    void getRequirementSubject() {
        final SpecificationRequirement requirement = new SpecificationRequirement(iri(SPEC_NS, "requirement"));
        assertEquals(SPEC.Server.stringValue(), requirement.getRequirementSubject());
    }

    @Test
    void getRequirementSubjectClass() {
        final SpecificationRequirement requirement = new SpecificationRequirement(iri(SPEC_NS, "requirement"));
        assertEquals(SPEC.Server.getLocalName().toLowerCase(Locale.ROOT), requirement.getRequirementSubjectClass());
    }

    @Test
    void getRequirementLevel() {
        final SpecificationRequirement requirement = new SpecificationRequirement(iri(SPEC_NS, "requirement"));
        assertEquals(SPEC.MUST.stringValue(), requirement.getRequirementLevel());
    }

    @Test
    void getStatement() {
        final SpecificationRequirement requirement = new SpecificationRequirement(iri(SPEC_NS, "requirement"));
        assertEquals("excerpt of requirement 1", requirement.getStatement());
    }

    @Test
    void getTestCase() {
        final SpecificationRequirement requirement = new SpecificationRequirement(iri(SPEC_NS, "requirement"));
        final List<TestCase> testCases = requirement.getTestCases();
        assertNotNull(testCases);
        assertEquals(1, testCases.size());
        assertEquals("Group 1", testCases.get(0).getTitle());
    }

    @Test
    void getTestCaseMissing() {
        final SpecificationRequirement requirement = new SpecificationRequirement(iri(SPEC_NS, "requirement2"));
        final List<TestCase> testCases = requirement.getTestCases();
        assertNull(testCases);
        assertEquals(0, requirement.countTestCases());
        assertEquals(0, requirement.countPassed());
        assertEquals(0, requirement.countFailed());
        assertFalse(requirement.isFailed());
        assertFalse(requirement.isPassed());
    }

    @Test
    void countTestCases() {
        final SpecificationRequirement requirement = new SpecificationRequirement(iri(SPEC_NS, "requirement"));
        assertEquals(1, requirement.countTestCases());
    }

    @Test
    void countFailed() {
        final SpecificationRequirement requirement = new SpecificationRequirement(iri(SPEC_NS, "requirementFail"));
        assertEquals(1, requirement.countFailed());
    }

    @Test
    void countPassed() {
        final SpecificationRequirement requirement = new SpecificationRequirement(iri(SPEC_NS, "requirementPass"));
        assertEquals(1, requirement.countPassed());
    }

    @Test
    void isFailed() {
        final SpecificationRequirement requirement = new SpecificationRequirement(iri(SPEC_NS, "requirementFail"));
        assertTrue(requirement.isFailed());
    }

    @Test
    void isPassed() {
        final SpecificationRequirement requirement = new SpecificationRequirement(iri(SPEC_NS, "requirementPass"));
        assertTrue(requirement.isPassed());
    }
}
