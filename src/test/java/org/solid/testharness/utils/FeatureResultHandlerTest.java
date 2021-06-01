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
package org.solid.testharness.utils;

import com.intuit.karate.Suite;
import com.intuit.karate.core.Feature;
import com.intuit.karate.core.FeatureResult;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class FeatureResultHandlerTest {

    @Inject
    FeatureResultHandler featureResultHandler;

    @InjectMock
    DataRepository dataRepository;

    @Test
    void featureReport() {
        final FeatureResult fr = new FeatureResult(Feature.read("src/test/resources/test.feature"));
        featureResultHandler.featureReport(new Suite(), fr);
        verify(dataRepository).addFeatureResult(any(), any(), eq(iri("https://example.org/features/test.feature")));
    }

    @Test
    void featureReportFails() {
        final FeatureResult fr = new FeatureResult(Feature.read("src/test/resources/test.feature"));
        fr.setDisplayName("FAIL");
        featureResultHandler.featureReport(new Suite(), fr);
        verify(dataRepository, never()).addFeatureResult(any(), any(), any());
    }
}
