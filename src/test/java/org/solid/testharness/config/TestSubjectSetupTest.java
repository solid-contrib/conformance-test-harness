package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.TestHarnessInitializationException;
import org.solid.testharness.utils.TestUtils;

import javax.inject.Inject;
import java.net.MalformedURLException;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@QuarkusTest
public class TestSubjectSetupTest {

    @InjectMock
    Config config;

    @Inject
    TestSubject testSubject;

    @Test
    void setupMissingTarget() throws MalformedURLException {
        when(config.getConfigUrl()).thenReturn(TestUtils.getFileUrl("src/test/resources/config-sample.ttl"));
        assertThrows(TestHarnessInitializationException.class, () -> testSubject.loadTestSubjectConfig());
    }

    @Test
    void setupMissingTargetSingleConfig() throws MalformedURLException {
        when(config.getConfigUrl()).thenReturn(TestUtils.getFileUrl("src/test/resources/config-sample-single.ttl"));
        testSubject.loadTestSubjectConfig();
        TargetServer targetServer = testSubject.getTargetServer();
        assertNotNull(targetServer);
        assertEquals("https://github.com/solid/conformance-test-harness/default", targetServer.getSubject());
    }


    @Test
    void setupTargetMultipleConfig() throws MalformedURLException {
        when(config.getConfigUrl()).thenReturn(TestUtils.getFileUrl("src/test/resources/config-sample.ttl"));
        when(config.getTestSubject()).thenReturn(iri("https://github.com/solid/conformance-test-harness/testserver"));
        testSubject.loadTestSubjectConfig();
        TargetServer targetServer = testSubject.getTargetServer();
        assertNotNull(targetServer);
        assertEquals("https://github.com/solid/conformance-test-harness/testserver", targetServer.getSubject());
    }

    @Test
    void setupDifferentTargetSingleConfig() throws MalformedURLException {
        when(config.getConfigUrl()).thenReturn(TestUtils.getFileUrl("src/test/resources/config-sample-single.ttl"));
        when(config.getTestSubject()).thenReturn(iri("https://github.com/solid/conformance-test-harness/missing"));
        assertThrows(TestHarnessInitializationException.class, () -> testSubject.loadTestSubjectConfig());
    }

    @Test
    void setupConfigWithoutServer() throws MalformedURLException {
        when(config.getConfigUrl()).thenReturn(TestUtils.getFileUrl("src/test/resources/harness-sample.ttl"));
        assertThrows(TestHarnessInitializationException.class, () -> testSubject.loadTestSubjectConfig());
    }

    @Test
    void setupBadConfig() throws MalformedURLException {
        when(config.getConfigUrl()).thenReturn(TestUtils.getFileUrl("inrupt-alice.json"));
        assertThrows(TestHarnessInitializationException.class, () -> testSubject.loadTestSubjectConfig());
    }

    @Test
    void registerWithoutServer() throws MalformedURLException {
        assertThrows(TestHarnessInitializationException.class, () -> testSubject.registerClients());
    }
}
