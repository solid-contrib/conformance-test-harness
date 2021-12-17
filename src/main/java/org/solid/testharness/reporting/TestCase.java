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

import org.eclipse.rdf4j.model.IRI;
import org.solid.common.vocab.DCTERMS;
import org.solid.common.vocab.EARL;
import org.solid.common.vocab.SPEC;
import org.solid.common.vocab.TD;
import org.solid.testharness.utils.DataModelBase;

import java.util.List;

public class TestCase extends DataModelBase {
    private List<Scenario> scenarios;
    private Assertion assertion;

    public TestCase(final IRI subject) {
        super(subject, ConstructMode.INC_REFS);
        scenarios = getModelList(DCTERMS.hasPart, Scenario.class);
        final List<Assertion> assertions = getModelListByObject(EARL.test, Assertion.class);
        if (assertions != null) {
            assertion = assertions.get(0);
        }
    }

    public String getTitle() {
        return getLiteralAsString(DCTERMS.title);
    }

    public String getDescription() {
        return getLiteralAsString(DCTERMS.description);
    }

    public String getStatus() {
        return getIriAsString(TD.reviewStatus);
    }

    public String getTestScript() {
        return getIriAsString(SPEC.testScript);
    }

    public String getRequirementReference() {
        return getIriAsString(SPEC.requirementReference);
    }

    public String getRequirementAnchor() {
        final IRI requirement = getAsIri(SPEC.requirementReference);
        return requirement != null ? requirement.getLocalName() : null;
    }

    public boolean isImplemented() {
        return getIriAsString(SPEC.testScript) != null;
    }

    public Assertion getAssertion() {
        return assertion;
    }

    public List<Scenario> getScenarios() {
        return scenarios;
    }

    public int countScenarios() {
        return scenarios != null ?  scenarios.size() : 0;
    }

    public long countFailed() {
        return scenarios != null ? scenarios.stream().filter(Scenario::isFailed).count() : 0;
    }

    public long countPassed() {
        return scenarios != null ? scenarios.stream().filter(Scenario::isPassed).count() : 0;
    }

    public boolean isFailed() {
        return assertion != null && assertion.isFailed();
    }

    public boolean isPassed() {
        return assertion != null && assertion.isPassed();
    }
}
