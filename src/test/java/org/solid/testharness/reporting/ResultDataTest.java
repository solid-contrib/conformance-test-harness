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
    void getSubject() {
        final ResultData resultData = new ResultData(Collections.emptyList(), Collections.emptyList(), null);
        assertNotNull(resultData.getIdentifier());
    }

    @Test
    void getHtmlPrefixes() {
        final ResultData resultData = new ResultData(Collections.emptyList(), Collections.emptyList(), null);
        final String prefixes = resultData.getPrefixes();
        assertTrue(prefixes.contains("rdf: http://www.w3.org/1999/02/22-rdf-syntax-ns#"));
        assertTrue(prefixes.contains("rdfs: http://www.w3.org/2000/01/rdf-schema#"));
        assertTrue(prefixes.contains("xsd: http://www.w3.org/2001/XMLSchema#"));
        assertTrue(prefixes.contains("owl: http://www.w3.org/2002/07/owl#"));
        assertTrue(prefixes.contains("dcterms: http://purl.org/dc/terms/"));
        assertTrue(prefixes.contains("doap: http://usefulinc.com/ns/doap#"));
        assertTrue(prefixes.contains("solid: http://www.w3.org/ns/solid/terms#"));
        assertTrue(prefixes.contains("solid-test: https://github.com/solid/conformance-test-harness/vocab#"));
        assertTrue(prefixes.contains("earl: http://www.w3.org/ns/earl#"));
        assertTrue(prefixes.contains("td: http://www.w3.org/2006/03/test-description#"));
        assertTrue(prefixes.contains("prov: http://www.w3.org/ns/prov#"));
        assertTrue(prefixes.contains("spec: http://www.w3.org/ns/spec#"));
        assertTrue(prefixes.contains("schema: http://schema.org/"));
    }

    @Test
    void getSpecifications() {
        final ResultData resultData = new ResultData(
                List.of(iri("https://example.org/specification1"), iri("https://example.org/specification2")),
                List.of(iri("https://example.org/test1")), null
        );
        final List<Specification> specifications = resultData.getSpecifications();
        assertNotNull(specifications);
        assertEquals(2, specifications.size());
        assertEquals(3, specifications.get(0).getSpecificationRequirements().size());
        assertEquals(1, specifications.get(1).getSpecificationRequirements().size());
        final List<TestCase> testCases = resultData.getTestCases();
        assertNotNull(testCases);
        assertEquals(1, testCases.size());
        assertEquals("https://example.org/specification1#spec1", testCases.get(0).getRequirementReference());
    }

    @Test
    void getAssertor() {
        final ResultData resultData = new ResultData(Collections.emptyList(), Collections.emptyList(), null);
        assertNotNull(resultData.getAssertor());
    }

    @Test
    void getSpecificationTests() {
        final ResultData resultData = new ResultData(Collections.emptyList(), Collections.emptyList(), null);
        assertNotNull(resultData.getSpecificationTests());
    }

    @Test
    void getTestSubject() {
        final ResultData resultData = new ResultData(Collections.emptyList(), Collections.emptyList(),
                new TestSuiteResults(null));
        assertNotNull(resultData.getTestSubject());
    }

    @Test
    void getTestSubjectNull() {
        final ResultData resultData = new ResultData(Collections.emptyList(), Collections.emptyList(), null);
        assertNull(resultData.getTestSubject());
    }

    @Test
    void getTestSuiteResults() {
        final ResultData resultData = new ResultData(Collections.emptyList(), Collections.emptyList(),
                new TestSuiteResults(null));
        assertNotNull(resultData.getTestSuiteResults());
    }
}
