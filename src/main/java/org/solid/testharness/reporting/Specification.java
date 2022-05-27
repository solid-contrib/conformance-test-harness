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

import java.util.List;

public class Specification extends DataModelBase {
    private List<SpecificationRequirement> requirements;

    public Specification(final IRI subject) {
        super(subject);
        requirements = getModelList(SPEC.requirement, SpecificationRequirement.class);
    }

    public List<SpecificationRequirement> getSpecificationRequirements() {
        return requirements;
    }

    public int countRequirements() {
        return requirements != null ?  requirements.size() : 0;
    }

    public long countFailed() {
        return requirements != null
                ? requirements.stream().filter(SpecificationRequirement::isFailed).count()
                : 0;
    }

    public long countPassed() {
        return requirements != null
                ? requirements.stream().filter(SpecificationRequirement::isPassed).count()
                : 0;
    }
}
