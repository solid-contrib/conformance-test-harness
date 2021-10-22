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

import com.intuit.karate.core.ScenarioEngine;
import jakarta.ws.rs.core.Link;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.config.Config;

import javax.enterprise.inject.spi.CDI;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class HttpUtils {
    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);
    private static final String RESPONSE_PREFIX = "  < ";
    private static final String REQUEST_PREFIX = "  > ";
    private static Config config;

    private static Pattern AUTH_HEADER = Pattern.compile("^(\\S+)\\s.*([^\"]{6})$");
    private static Pattern DPOP_HEADER = Pattern.compile("^.*([^\"]{6})$");
    private static Pattern BODY_TOKENS = Pattern.compile("_token\"\\s*:\\s*\"[^\"]+([^\"]{6})\"");

    public static String getAgent() {
        return getConfig().getAgent();
    }

    public static Duration getConnectTimeout() {
        return Duration.ofMillis(getConfig().getConnectTimeout() > 0
                ? getConfig().getConnectTimeout()
                : Config.DEFAULT_TIMEOUT);
    }

    public static HttpRequest.Builder newRequestBuilder(final URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(getConfig().getReadTimeout()))
                .setHeader(HttpConstants.USER_AGENT, getAgent());
    }

    public static boolean isSuccessful(final int code) {
        return code >= 200 && code < 300;
    }

    public static boolean isRedirect(final int statusCode) {
        return statusCode == 302;
    }

    public static boolean isSuccessfulOrRedirect(final int code) {
        return code >= 200 && code < 400;
    }

    public static boolean isHttpProtocol(final String protocol) {
        return "http".equals(protocol) || "https".equals(protocol);
    }

    public static boolean isFileProtocol(final String protocol) {
        return "file".equals(protocol);
    }

    public static void logRequest(final Logger logger, final HttpRequest request) {
        if (logger.isDebugEnabled()) {
            logger.debug("request:\n{}", formatRequestLog(request, null));
        }
    }

    public static void logRequestToKarate(final Logger fallbackLogger, final HttpRequest request, final String body) {
        final com.intuit.karate.Logger logger = Optional.ofNullable(ScenarioEngine.get())
                .map(se -> se.logger).orElse(null);
        if (logger != null) {
            logger.debug("request:\n{}", formatRequestLog(request, body));
        } else if (fallbackLogger.isDebugEnabled()) {
            fallbackLogger.debug("request:\n{}", formatRequestLog(request, body));
        }
    }

    private static String formatRequestLog(final HttpRequest request, final String body) {
        final StringBuilder sb = new StringBuilder();
        sb.append(REQUEST_PREFIX).append(request.method()).append(' ').append(request.uri()).append('\n');
        logHeaders(sb, request.headers().map(), true);
        if (body != null) {
            sb.append(maskBody(body));
        }
        return sb.toString();
    }

    public static <T> void logResponse(final Logger logger, final HttpResponse<T> response) {
        if (logger.isDebugEnabled()) {
            logger.debug("response:\n{}", formatResponseLog(response));
        }
    }

    public static <T> void logResponseToKarate(final Logger fallbackLogger, final HttpResponse<T> response) {
        final com.intuit.karate.Logger logger = Optional.ofNullable(ScenarioEngine.get())
                .map(se -> se.logger).orElse(null);
        if (logger != null) {
            logger.debug("response:\n{}", formatResponseLog(response));
        } else if (fallbackLogger.isDebugEnabled()) {
            fallbackLogger.debug("response:\n{}", formatResponseLog(response));
        }
    }

    private static <T> String formatResponseLog(final HttpResponse<T> response) {
        final StringBuilder sb = new StringBuilder();
        sb.append(RESPONSE_PREFIX).append(response.statusCode()).append('\n');
        logHeaders(sb, response.headers().map(), false);
        final T body = response.body();
        if (body != null) {
            sb.append(maskBody((String)body));
        }
        return sb.toString();
    }

    private static void logHeaders(final StringBuilder sb, final Map<String, List<String>> headerMap,
                                   final boolean outgoing) {
        headerMap.forEach((key, values)
                -> values.forEach(value -> sb.append(outgoing ? REQUEST_PREFIX : RESPONSE_PREFIX)
                        .append(key).append(": ").append(maskHeader(key, value)).append('\n'))
        );
    }

    public static String maskHeader(final String header, final String value) {
        if (header.equalsIgnoreCase(HttpConstants.HEADER_AUTHORIZATION)) {
            final Matcher matcher = AUTH_HEADER.matcher(value);
            if (matcher.matches()) {
                return matcher.group(1) + " ***" + matcher.group(2);
            }
        }
        if (header.equalsIgnoreCase(HttpConstants.HEADER_DPOP)) {
            final Matcher matcher = DPOP_HEADER.matcher(value);
            if (matcher.matches()) {
                return "***" + matcher.group(1);
            }
        }
        return value;
    }

    public static String maskBody(final String body) {
        if (!StringUtils.isBlank(body) && body.charAt(0) == '{') {
            final Matcher matcher = BODY_TOKENS.matcher(body);
            return matcher.replaceAll("_token\":\"***$1\"");
        }
        return body;
    }

    public static HttpRequest.BodyPublisher ofFormData(@NotNull final Map<Object, Object> data) {
        Objects.requireNonNull(data, "data is required");
        final var builder = new StringBuilder();
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(encodeValue(entry.getKey().toString()));
            builder.append('=');
            builder.append(encodeValue(entry.getValue().toString()));
        }
        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }

    public static String ensureSlashEnd(final String value) {
        return value != null && !value.endsWith("/") ? value + "/" : value;
    }

    public static String ensureNoSlashEnd(final String value) {
        if (value != null && value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        } else {
            return value;
        }
    }

    public static String encodeValue(@NotNull final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String decodeValue(@NotNull final String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    public static Map<String, List<String>> splitQuery(final URI url) {
        if (url == null || url.getQuery() == null || url.getQuery().isEmpty()) {
            return Collections.emptyMap();
        }
        return Arrays.stream(url.getQuery().split("&"))
                .map(HttpUtils::splitQueryParameter)
                .collect(Collectors.groupingBy(
                        AbstractMap.SimpleImmutableEntry::getKey,
                        LinkedHashMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList()))
                );
    }

    private static AbstractMap.SimpleImmutableEntry<String, String> splitQueryParameter(final String it) {
        final int idx = it.indexOf('=');
        final String key = idx > 0 ? it.substring(0, idx) : it;
        final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;
        return new AbstractMap.SimpleImmutableEntry<>(
                decodeValue(key),
                value != null ? decodeValue(value) : null
        );
    }

    // Link can be multi-valued (comma separated) or multi-instance so this builds a list from either form (or both)
    // TODO: This is temporary as a link can contain a comma so this is not safe - a parser is required
    public static List<Link> parseLinkHeaders(@NotNull final HttpHeaders headers) {
        Objects.requireNonNull(headers, "headers is required");
        List<String> links = headers.allValues(HttpConstants.HEADER_LINK);
        // TODO: the following must be applied to all link headers, then the whole list flattened
        if (links.size() == 1 && links.get(0).contains(", ")) {
            links = Arrays.asList(links.get(0).split(", "));
        }
        return links.stream().map(Link::valueOf).collect(Collectors.toList());
    }

    // This version is used in Karate tests which only work with simple classes and collections
    public static List<Map<String, String>> parseLinkHeaders(final Map<String, List<String>> headers) {
        if (headers == null) {
            return Collections.EMPTY_LIST;
        }
        List<String> links = headers.entrySet()
                .stream()
                .filter(entry -> "link".equals(entry.getKey().toLowerCase(Locale.ROOT)))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(Collections.EMPTY_LIST);
        if (links.size() == 1 && links.get(0).contains(", ")) {
            links = Arrays.asList(links.get(0).split(", "));
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

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    // This method deliberately creates objects in a loop dependent on the structure of the header
    public static Map<String, List<String>> parseWacAllowHeader(@NotNull final Map<String, List<String>> headers) {
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
            final Pattern p = Pattern.compile("(\\w+)\\s*=\\s*\"?\\s*((?:\\s*[^\",\\s]+)*)\\s*\"?");
            final Matcher m = p.matcher(wacAllowHeader);
            while (m.find()) {
                if (!permissions.containsKey(m.group(1))) {
                    permissions.put(m.group(1), new HashSet<>());
                }
                if (!m.group(2).isEmpty()) {
                    permissions.get(m.group(1)).addAll(
                            Arrays.asList(m.group(2).toLowerCase(Locale.ROOT).split("\\s+"))
                    );
                }
            }
        } else {
            logger.error("WAC-Allow header missing");
        }
        return permissions.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> List.copyOf(entry.getValue())));
    }

    public static String resolveUri(@NotNull final String baseUri, @NotNull final String target) {
        if (baseUri == null || target == null) return null;
        return URI.create(baseUri).resolve(URI.create(target)).toString();
    }

    private static Config getConfig() {
        synchronized (HttpUtils.class) {
            if (config == null) {
                config = CDI.current().select(Config.class).get();
            }
            return config;
        }
    }

    private HttpUtils() { }
}
