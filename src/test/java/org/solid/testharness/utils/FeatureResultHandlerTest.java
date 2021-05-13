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
