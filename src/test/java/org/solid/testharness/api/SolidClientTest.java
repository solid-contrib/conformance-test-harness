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
package org.solid.testharness.api;

import org.junit.jupiter.api.Test;
import org.solid.testharness.http.Client;
import org.solid.testharness.http.SolidClientProvider;
import org.solid.testharness.utils.TestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SolidClientTest {
    @Test
    void getAuthHeaders() {
        final Client client = mock(Client.class);
        when(client.getAuthHeaders(any(), any())).thenReturn(Collections.emptyMap());
        final SolidClientProvider solidClientProvider = mock(SolidClientProvider.class);
        when(solidClientProvider.getClient()).thenReturn(client);

        final SolidClient solidClient = new SolidClient(solidClientProvider);
        final var headers = solidClient.getAuthHeaders("GET", "URL");
        assertTrue(headers.isEmpty());
    }

    @Test
    void getAuthHeadersException() {
        final SolidClientProvider solidClientProvider = mock(SolidClientProvider.class);
        when(solidClientProvider.getClient()).thenThrow(new NullPointerException("FAIL"));
        final SolidClient solidClient = new SolidClient(solidClientProvider);
        assertThrows(TestHarnessApiException.class, () -> solidClient.getAuthHeaders("GET", "URL"));
    }

    @Test
    void send() {
        final Client client = mock(Client.class);
        final HttpResponse<String> mockStringResponse = TestUtils.mockStringResponse(200, "data");
        when(client.send(any(), any(), any(), any(), any(), eq(false))).thenReturn(mockStringResponse);
        final SolidClientProvider solidClientProvider = mock(SolidClientProvider.class);
        when(solidClientProvider.getClient()).thenReturn(client);

        final SolidClient solidClient = new SolidClient(solidClientProvider);
        final var response = solidClient.send("GET", TestUtils.SAMPLE_BASE, "", null, null);
        assertFalse(response.isEmpty());
        assertEquals(HttpClient.Version.HTTP_1_1, response.get("version"));
        assertEquals(200, response.get("status"));
        assertTrue(response.get("headers") instanceof Map);
        assertEquals("data", response.get("body"));
    }

    @Test
    void sendException() {
        final SolidClientProvider solidClientProvider = mock(SolidClientProvider.class);
        when(solidClientProvider.getClient()).thenThrow(new NullPointerException("FAIL"));
        final SolidClient solidClient = new SolidClient(solidClientProvider);
        assertThrows(TestHarnessApiException.class,
                () -> solidClient.send("GET", TestUtils.SAMPLE_BASE, "", null, null));
    }

    @Test
    void sendAuthorized() {
        final Client client = mock(Client.class);
        final HttpResponse<String> mockStringResponse = TestUtils.mockStringResponse(200, "data");
        when(client.send(any(), any(), any(), any(), any(), eq(true))).thenReturn(mockStringResponse);
        final SolidClientProvider solidClientProvider = mock(SolidClientProvider.class);
        when(solidClientProvider.getClient()).thenReturn(client);

        final SolidClient solidClient = new SolidClient(solidClientProvider);
        final var response = solidClient.sendAuthorized("GET", TestUtils.SAMPLE_BASE, "", null, null);
        assertFalse(response.isEmpty());
        assertEquals(HttpClient.Version.HTTP_1_1, response.get("version"));
        assertEquals(200, response.get("status"));
        assertTrue(response.get("headers") instanceof Map);
        assertEquals("data", response.get("body"));
    }

    @Test
    void sendAuthorizedException() {
        final SolidClientProvider solidClientProvider = mock(SolidClientProvider.class);
        when(solidClientProvider.getClient()).thenThrow(new NullPointerException("FAIL"));
        final SolidClient solidClient = new SolidClient(solidClientProvider);
        assertThrows(TestHarnessApiException.class,
                () -> solidClient.sendAuthorized("GET", TestUtils.SAMPLE_BASE, "", null, null));
    }
}
