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
import jakarta.ws.rs.core.Link;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class HttpUtilsTest {
    @Test
    void getAgent() {
        assertEquals("AGENT", HttpUtils.getAgent());
    }

    @Test
    void newRequestBuilder() {
        final HttpRequest request = HttpUtils.newRequestBuilder(URI.create("http://example.org")).build();
        assertEquals("AGENT", request.headers().firstValue(HttpConstants.USER_AGENT).get());
    }

    @Test
    void isSuccessful() {
        assertTrue(HttpUtils.isSuccessful(200));
        assertTrue(HttpUtils.isSuccessful(299));
        assertFalse(HttpUtils.isSuccessful(199));
        assertFalse(HttpUtils.isSuccessful(300));
    }

    @Test
    void isRedirect() {
        assertTrue(HttpUtils.isRedirect(302));
        assertFalse(HttpUtils.isRedirect(200));
    }

    @Test
    void isSuccessfulOrRedirect() {
        assertTrue(HttpUtils.isSuccessfulOrRedirect(200));
        assertTrue(HttpUtils.isSuccessfulOrRedirect(399));
        assertFalse(HttpUtils.isSuccessfulOrRedirect(199));
        assertFalse(HttpUtils.isSuccessfulOrRedirect(400));
    }

    @Test
    void isHttpProtocol() {
        assertTrue(HttpUtils.isHttpProtocol("http"));
        assertTrue(HttpUtils.isHttpProtocol("https"));
        assertFalse(HttpUtils.isHttpProtocol("ws"));
        assertFalse(HttpUtils.isHttpProtocol(null));
    }

    @Test
    void isFileProtocol() {
        assertTrue(HttpUtils.isFileProtocol("file"));
        assertFalse(HttpUtils.isFileProtocol("files"));
        assertFalse(HttpUtils.isFileProtocol(null));
    }

    @Test
    void logRequestDisabled() {
        final Logger logger = mock(Logger.class);
        final HttpRequest request = mock(HttpRequest.class);
        when(logger.isDebugEnabled()).thenReturn(false);
        HttpUtils.logRequest(logger, request);
        verify(logger, never()).debug(anyString(), ArgumentMatchers.<Object>any());
    }

    @Test
    void logRequest() {
        final Logger logger = mock(Logger.class);
        when(logger.isDebugEnabled()).thenReturn(true);
        final HttpRequest request = HttpRequest.newBuilder(URI.create("https://example.org/"))
                .header("key", "value")
                .build();
        HttpUtils.logRequest(logger, request);
        verify(logger).isDebugEnabled();
        verify(logger, times(2)).debug(anyString(), anyString(), any());
        verifyNoMoreInteractions(logger);
    }

    @Test
    void logResponsetDisabled() {
        final Logger logger = mock(Logger.class);
        final HttpResponse<String> response = mock(HttpResponse.class);
        when(logger.isDebugEnabled()).thenReturn(false);
        HttpUtils.logResponse(logger, response);
        verify(logger, never()).debug(anyString(), ArgumentMatchers.<Object>any());
    }

    @Test
    void logResponse() {
        final Logger logger = mock(Logger.class);
        final HttpRequest request = HttpRequest.newBuilder(URI.create("https://example.org/")).build();
        final HttpResponse<String> response = mock(HttpResponse.class);
        when(logger.isDebugEnabled()).thenReturn(true);
        when(response.request()).thenReturn(request);
        when(response.uri()).thenReturn(URI.create("https://example.org/"));
        when(response.statusCode()).thenReturn(400);
        when(response.headers()).thenReturn(setupHeaders("key", List.of("value")));
        when(response.body()).thenReturn("BODY");
        HttpUtils.logResponse(logger, response);
        verify(logger).isDebugEnabled();
        verify(logger, times(2)).debug(anyString(), anyString(), any());
        verify(logger, times(1)).debug(anyString(), eq(400));
        verify(logger, times(1)).debug(anyString(), eq("BODY"));
        verifyNoMoreInteractions(logger);
    }

    @Test
    void logResponseVoid() {
        final Logger logger = mock(Logger.class);
        final HttpRequest request = HttpRequest.newBuilder(URI.create("https://example.org/")).build();
        final HttpResponse<Void> response = mock(HttpResponse.class);
        when(logger.isDebugEnabled()).thenReturn(true);
        when(response.request()).thenReturn(request);
        when(response.uri()).thenReturn(URI.create("https://example.org/"));
        when(response.statusCode()).thenReturn(400);
        when(response.headers()).thenReturn(setupHeaders("key", List.of("value")));
        when(response.body()).thenReturn(null);
        HttpUtils.logResponse(logger, response);
        verify(logger).isDebugEnabled();
        verify(logger, times(2)).debug(anyString(), anyString(), any());
        verify(logger, times(1)).debug(anyString(), eq(400));
        verifyNoMoreInteractions(logger);
    }

    @Test
    void ofFormDataSingle() {
        assertEquals(4, HttpUtils.ofFormData(Map.of("ab", 1)).contentLength());
    }

    @Test
    void ofFormDataMulti() {
        assertEquals(9, HttpUtils.ofFormData(Map.of("ab", 1, "cd", "2")).contentLength());
    }

    @Test
    void ofFormDataEmpty() {
        assertEquals(0, HttpUtils.ofFormData(Collections.emptyMap()).contentLength());
    }

    @Test
    void ofFormDataNull() {
        assertThrows(NullPointerException.class, () -> HttpUtils.ofFormData(null));
    }

    @Test
    void encodeValue() {
        assertEquals("a+b", HttpUtils.encodeValue("a b"));
    }

    @Test
    void encodeValueNull() {
        assertThrows(NullPointerException.class, () -> HttpUtils.encodeValue(null));
    }

    @Test
    void splitQueryNull() {
        assertTrue(HttpUtils.splitQuery(null).isEmpty());
    }

    @Test
    void splitQueryNoQuery() {
        assertTrue(HttpUtils.splitQuery(URI.create("https://example.org/")).isEmpty());
    }

    @Test
    void splitQueryEmptyQuery() {
        assertTrue(HttpUtils.splitQuery(URI.create("https://example.org/?")).isEmpty());
    }

    @Test
    void splitQuerySingle() {
        final Map<String, List<String>> parts = HttpUtils.splitQuery(URI.create("https://example.org/?a=1"));
        assertFalse(parts.isEmpty());
        assertEquals(1, parts.keySet().size());
        assertTrue(parts.containsKey("a"));
        assertEquals(1, parts.get("a").size());
        assertEquals("1", parts.get("a").get(0));
    }

    @Test
    void splitQuerySingleList() {
        final Map<String, List<String>> parts = HttpUtils.splitQuery(URI.create("https://example.org/?a=1&a=2"));
        assertFalse(parts.isEmpty());
        assertEquals(1, parts.keySet().size());
        assertTrue(parts.containsKey("a"));
        assertEquals(2, parts.get("a").size());
        assertEquals("1", parts.get("a").get(0));
        assertEquals("2", parts.get("a").get(1));
    }

    @Test
    void splitQueryMultiple() {
        final Map<String, List<String>> parts = HttpUtils.splitQuery(URI.create("https://example.org/?a=1&b=2"));
        assertFalse(parts.isEmpty());
        assertEquals(2, parts.keySet().size());
        assertTrue(parts.containsKey("a"));
        assertEquals(1, parts.get("a").size());
        assertEquals("1", parts.get("a").get(0));
        assertTrue(parts.containsKey("b"));
        assertEquals(1, parts.get("b").size());
        assertEquals("2", parts.get("b").get(0));
    }

    @Test
    void splitQueryKeyNoValue() {
        final Map<String, List<String>> parts = HttpUtils.splitQuery(URI.create("https://example.org/?a="));
        assertFalse(parts.isEmpty());
        assertEquals(1, parts.keySet().size());
        assertTrue(parts.containsKey("a"));
        assertEquals(1, parts.get("a").size());
        assertNull(parts.get("a").get(0));
    }

    @Test
    void splitQueryKeyOnly() {
        final Map<String, List<String>> parts = HttpUtils.splitQuery(URI.create("https://example.org/?a"));
        assertFalse(parts.isEmpty());
        assertEquals(1, parts.keySet().size());
        assertTrue(parts.containsKey("a"));
        assertEquals(1, parts.get("a").size());
        assertNull(parts.get("a").get(0));
    }

    @Test
    void parseLinkHeaders() {
        final List<Link> links = HttpUtils.parseLinkHeaders(setupHeaders(HttpConstants.HEADER_LINK,
                List.of("<https://example.org/next>; rel=\"next\"")));
        assertEquals(1, links.size());
        assertEquals(URI.create("https://example.org/next"), links.get(0).getUri());
        assertEquals("next", links.get(0).getRel());
    }

    @Test
    void parseLinkHeadersTwoInOne() {
        final List<Link> links = HttpUtils.parseLinkHeaders(setupHeaders(HttpConstants.HEADER_LINK,
                List.of("<https://example.org/next>; rel=\"next\", <https://example.org/last>; rel=\"last\"")));
        assertEquals(2, links.size());
        assertEquals(URI.create("https://example.org/next"), links.get(0).getUri());
        assertEquals("next", links.get(0).getRel());
        assertEquals(URI.create("https://example.org/last"), links.get(1).getUri());
        assertEquals("last", links.get(1).getRel());
    }

    @Test
    void parseLinkHeadersTwo() {
        final List<Link> links = HttpUtils.parseLinkHeaders(setupHeaders(HttpConstants.HEADER_LINK,
                List.of("<https://example.org/next>; rel=\"next\"", "<https://example.org/last>; rel=\"last\"")));
        assertEquals(URI.create("https://example.org/next"), links.get(0).getUri());
        assertEquals("next", links.get(0).getRel());
        assertEquals(URI.create("https://example.org/last"), links.get(1).getUri());
        assertEquals("last", links.get(1).getRel());
    }

    @Test
    void parseLinkHeadersNoLink() {
        assertTrue(HttpUtils.parseLinkHeaders(setupHeaders("NotLink", List.of("something"))).isEmpty());
    }

    @Test
    void parseLinkHeadersNull() {
        assertThrows(NullPointerException.class, () -> HttpUtils.parseLinkHeaders(null));
    }

    @Test
    void parseWacAllowHeader() {
        final Map<String, List<String>> header = HttpUtils.parseWacAllowHeader(
                Map.of(HttpConstants.HEADER_WAC_ALLOW, List.of("user=\"read write\", public=\"read\""))
        );
        assertEquals(2, header.get("user").size());
        assertEquals("read", header.get("user").get(0));
        assertEquals("write", header.get("user").get(1));
        assertEquals(1, header.get("public").size());
        assertEquals("read", header.get("public").get(0));
    }

    @Test
    void parseWacAllowHeaderEmptyGroup() {
        final Map<String, List<String>> header = HttpUtils.parseWacAllowHeader(
                Map.of(HttpConstants.HEADER_WAC_ALLOW, List.of("user=\"read\", public="))
        );
        assertEquals(1, header.get("user").size());
        assertEquals("read", header.get("user").get(0));
        assertTrue(header.get("public").isEmpty());
    }

    @Test
    void parseWacAllowHeaderNewGroup() {
        final Map<String, List<String>> header = HttpUtils.parseWacAllowHeader(
                Map.of(HttpConstants.HEADER_WAC_ALLOW, List.of("user=\"read\", internal=\"append\""))
        );
        assertEquals(1, header.get("user").size());
        assertEquals("read", header.get("user").get(0));
        assertTrue(header.get("public").isEmpty());
        assertEquals(1, header.get("internal").size());
        assertEquals("append", header.get("internal").get(0));
    }

    @Test
    void parseWacAllowHeaderMissing() {
        final Map<String, List<String>> header = HttpUtils.parseWacAllowHeader(
                Map.of("NotWacAllow", List.of("something"))
        );
        assertTrue(header.get("user").isEmpty());
        assertTrue(header.get("public").isEmpty());
    }

    @Test
    void parseWacAllowHeaderNull() {
        assertThrows(NullPointerException.class, () -> HttpUtils.parseWacAllowHeader(null));
    }

    private HttpHeaders setupHeaders(final String name, final List<String> values) {
        return HttpHeaders.of(Map.of(name, values), (k, v) -> true);
    }
}
