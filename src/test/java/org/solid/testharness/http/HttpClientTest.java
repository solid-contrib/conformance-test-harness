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

import jakarta.ws.rs.core.Link;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// TODO: Tests not implemented during POC phase but will be going forwards

@Disabled
class HttpClientTest {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientTest.class);

    @Test
    void linkHeaders() throws IOException, InterruptedException {
        final HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpUtils.newRequestBuilder(
                URI.create("https://solid-test-suite-alice.inrupt.net")
        ).build();
        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        List<String> links = response.headers().allValues(HttpConstants.HEADER_LINK);
        logger.debug("NSS links {}: {}", links.size(), links);

        request = HttpUtils.newRequestBuilder(
                URI.create("https://pod-compat.inrupt.com/solid-test-suite-alice/profile/card#me")
        ).build();
        response = client.send(request, HttpResponse.BodyHandlers.discarding());
        links = response.headers().allValues(HttpConstants.HEADER_LINK);
        logger.debug("ESS links {}: {}", links.size(), links);
    }

    @Test
    void parseNoLinkHeaders() throws IOException, InterruptedException {
        final HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        final HttpRequest request = HttpUtils.newRequestBuilder(URI.create("https://pod-compat.inrupt.com/")).build();
        final HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        final List<Link> links = HttpUtils.parseLinkHeaders(response.headers());
        assertNotNull(links);
        assertEquals(0, links.size());
    }

    @Test
    void parseMultiValueLinkHeaders() throws IOException, InterruptedException {
        final HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        final HttpRequest request = HttpUtils.newRequestBuilder(
                URI.create("https://solid-test-suite-alice.inrupt.net")
        ).build();
        final HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        final List<Link> links = HttpUtils.parseLinkHeaders(response.headers());
        assertNotNull(links);
        assertTrue(links.size() > 1);
        // TODO: test that no link contains multiple links
    }

    @Test
    void parseMultipleLinkHeaders() throws IOException, InterruptedException {
        final HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        final HttpRequest request = HttpUtils.newRequestBuilder(
                URI.create("https://pod-compat.inrupt.com/solid-test-suite-alice/profile/card#me")
        ).build();
        final HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        final List<Link> links = HttpUtils.parseLinkHeaders(response.headers());
        assertNotNull(links);
        assertTrue(links.size() > 1);
        // TODO: test that no link contains multiple links
    }
}
