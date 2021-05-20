package org.solid.testharness.reporting;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.jupiter.api.Test;
import org.solid.common.vocab.DOAP;
import org.solid.testharness.utils.DataRepository;

import javax.inject.Inject;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class ReportGeneratorNoRepositoryTest {
    @InjectMock
    DataRepository dataRepository;

    @Inject
    ReportGenerator reportGenerator;

    @Test
    void buildHtmlCoverageReportBadRepository() {
        when(dataRepository.getConnection()).thenThrow(new RepositoryException("BAD REPOSITORY"));
        final StringWriter sw = new StringWriter();
        assertThrows(RepositoryException.class, () -> reportGenerator.buildHtmlCoverageReport(sw));
    }

    @Test
    void buildHtmlCoverageReportBadResult() {
        final RepositoryConnection conn = mock(RepositoryConnection.class);
        when(dataRepository.getConnection()).thenReturn(conn);
        when(conn.getStatements(null, DOAP.implements_, null)).thenThrow(new RepositoryException("BAD RESULT"));
        final StringWriter sw = new StringWriter();
        assertThrows(RepositoryException.class, () -> reportGenerator.buildHtmlCoverageReport(sw));
    }
}

