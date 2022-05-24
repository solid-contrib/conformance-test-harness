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

import jakarta.ws.rs.core.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.http.HttpConstants;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class provides a set of static utility functions to tests written in Karate and the methods only use basic the
 * types of String, List, Map as these are automatically translated into the Javascript environment that runs the tests.
 */
public final class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    /**
     * Returns a list of link headers. Each member of the list is a map of key/value pairs representing the different
     * values of the link header.
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc2068#section-19.6.2.4">RFC2068 section 19.6.2.4</a>
     * @param headers a map of the response headers
     * @return list of link headers
     */
    public static List<Map<String, String>> parseLinkHeaders(final Map<String, List<String>> headers) {
        if (headers == null) {
            return Collections.emptyList();
        }
        // Link can be multi-valued (comma separated) or multi-instance so this builds a list from both forms
        // TODO: This is temporary as a link can contain a comma so this is not safe - a parser is required
        // See https://blog.stevenlevithan.com/archives/match-quoted-string
        // See https://gist.github.com/pimbrouwers/8f78e318ccfefff18f518a483997be29
        List<String> links = headers.entrySet()
                .stream()
                .filter(entry -> "link".equals(entry.getKey().toLowerCase(Locale.ROOT)))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(Collections.emptyList());
        if (links.size() == 1 && links.get(0).contains(",")) {
            links = Arrays.asList(links.get(0).split("\\s*,\\s*"));
        }
        return links.stream().map(Link::valueOf).map(l -> {
            final var map = new HashMap<String, String>();
            map.put("rel", l.getRel());
            map.put("uri", l.getUri().toString());
            if (l.getTitle() != null) map.put("title", l.getTitle());
            if (l.getType() != null) map.put("type", l.getType());
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * Returns a map of the permission groups found in a WAC-Allow header. Each group contains a list of permissions.
     * @param headers a map of the response headers
     * @return the map of permissions groups and permissions
     */
    // This method deliberately creates objects in a loop dependent on the structure of the header
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public static Map<String, List<String>> parseWacAllowHeader(@NotNull final Map<String, List<String>> headers) {
        try {
            Objects.requireNonNull(headers, "headers is required");
            logger.debug("WAC-Allow: {}", headers);
            final Map<String, Set<String>> permissions = new HashMap<>();
            permissions.put("user", new HashSet<>());
            permissions.put("public", new HashSet<>());
            final Optional<Map.Entry<String, List<String>>> header = headers.entrySet()
                    .stream()
                    .filter(entry -> HttpConstants.HEADER_WAC_ALLOW.equalsIgnoreCase(entry.getKey()))
                    .findFirst();
            if (header.isPresent()) {
                final String wacAllowHeader = header.get().getValue().get(0);
                // note this does not support imbalanced quotes
                final Pattern p = Pattern.compile(
                        "(\\w+)[ \\t]*+=[ \\t]*+\"[ \\t]*+((?:[ \\t]*+[^\", \\t]+)*+)[ \\t]*+\""
                );
                final Matcher m = p.matcher(wacAllowHeader);
                while (m.find()) {
                    if (!permissions.containsKey(m.group(1))) {
                        permissions.put(m.group(1), new HashSet<>());
                    }
                    if (!m.group(2).isEmpty()) {
                        permissions.get(m.group(1)).addAll(
                                Arrays.asList(m.group(2).toLowerCase(Locale.ROOT).split("[ \\t]++"))
                        );
                    }
                }
            } else {
                logger.error("WAC-Allow header missing");
            }
            return permissions.entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> List.copyOf(entry.getValue())));
        } catch (Exception e) {
            throw new TestHarnessApiException("Failed to parse WAC-Allow header", e);
        }
    }

    /**
     * Return a new URI by resolving the new target against a base URI.
     * @param baseUri the base URI
     * @param target the new target
     * @return the resolved URI
     */
    public static String resolveUri(final String baseUri, final String target) {
        try {
            if (baseUri == null || target == null) return null;
            return URI.create(baseUri).resolve(URI.create(target)).toString();
        } catch (Exception e) {
            throw new TestHarnessApiException("Failed to resolve URI", e);
        }
    }

    private Utils() { }
}
