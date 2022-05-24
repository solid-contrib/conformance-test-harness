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

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.model.datatypes.XMLDateTime;
import org.junit.jupiter.api.Test;
import org.solid.common.vocab.EARL;
import org.solid.testharness.utils.AbstractDataModelTests;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class GeneratedOutputTest extends AbstractDataModelTests {
    @Override
    public String getTestFile() {
        return "src/test/resources/reporting/generatedoutput-testing-feature.ttl";
    }

    @Test
    void getTimestamp() {
        final GeneratedOutput generatedOutput = new GeneratedOutput(iri(NS, "scenario1-output"));
        assertEquals(0, generatedOutput.getTimestamp().compareTo(new XMLDateTime("2021-04-06T17:41:20.889Z")));
    }

    @Test
    void getValue() {
        final GeneratedOutput generatedOutput = new GeneratedOutput(iri(NS, "scenario1-output"));
        assertEquals(EARL.passed.stringValue(), generatedOutput.getValue());
    }

    @Test
    void isFailed() {
        final GeneratedOutput generatedOutput = new GeneratedOutput(iri(NS, "scenario1-output2"));
        assertTrue(generatedOutput.isFailed());
    }

    @Test
    void isPassed() {
        final GeneratedOutput generatedOutput = new GeneratedOutput(iri(NS, "scenario1-output"));
        assertTrue(generatedOutput.isPassed());
    }

    @Test
    void getValueLocalName() {
        final GeneratedOutput generatedOutput = new GeneratedOutput(iri(NS, "scenario1-output"));
        assertEquals(EARL.passed.getLocalName(), generatedOutput.getValueLocalName());
    }

    @Test
    void getDescription() {
        final GeneratedOutput generatedOutput = new GeneratedOutput(iri(NS, "scenario1-output"));
        assertTrue(generatedOutput.getDescription().contains("GET https"));
    }

    @Test
    void getNoDescription() {
        final GeneratedOutput generatedOutput = new GeneratedOutput(iri(NS, "scenario1-step1-output"));
        assertNull(generatedOutput.getDescription());
    }
}
