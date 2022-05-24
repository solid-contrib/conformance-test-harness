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
import io.quarkus.test.junit.mockito.InjectMock;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.jupiter.api.Test;
import org.solid.testharness.config.Config;
import org.solid.testharness.discovery.TestSuiteDescription;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@QuarkusTest
class TestSuiteDescriptionNoRepositoryTest {
    @InjectMock
    DataRepository dataRepository;

    @Inject
    TestSuiteDescription testSuiteDescription;

    @Test
    void setNonRunningTestAssertionsException() {
        when(dataRepository.getConnection()).thenThrow(new RepositoryException("BAD REPOSITORY"));
        assertThrows(TestHarnessInitializationException.class,
                () -> testSuiteDescription.setNonRunningTestAssertions(null, null));
    }


    @Test
    void prepareTestCasesException() {
        when(dataRepository.getConnection()).thenThrow(new RepositoryException("BAD REPOSITORY"));
        assertThrows(TestHarnessInitializationException.class,
                () -> testSuiteDescription.prepareTestCases(Config.RunMode.TEST));
    }
}

