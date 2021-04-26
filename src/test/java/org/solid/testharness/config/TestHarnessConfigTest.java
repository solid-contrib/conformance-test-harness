package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.http.AuthManager;
import org.solid.testharness.http.SolidClient;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
public class TestHarnessConfigTest {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.config.TestHarnessConfigTest");

    @Inject
    TestHarnessConfig testHarnessConfig;

    @InjectMock
    AuthManager authManager;

    @Test
    void registerClients() throws Exception {
        when(authManager.authenticate(anyString(), any(TargetServer.class))).thenReturn(new SolidClient());
        testHarnessConfig.loadTestSubjectConfig();
        testHarnessConfig.registerClients();
        Map<String, SolidClient> clients = testHarnessConfig.getClients();
        assertNotNull(clients);
        assertEquals(2, clients.size());
    }

    @Test
    void registerClientsWithAuthException() throws Exception {
        when(authManager.authenticate(anyString(), any(TargetServer.class))).thenThrow(new Exception("Failed as expected"));
        testHarnessConfig.loadTestSubjectConfig();
        assertThrows(TestHarnessInitializationException.class, () -> testHarnessConfig.registerClients());
    }

    @Test
    void getTargetServer() {
        testHarnessConfig.loadTestSubjectConfig();
        TargetServer targetServer = testHarnessConfig.getTargetServer();
        assertNotNull(targetServer);
    }

    @Test
    void getServers() {
        testHarnessConfig.loadTestSubjectConfig();
        Map<String, TargetServer> servers = testHarnessConfig.getServers();
        assertNotNull(servers);
        assertEquals(2, servers.size());
        assertTrue(servers.containsKey("https://github.com/solid/conformance-test-harness/testserver"));
        assertEquals("https://pod-compat.inrupt.com", servers.get("https://github.com/solid/conformance-test-harness/testserver").getServerRoot());
        assertTrue(servers.containsKey("https://github.com/solid/conformance-test-harness/testserver2"));
        assertEquals("http://localhost:3000", servers.get("https://github.com/solid/conformance-test-harness/testserver2").getServerRoot());
    }

    @Test
    void getClients() throws Exception {
        when(authManager.authenticate(anyString(), any(TargetServer.class))).thenReturn(new SolidClient());
        testHarnessConfig.loadTestSubjectConfig();
        testHarnessConfig.registerClients();
        Map<String, SolidClient> clients = testHarnessConfig.getClients();
        assertNotNull(clients);
        assertEquals(2, clients.size());
        assertTrue(clients.containsKey("alice"));
        assertNotNull(clients.get("alice"));
        assertTrue(clients.containsKey("bob"));
        assertNotNull(clients.get("bob"));
    }

    @Test
    void getTestSuiteDescription() {
        URL url = testHarnessConfig.getTestSuiteDescription();
        assertEquals("file", url.getProtocol());
    }

    @Test
    void getCredentialsDirectory() {
        File file = testHarnessConfig.getCredentialsDirectory();
        assertTrue(file.exists() && file.isDirectory());
    }

    @Test
    void getPathMappings() {
        List<PathMappings.Mapping> mappings = testHarnessConfig.getPathMappings();
        assertNotNull(mappings);
        assertEquals(1, mappings.size());
        assertEquals("https://example.org", mappings.get(0).prefix);
        assertEquals("src/test/resources/dummy-features", mappings.get(0).path);
    }
}
