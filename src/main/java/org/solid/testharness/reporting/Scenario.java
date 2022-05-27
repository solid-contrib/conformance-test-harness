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
import org.eclipse.rdf4j.model.datatypes.XMLDateTime;
import org.solid.common.vocab.DCTERMS;
import org.solid.common.vocab.PROV;
import org.solid.testharness.utils.DataModelBase;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Scenario extends DataModelBase {
    private GeneratedOutput generatedOutput;
    private List<Step> steps;

    public Scenario(final IRI subject) {
        super(subject, ConstructMode.DEEP_WITH_LISTS);
        final List<GeneratedOutput> generatedOutputs = getModelList(PROV.generated, GeneratedOutput.class);
        if (generatedOutputs != null) {
            generatedOutput = generatedOutputs.get(0);
        }
        steps = getModelCollectionList(DCTERMS.hasPart, Step.class);
        if (steps == null) {
            steps = Collections.emptyList();
        }
    }

    public String getTitle() {
        return getLiteralAsString(DCTERMS.title);
    }

    public String getDescription() {
        return getLiteralAsString(DCTERMS.description);
    }

    public String getUsed() {
        return getIriAsString(PROV.used);
    }

    public XMLDateTime getStartTime() {
        return getLiteralAsDateTime(PROV.startedAtTime);
    }
    public XMLDateTime getEndTime() {
        return getLiteralAsDateTime(PROV.endedAtTime);
    }

    public GeneratedOutput getGeneratedOutput() {
        return generatedOutput;
    }

    public List<Step> getSteps() {
        return steps.stream().filter(s -> !s.isBackground()).collect(Collectors.toList());
    }

    public List<Step> getBackgroundSteps() {
        return steps.stream().filter(Step::isBackground).collect(Collectors.toList());
    }

    public boolean isFailed() {
        return generatedOutput.isFailed();
    }

    public boolean isPassed() {
        return generatedOutput.isPassed();
    }
}
