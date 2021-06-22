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

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class StepTest extends AbstractDataModelTests {
    @Override
    public String getTestFile() {
        return "src/test/resources/reporting/step-testing-feature.ttl";
    }

    @Test
    void getTitle() {
        final Step step = new Step(iri(NS, "step1"));
        assertEquals("TITLE", step.getTitle());
    }

    @Test
    void getScriptLocation() {
        final Step step = new Step(iri(NS, "step1"));
        assertEquals("https://example.org/test.feature#line=11", step.getUsed());
    }

    @Test
    void getScenario() {
        final Step step = new Step(iri(NS, "step1"));
        assertEquals("https://example.org/scenario1", step.getScenario());
    }

    @Test
    void isNotBackground() {
        final Step step = new Step(iri(NS, "step1"));
        assertFalse(step.isBackground());
    }

    @Test
    void isBackground() {
        final Step step = new Step(iri(NS, "step2"));
        assertTrue(step.isBackground());
    }

    @Test
    void getGeneratedOutput() {
        final Step step = new Step(iri(NS, "step1"));
        assertNotNull(step.getGeneratedOutput());
    }

    @Test
    void getNoGeneratedOutput() {
        final Step step = new Step(iri(NS, "step2"));
        assertNull(step.getGeneratedOutput());
    }
}
