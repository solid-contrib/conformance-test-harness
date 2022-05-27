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
package org.solid.testharness.http;

import com.intuit.karate.core.ScenarioEngine;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.ws.rs.core.Link;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.slf4j.Logger;
import org.solid.testharness.config.Config;
import org.solid.testharness.utils.TestUtils;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class HttpUtilsTest {
    private static final URI BASE = URI.create(TestUtils.SAMPLE_BASE).resolve("/");
    @InjectMock
    Config config;

    @Test
    void getAgent() {
        when(config.getAgent()).thenReturn("AGENT");
        assertEquals("AGENT", HttpUtils.getAgent());
    }

    @Test
    void getConnectTimeout() {
        when(config.getConnectTimeout()).thenReturn(1111);
        assertEquals(Duration.ofMillis(1111), HttpUtils.getConnectTimeout());
    }

    @Test
    void getDefaultConnectTimeout() {
        when(config.getConnectTimeout()).thenReturn(0);
        assertEquals(Duration.ofMillis(Config.DEFAULT_TIMEOUT), HttpUtils.getConnectTimeout());
    }

    @Test
    void newRequestBuilder() {
        when(config.getAgent()).thenReturn("AGENT");
        when(config.getReadTimeout()).thenReturn(1111);
        final HttpRequest request = HttpUtils.newRequestBuilder(URI.create(TestUtils.SAMPLE_BASE)).build();
        assertEquals("AGENT", request.headers().firstValue(HttpConstants.USER_AGENT).orElse(null));
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
        assertTrue(HttpUtils.isHttpProtocol(URI.create("http://example.org")));
        assertTrue(HttpUtils.isHttpProtocol(URI.create("https://example.org")));
        assertFalse(HttpUtils.isHttpProtocol(URI.create("ws://example.org")));
        assertFalse(HttpUtils.isHttpProtocol(null));
    }

    @Test
    void isFileProtocol() {
        assertTrue(HttpUtils.isFileProtocol(URI.create("file://path")));
        assertFalse(HttpUtils.isFileProtocol(URI.create("files://path")));
        assertFalse(HttpUtils.isFileProtocol(null));
    }

    @Test
    void logToKarate() {
        ScenarioEngine.set(TestUtils.createEmptyScenarioEngine());
        final Logger logger = mock(Logger.class);
        when(logger.isDebugEnabled()).thenReturn(true);
        HttpUtils.logToKarate(logger, "FORMAT {}", "MESSAGE");
        verify(logger, never()).isDebugEnabled();
        verify(logger, never()).debug(anyString(), ArgumentMatchers.<Object>any());
    }

    @Test
    void logToKarateFallback() {
        ScenarioEngine.set(null);
        final Logger logger = mock(Logger.class);
        when(logger.isDebugEnabled()).thenReturn(true);
        HttpUtils.logToKarate(logger, "FORMAT {}", "MESSAGE");
        verify(logger).isDebugEnabled();
        verify(logger).debug(anyString(), ArgumentMatchers.<Object[]>any());
        verifyNoMoreInteractions(logger);
    }

    @Test
    void logToKarateFallbackDisabled() {
        ScenarioEngine.set(null);
        final Logger logger = mock(Logger.class);
        when(logger.isDebugEnabled()).thenReturn(false);
        HttpUtils.logToKarate(logger, "FORMAT {}", "MESSAGE");
        verify(logger, never()).debug(anyString(), ArgumentMatchers.<Object[]>any());
    }

    @Test
    void logRequestDisabled() {
        final Logger logger = mock(Logger.class);
        final HttpRequest request = mock(HttpRequest.class);
        when(logger.isDebugEnabled()).thenReturn(false);
        HttpUtils.logRequest(logger, request);
        verify(logger, never()).debug(anyString(), ArgumentMatchers.<Object[]>any());
    }

    @Test
    void logRequest() {
        final Logger logger = mock(Logger.class);
        when(logger.isDebugEnabled()).thenReturn(true);
        final HttpRequest request = HttpRequest.newBuilder(BASE)
                .header("key", "value")
                .build();
        HttpUtils.logRequest(logger, request);
        verify(logger).isDebugEnabled();
        verify(logger, times(1)).debug(anyString(), anyString());
        verifyNoMoreInteractions(logger);
    }

    @Test
    void logRequestToKarate() {
        ScenarioEngine.set(TestUtils.createEmptyScenarioEngine());
        final Logger logger = mock(Logger.class);
        when(logger.isDebugEnabled()).thenReturn(true);
        final HttpRequest request = HttpRequest.newBuilder(BASE)
                .header("key", "value")
                .build();
        HttpUtils.logRequestToKarate(logger, request, "body");
        verify(logger, never()).isDebugEnabled();
        verify(logger, never()).debug(anyString(), ArgumentMatchers.<Object>any());
    }

    @Test
    void logRequestToKarateFallback() {
        ScenarioEngine.set(null);
        final Logger logger = mock(Logger.class);
        when(logger.isDebugEnabled()).thenReturn(true);
        final HttpRequest request = HttpRequest.newBuilder(BASE)
                .header("key", "value")
                .build();
        HttpUtils.logRequestToKarate(logger, request, "body");
        verify(logger).isDebugEnabled();
        verify(logger, times(1)).debug(anyString(), ArgumentMatchers.<Object[]>any());
        verifyNoMoreInteractions(logger);
    }

    @Test
    void logRequestToKarateFallbackDisabled() {
        ScenarioEngine.set(null);
        final Logger logger = mock(Logger.class);
        final HttpRequest request = HttpRequest.newBuilder(BASE)
                .header("key", "value")
                .build();
        when(logger.isDebugEnabled()).thenReturn(false);
        HttpUtils.logRequestToKarate(logger, request, "body");
        verify(logger, never()).debug(anyString(), ArgumentMatchers.<Object[]>any());
    }

    @Test
    void logResponseDisabled() {
        final Logger logger = mock(Logger.class);
        final HttpResponse<String> response = mock(HttpResponse.class);
        when(logger.isDebugEnabled()).thenReturn(false);
        HttpUtils.logResponse(logger, response);
        verify(logger, never()).debug(anyString(), ArgumentMatchers.<Object>any());
    }

    @Test
    void logResponse() {
        final Logger logger = mock(Logger.class);
        final HttpRequest request = HttpRequest.newBuilder(BASE).build();
        final HttpResponse<String> response = mock(HttpResponse.class);
        when(logger.isDebugEnabled()).thenReturn(true);
        when(response.request()).thenReturn(request);
        when(response.uri()).thenReturn(BASE);
        when(response.statusCode()).thenReturn(400);
        when(response.headers()).thenReturn(setupHeaders("key", List.of("value")));
        when(response.body()).thenReturn("BODY");
        HttpUtils.logResponse(logger, response);
        verify(logger).isDebugEnabled();
        verify(logger, times(1)).debug(anyString(), anyString());
        verifyNoMoreInteractions(logger);
    }


    @Test
    void logResponseToKarate() {
        ScenarioEngine.set(TestUtils.createEmptyScenarioEngine());
        final Logger logger = mock(Logger.class);
        final HttpRequest request = HttpRequest.newBuilder(BASE).build();
        final HttpResponse<String> response = mock(HttpResponse.class);
        when(logger.isDebugEnabled()).thenReturn(true);
        when(response.request()).thenReturn(request);
        when(response.uri()).thenReturn(BASE);
        when(response.statusCode()).thenReturn(400);
        when(response.headers()).thenReturn(setupHeaders("key", List.of("value")));
        when(response.body()).thenReturn("BODY");
        HttpUtils.logResponseToKarate(logger, response);
        verify(logger, never()).isDebugEnabled();
        verify(logger, never()).debug(anyString(), ArgumentMatchers.<Object>any());
    }

    @Test
    void logResponseToKarateFallback() {
        ScenarioEngine.set(null);
        final Logger logger = mock(Logger.class);
        final HttpRequest request = HttpRequest.newBuilder(BASE).build();
        final HttpResponse<String> response = mock(HttpResponse.class);
        when(logger.isDebugEnabled()).thenReturn(true);
        when(response.request()).thenReturn(request);
        when(response.uri()).thenReturn(BASE);
        when(response.statusCode()).thenReturn(400);
        when(response.headers()).thenReturn(setupHeaders("key", List.of("value")));
        when(response.body()).thenReturn("BODY");
        HttpUtils.logResponseToKarate(logger, response);
        verify(logger).isDebugEnabled();
        verify(logger, times(1)).debug(anyString(), ArgumentMatchers.<Object[]>any());
        verifyNoMoreInteractions(logger);
    }

    @Test
    void logResponseToKarateFallbackDisabled() {
        ScenarioEngine.set(null);
        final Logger logger = mock(Logger.class);
        final HttpRequest request = HttpRequest.newBuilder(BASE).build();
        final HttpResponse<String> response = mock(HttpResponse.class);
        when(logger.isDebugEnabled()).thenReturn(false);
        when(response.request()).thenReturn(request);
        when(response.uri()).thenReturn(BASE);
        when(response.statusCode()).thenReturn(400);
        when(response.headers()).thenReturn(setupHeaders("key", List.of("value")));
        when(response.body()).thenReturn(null);
        HttpUtils.logResponseToKarate(logger, response);
        verify(logger, never()).debug(anyString(), ArgumentMatchers.<Object[]>any());
    }

    @Test
    void logResponseVoid() {
        final Logger logger = mock(Logger.class);
        final HttpRequest request = HttpRequest.newBuilder(BASE).build();
        final HttpResponse<Void> response = mock(HttpResponse.class);
        when(logger.isDebugEnabled()).thenReturn(true);
        when(response.request()).thenReturn(request);
        when(response.uri()).thenReturn(BASE);
        when(response.statusCode()).thenReturn(400);
        when(response.headers()).thenReturn(setupHeaders("key", List.of("value")));
        when(response.body()).thenReturn(null);
        HttpUtils.logResponse(logger, response);
        verify(logger).isDebugEnabled();
        verify(logger, times(1)).debug(anyString(), anyString());
        verifyNoMoreInteractions(logger);
    }

    @Test
    void maskHeaderAuth() {
        assertEquals("Bearer ***abcdef", HttpUtils.maskHeader("AUTHORIZATION", "Bearer xxxxxabcdef"));
    }

    @Test
    void maskHeaderAuthNoMatch() {
        assertEquals("abcdef", HttpUtils.maskHeader("AUTHORIZATION", "abcdef"));
    }

    @Test
    void maskHeaderDpop() {
        assertEquals("***abcdef", HttpUtils.maskHeader("DPOP", "xxxxxabcdef"));
    }

    @Test
    void maskHeaderDpopNoMatch() {
        assertEquals("abcde", HttpUtils.maskHeader("DPOP", "abcde"));
    }

    @Test
    void maskHeaderOther() {
        assertEquals("Other", HttpUtils.maskHeader("OTHER", "Other"));
    }

    @Test
    void maskBodyTokens() {
        assertEquals("{\"access_token\":\"***abcdef\",\"id_token\":\"***ghijkl\"}",
                HttpUtils.maskBody("{\"access_token\":\"xxxxxabcdef\",\"id_token\":\"xxxxxghijkl\"}"));
    }

    @Test
    void maskBodyText() {
        assertEquals("Other", HttpUtils.maskBody("Other"));
    }

    @Test
    void maskBodyBlank() {
        assertEquals("", HttpUtils.maskBody(""));
    }

    @Test
    void maskBodyJson() {
        assertEquals("{\"key\":\"value\"}", HttpUtils.maskBody("{\"key\":\"value\"}"));
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
    void ensureSlashEnd() {
        assertNull(HttpUtils.ensureSlashEnd(null));
        assertEquals("/", HttpUtils.ensureSlashEnd(""));
        assertEquals("/", HttpUtils.ensureSlashEnd("/"));
        assertEquals("/", HttpUtils.ensureSlashEnd("//"));
        assertEquals("abc/", HttpUtils.ensureSlashEnd("abc"));
        assertEquals("abc/", HttpUtils.ensureSlashEnd("abc/"));
        assertEquals("abc/", HttpUtils.ensureSlashEnd("abc//"));
    }

    @Test
    void ensureNoSlashEnd() {
        assertNull(HttpUtils.ensureNoSlashEnd(null));
        assertEquals("", HttpUtils.ensureNoSlashEnd(""));
        assertEquals("", HttpUtils.ensureNoSlashEnd("/"));
        assertEquals("", HttpUtils.ensureNoSlashEnd("//"));
        assertEquals("abc", HttpUtils.ensureNoSlashEnd("abc"));
        assertEquals("abc", HttpUtils.ensureNoSlashEnd("abc/"));
        assertEquals("abc", HttpUtils.ensureNoSlashEnd("abc//"));
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
        assertTrue(HttpUtils.splitQuery(BASE).isEmpty());
    }

    @Test
    void splitQueryEmptyQuery() {
        assertTrue(HttpUtils.splitQuery(BASE.resolve("?")).isEmpty());
    }

    @Test
    void splitQuerySingle() {
        final Map<String, List<String>> parts = HttpUtils.splitQuery(BASE.resolve("?a=1"));
        assertFalse(parts.isEmpty());
        assertEquals(1, parts.keySet().size());
        assertTrue(parts.containsKey("a"));
        assertEquals(1, parts.get("a").size());
        assertEquals("1", parts.get("a").get(0));
    }

    @Test
    void splitQuerySingleList() {
        final Map<String, List<String>> parts = HttpUtils.splitQuery(BASE.resolve("?a=1&a=2"));
        assertFalse(parts.isEmpty());
        assertEquals(1, parts.keySet().size());
        assertTrue(parts.containsKey("a"));
        assertEquals(2, parts.get("a").size());
        assertEquals("1", parts.get("a").get(0));
        assertEquals("2", parts.get("a").get(1));
    }

    @Test
    void splitQueryMultiple() {
        final Map<String, List<String>> parts = HttpUtils.splitQuery(BASE.resolve("?a=1&b=2"));
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
        final Map<String, List<String>> parts = HttpUtils.splitQuery(BASE.resolve("?a="));
        assertFalse(parts.isEmpty());
        assertEquals(1, parts.keySet().size());
        assertTrue(parts.containsKey("a"));
        assertEquals(1, parts.get("a").size());
        assertNull(parts.get("a").get(0));
    }

    @Test
    void splitQueryKeyOnly() {
        final Map<String, List<String>> parts = HttpUtils.splitQuery(BASE.resolve("?a"));
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
                List.of("<https://example.org/next>; rel=\"next\"  , <https://example.org/last>; rel=\"last\"")));
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

    private HttpHeaders setupHeaders(final String name, final List<String> values) {
        return HttpHeaders.of(Map.of(name, values), (k, v) -> true);
    }
}
