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

import org.solid.testharness.http.SolidClientProvider;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * SolidClient is an adapter designed to be used from tests written in Karate and the methods only use basic the types
 * of String, List, Map as these are automatically translated into the Javascript environment that runs the tests.
 * Internally it maps calls through to a <a href="#SolidClientProvider">SolidClientProvider</a> object.
 */
public class SolidClient {
    protected SolidClientProvider solidClientProvider;

    /**
     * Construct a new SolidClient adapter for a <code>SolidClientProvider</code>.
     * @param solidClientProvider the <code>SolidClientProvider</code> to be adapted
     */
    public SolidClient(final SolidClientProvider solidClientProvider) {
        this.solidClientProvider = solidClientProvider;
    }

    /**
     * Returns the headers needed to authorise a request to a Solid server.
     * @param method the HTTP method
     * @param uri the URL of the request
     * @return the headers to be added to the HTTP request
     */
    public Map<String, String> getAuthHeaders(final String method, final String uri) {
        try {
            return solidClientProvider.getClient().getAuthHeaders(method, URI.create(uri));
        } catch (RuntimeException e) {
            throw new TestHarnessApiException("Failed to prepare auth headers", e);
        }
    }

    /**
     * Send a request to the server using the method, data and headers provided.
     * @param method the method
     * @param uri the URL of the resource
     * @param data the request body (optional)
     * @param headers a map of request headers (optional)
     * @param version the HTTP version to use for the request (optional)
     * @return a map containing the response using the keys: status, headers, body
     */
    public Map<String, Object> send(final String method, final String uri, final String data,
                                    final Map<String, Object> headers, final String version) {
        try {
            final HttpResponse<String> response = solidClientProvider.getClient().send(method, URI.create(uri),
                    data, headers, version, false);
            return Map.of(
                    "version", response.version(),
                    "status", response.statusCode(),
                    "body", response.body(),
                    "headers", response.headers().map()
            );
        } catch (RuntimeException e) {
            throw new TestHarnessApiException("Failed to send request", e);
        }
    }

    /**
     * Send an authorized request to the server using the method, data and headers provided.
     * @param method the method
     * @param uri the URL of the resource
     * @param data the request body (optional)
     * @param headers a map of request headers (optional)
     * @param version the HTTP version to use for the request (optional)
     * @return a map containing the response using the keys: status, headers, body
     */
    public Map<String, Object> sendAuthorized(final String method, final String uri, final String data,
                                    final Map<String, Object> headers, final String version) {
        try {
            final HttpResponse<String> response = solidClientProvider.getClient().send(method, URI.create(uri),
                    data, headers, version, true);
            return Map.of(
                    "version", response.version(),
                    "status", response.statusCode(),
                    "body", response.body(),
                    "headers", response.headers().map()
            );
        } catch (RuntimeException e) {
            throw new TestHarnessApiException("Failed to send authorized request", e);
        }
    }
}
