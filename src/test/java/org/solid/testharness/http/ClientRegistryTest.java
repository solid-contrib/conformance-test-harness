package org.solid.testharness.http;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ClientRegistryTest {
    @Inject
    ClientRegistry clientRegistry;

    @Test
    void register() {
        clientRegistry.register("registerTest", new Client.Builder("registerTest").build());
        assertEquals("registerTest", clientRegistry.getClient("registerTest").getUser());
    }

    @Test
    void unregister() {
        clientRegistry.register("unregisterTest", new Client.Builder("unregisterTest").build());
        assertEquals("registerTest", clientRegistry.getClient("registerTest").getUser());
        clientRegistry.unregister("unregisterTest");
        assertNull(clientRegistry.getClient("unregisterTest"));
    }

    @Test
    void hasClient() {
        assertTrue(clientRegistry.hasClient(ClientRegistry.DEFAULT));
    }

    @Test
    void hasClientFalse() {
        assertFalse(clientRegistry.hasClient("unknown"));
    }

    @Test
    void getClient() {
        clientRegistry.register("newClient", new Client.Builder("newClient").build());
        assertEquals("newClient", clientRegistry.getClient("newClient").getUser());
    }

    @Test
    void getClientSessionBased() {
        assertTrue(clientRegistry.getClient(ClientRegistry.SESSION_BASED).getHttpClient().cookieHandler().isPresent());
    }

    @Test
    void getClientDefault() {
        assertFalse(clientRegistry.getClient(ClientRegistry.DEFAULT).getHttpClient().cookieHandler().isPresent());
    }

    @Test
    void getClientMissing() {
        assertNull(clientRegistry.getClient("unknown"));
    }

    @Test
    void getClientNoLabel() {
        assertEquals(clientRegistry.getClient(ClientRegistry.DEFAULT), clientRegistry.getClient(null));
    }
}
