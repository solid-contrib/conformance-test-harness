/*
 * MIT License
 *
 * Copyright (c) 2019 - 2022 W3C Solid Community Group
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

import org.eclipse.rdf4j.model.IRI;
import org.solid.common.vocab.SPEC;
import org.solid.testharness.utils.DataModelBase;
import org.solid.testharness.utils.Namespaces;

import java.util.List;
import java.util.Locale;

public class SpecificationRequirement extends DataModelBase {
    private final List<TestCase> testCases;

    public SpecificationRequirement(final IRI subject) {
        super(subject, ConstructMode.INC_REFS);
        testCases = getModelListByObject(SPEC.requirementReference, TestCase.class);
    }

    @Override
    public String getAnchor() {
        return Namespaces.getSpecificationNamespace(subject) + "_" + subject.getLocalName();
    }

    public String getRequirementSubject() {
        return getIriAsString(SPEC.requirementSubject);
    }

    public String getRequirementSubjectClass() {
        final IRI iri = getAsIri(SPEC.requirementSubject);
        return iri != null ? iri.getLocalName().toLowerCase(Locale.ROOT) : null;
    }

    public String getRequirementLevel() {
        return getIriAsString(SPEC.requirementLevel);
    }

    public String getStatement() {
        return getIriAsString(SPEC.statement);
    }

    public List<TestCase> getTestCases() {
        return testCases;
    }

    public int countTestCases() {
        return getTestCases() != null ?  getTestCases().size() : 0;
    }

    public long countFailed() {
        return testCases != null
                ? testCases.stream().filter(TestCase::isFailed).count()
                : 0;
    }

    public long countPassed() {
        return testCases != null
                ? testCases.stream().filter(TestCase::isPassed).count()
                : 0;
    }

    public boolean isFailed() {
        return testCases != null && testCases.stream().anyMatch(TestCase::isFailed);
    }

    public boolean isPassed() {
        return testCases != null && testCases.stream().allMatch(TestCase::isPassed);
    }
}
