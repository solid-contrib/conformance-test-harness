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
package org.solid.testharness.api;

import org.junit.jupiter.api.Test;
import org.solid.testharness.http.HttpConstants;

import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class UtilsTest {
    @Test
    void parseLinkHeaders() {
        final List<Map<String, String>> links = Utils.parseLinkHeaders(setupHeaders(HttpConstants.HEADER_LINK,
                List.of("<https://example.org/next>; rel=\"next\"; title=\"title\"; type=\"type\"")).map());
        assertEquals(1, links.size());
        assertEquals("https://example.org/next", links.get(0).get("uri"));
        assertEquals("title", links.get(0).get("title"));
        assertEquals("type", links.get(0).get("type"));
        assertEquals("next", links.get(0).get("rel"));
    }

    @Test
    void parseLinkHeadersTwoInOne() {
        final List<Map<String, String>> links = Utils.parseLinkHeaders(setupHeaders(HttpConstants.HEADER_LINK,
                List.of("<https://example.org/next>; rel=\"next\", <https://example.org/last>; rel=\"last\"")).map());
        assertEquals(2, links.size());
        assertEquals("https://example.org/next", links.get(0).get("uri"));
        assertEquals("next", links.get(0).get("rel"));
        assertEquals("https://example.org/last", links.get(1).get("uri"));
        assertEquals("last", links.get(1).get("rel"));
    }

    @Test
    void parseLinkHeadersTwo() {
        final List<Map<String, String>> links = Utils.parseLinkHeaders(setupHeaders(HttpConstants.HEADER_LINK,
                List.of("<https://example.org/type1>; rel=\"type\"",
                        "<https://example.org/next>; rel=\"next\"; type=\"text/plain\"")).map());
        assertEquals("https://example.org/type1", links.get(0).get("uri"));
        assertEquals("type", links.get(0).get("rel"));
        assertEquals("https://example.org/next", links.get(1).get("uri"));
        assertEquals("next", links.get(1).get("rel"));
        assertEquals("text/plain", links.get(1).get("type"));
    }

    @Test
    void parseLinkHeadersNoLink() {
        assertTrue(Utils.parseLinkHeaders(setupHeaders("NotLink", List.of("something")).map()).isEmpty());
    }

    @Test
    void parseLinkHeadersNull() {
        assertTrue(Utils.parseLinkHeaders(null).isEmpty());
    }

    @Test
    void parseWacAllowHeader() {
        final Map<String, List<String>> header = Utils.parseWacAllowHeader(
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
        final Map<String, List<String>> header = Utils.parseWacAllowHeader(
                Map.of(HttpConstants.HEADER_WAC_ALLOW, List.of("user=\"read\", public="))
        );
        assertEquals(1, header.get("user").size());
        assertEquals("read", header.get("user").get(0));
        assertTrue(header.get("public").isEmpty());
    }

    @Test
    void parseWacAllowHeaderNewGroup() {
        final Map<String, List<String>> header = Utils.parseWacAllowHeader(
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
        final Map<String, List<String>> header = Utils.parseWacAllowHeader(
                Map.of("NotWacAllow", List.of("something"))
        );
        assertTrue(header.get("user").isEmpty());
        assertTrue(header.get("public").isEmpty());
    }

    @Test
    void parseWacAllowHeaderNull() {
        assertThrows(TestHarnessException.class, () -> Utils.parseWacAllowHeader(null));
    }

    @Test
    void resolveUriNulls() {
        assertNull(Utils.resolveUri(null, ""));
        assertNull(Utils.resolveUri("", null));
    }

    @Test
    void resolveUriException() {
        assertThrows(TestHarnessException.class, () -> Utils.resolveUri("~://?", ""));
    }

    @Test
    void resolveUri() {
        assertEquals("https://example.org/new/",
                Utils.resolveUri("https://example.org/path/old/", "/new/"));
        assertEquals("https://example.org/path/new",
                Utils.resolveUri("https://example.org/path/old", "new"));
    }

    private HttpHeaders setupHeaders(final String name, final List<String> values) {
        return HttpHeaders.of(Map.of(name, values), (k, v) -> true);
    }
}
