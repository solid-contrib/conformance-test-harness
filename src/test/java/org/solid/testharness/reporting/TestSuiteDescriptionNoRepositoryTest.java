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

