package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.solid.testharness.http.AuthManager;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.http.SolidClient;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.inject.Inject;
import java.util.Map;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
public class TestSubjectTest {
    @Inject
    Config config;

    @Inject
    TestSubject testSubject;

    @InjectMock
    AuthManager authManager;

    @Test
    void registerClients() throws Exception {
        when(authManager.authenticate(anyString(), any(TargetServer.class))).thenReturn(new SolidClient());
        config.setTestSubject(iri("https://github.com/solid/conformance-test-harness/testserver"));
        testSubject.loadTestSubjectConfig();
        testSubject.registerClients();
        final Map<String, SolidClient> clients = testSubject.getClients();
        assertNotNull(clients);
        assertEquals(2, clients.size());
    }

    @Test
    void registerClientsWithAuthException() throws Exception {
        when(authManager.authenticate(anyString(), any(TargetServer.class)))
                .thenThrow(new Exception("Failed as expected"));
        config.setTestSubject(iri("https://github.com/solid/conformance-test-harness/testserver"));
        testSubject.loadTestSubjectConfig();
        assertThrows(TestHarnessInitializationException.class, () -> testSubject.registerClients());
    }

    @Test
    void getTargetServer() {
        config.setTestSubject(iri("https://github.com/solid/conformance-test-harness/testserver"));
        testSubject.loadTestSubjectConfig();
        final TargetServer targetServer = testSubject.getTargetServer();
        assertNotNull(targetServer);
        assertEquals("https://github.com/solid/conformance-test-harness/testserver", targetServer.getSubject());
    }

    @Test
    void getTargetServer2() {
        config.setTestSubject(iri("https://github.com/solid/conformance-test-harness/testserver2"));
        testSubject.loadTestSubjectConfig();
        final TargetServer targetServer = testSubject.getTargetServer();
        assertNotNull(targetServer);
        assertEquals("https://github.com/solid/conformance-test-harness/testserver2", targetServer.getSubject());
    }

    @Test
    void getTargetServerDefault() {
        config.setTestSubject(iri("https://github.com/solid/conformance-test-harness/testserver"));
        testSubject.loadTestSubjectConfig();
        final TargetServer targetServer = testSubject.getTargetServer();
        assertNotNull(targetServer);
    }

    @Test
    void getClients() throws Exception {
        when(authManager.authenticate(anyString(), any(TargetServer.class))).thenReturn(new SolidClient());
        testSubject.loadTestSubjectConfig();
        testSubject.registerClients();
        final Map<String, SolidClient> clients = testSubject.getClients();
        assertNotNull(clients);
        assertEquals(2, clients.size());
        assertTrue(clients.containsKey(HttpConstants.ALICE));
        assertNotNull(clients.get(HttpConstants.ALICE));
        assertTrue(clients.containsKey(HttpConstants.BOB));
        assertNotNull(clients.get(HttpConstants.BOB));
    }
}
