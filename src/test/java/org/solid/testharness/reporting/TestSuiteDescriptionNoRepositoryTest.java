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
import io.quarkus.test.junit.mockito.InjectMock;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.jupiter.api.Test;
import org.solid.testharness.discovery.TestSuiteDescription;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.inject.Inject;
import java.net.URL;
import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class TestSuiteDescriptionNoRepositoryTest {
    @InjectMock
    DataRepository dataRepository;

    @Inject
    TestSuiteDescription testSuiteDescription;

    @Test
    void loadException() {
        when(dataRepository.getConnection()).thenThrow(new RepositoryException("BAD REPOSITORY"));
        assertThrows(TestHarnessInitializationException.class,
                () -> testSuiteDescription.load(List.of(new URL("https://example.org/specification-sample-1.ttl"))));

    }

    @Test
    void locateTestCasesException() {
        when(dataRepository.getConnection()).thenThrow(new RepositoryException("BAD REPOSITORY"));
        final List<IRI> testCases = List.of(iri("https://example.org/dummy/group3/feature1"));
        assertThrows(TestHarnessInitializationException.class, () -> testSuiteDescription.locateTestCases(testCases));
    }

    @Test
    void getAllTestCasesException() {
        when(dataRepository.getConnection()).thenThrow(new RepositoryException("BAD REPOSITORY"));
        final List<IRI> testCases = List.of(iri("https://example.org/dummy/group3/feature1"));
        assertThrows(TestHarnessInitializationException.class, () -> testSuiteDescription.getAllTestCases());
    }
}

