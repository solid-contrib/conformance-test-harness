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
package org.solid.testharness.utils;

import com.intuit.karate.Suite;
import com.intuit.karate.core.Feature;
import com.intuit.karate.core.FeatureResult;
import com.intuit.karate.resource.Resource;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.io.File;
import java.nio.file.Path;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        verify(dataRepository).addFeatureResult(any(), any(), eq(iri("https://example.org/features/test.feature")),
                any());
    }

    @Test
    void featureReportReadFiles() {
        final FeatureResult fr = mock(FeatureResult.class);
        when(fr.getDisplayName()).thenReturn("src/test/resources/test.feature");
        final Feature feature = mock(Feature.class);
        when(fr.getFeature()).thenReturn(feature);
        final Resource resource = mock(Resource.class);
        when(feature.getResource()).thenReturn(resource);
        final File file = mock(File.class);
        when(resource.getFile()).thenReturn(file);
        when(file.toPath()).thenReturn(Path.of("/missing"));
        assertThrows(RuntimeException.class, () -> featureResultHandler.featureReport(new Suite(), fr));
    }

    @Test
    void featureReportFails() {
        final FeatureResult fr = new FeatureResult(Feature.read("src/test/resources/test.feature"));
        fr.setDisplayName("FAIL");
        featureResultHandler.featureReport(new Suite(), fr);
        verify(dataRepository, never()).addFeatureResult(any(), any(), any(), any());
    }
}
