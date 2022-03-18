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
package org.solid.testharness.http;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.testharness.config.Config;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@QuarkusTest
class ClientRegistryTest {
    @Inject
    ClientRegistry clientRegistry;

    @InjectMock
    Config config;

    @BeforeEach
    void setup() {
        when(config.getReadTimeout()).thenReturn(5000);
        when(config.getAgent()).thenReturn("AGENT");
    }

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
