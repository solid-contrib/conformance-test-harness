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
import org.solid.common.vocab.SPEC;
import org.solid.testharness.utils.DataModelBase;

import java.util.List;
import java.util.Optional;

public class Specification extends DataModelBase {
    private Optional<List<SpecificationRequirement>> requirements;

    public Specification(final IRI subject) {
        super(subject);
    }

    public List<SpecificationRequirement> getSpecificationRequirements() {
        if (requirements == null) {
            requirements = Optional.ofNullable(getModelList(SPEC.requirement, SpecificationRequirement.class));
        }
        return requirements.orElse(null);
    }

    public int countRequirements() {
        return getSpecificationRequirements() != null ?  getSpecificationRequirements().size() : 0;
    }

    public long countFailed() {
        return getSpecificationRequirements() != null
                ? getSpecificationRequirements().stream().filter(sr -> sr.countFailed() > 0).count()
                : 0;
    }

    public long countPassed() {
        return getSpecificationRequirements() != null
                ? getSpecificationRequirements().stream().filter(sr -> sr.countPassed() == sr.countTestCases()).count()
                : 0;
    }
}
