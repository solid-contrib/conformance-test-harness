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

import java.util.Collections;
import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ResultDataTest extends AbstractDataModelTests {
    @Override
    public String getTestFile() {
        return "src/test/resources/reporting/resultdata-testing-feature.ttl";
    }

    @Test
    void getHtmlPrefixes() {
        final ResultData resultData = new ResultData(Collections.emptyList());
        assertEquals("rdf: http://www.w3.org/1999/02/22-rdf-syntax-ns# " +
                "rdfs: http://www.w3.org/2000/01/rdf-schema# " +
                "xsd: http://www.w3.org/2001/XMLSchema# " +
                "dcterms: http://purl.org/dc/terms/ " +
                "doap: http://usefulinc.com/ns/doap# " +
                "solid: http://www.w3.org/ns/solid/terms# " +
                "solid-test: https://github.com/solid/conformance-test-harness/vocab# " +
                "earl: http://www.w3.org/ns/earl# " +
                "td: http://www.w3.org/2006/03/test-description# " +
                "spec: http://www.w3.org/ns/spec#", resultData.getPrefixes());
    }

    @Test
    void getSpecifications() {
        final ResultData resultData = new ResultData(
                List.of(iri("https://example.org/specification1"), iri("https://example.org/specification2"))
        );
        final List<Specification> specifications = resultData.getSpecifications();
        assertNotNull(specifications);
        assertEquals(2, specifications.size());
        assertEquals(3, specifications.get(0).getSpecificationRequirements().size());
        assertEquals(1, specifications.get(1).getSpecificationRequirements().size());
    }

    @Test
    void getAssertor() {
        final ResultData resultData = new ResultData(Collections.emptyList());
        assertNotNull(resultData.getAssertor());
    }
}
