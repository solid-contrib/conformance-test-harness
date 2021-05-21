package org.solid.testharness.http;

import jakarta.ws.rs.core.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class HttpUtils {
    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    public static String getAgent() {
        return System.getProperty("agent", "Solid-Conformance-Test-Suite");
    }

    public static HttpRequest.Builder newRequestBuilder(final URI uri) {
        return HttpRequest.newBuilder(uri).setHeader(HttpConstants.USER_AGENT, getAgent());
    }

    static boolean isSuccessful(final int code) {
        return code >= 200 && code < 300;
    }

    static boolean isSuccessfulOrRedirect(final int code) {
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
            logger.debug("REQUEST {} {}", request.method(), request.uri());
            request.headers().map().forEach((k, v) -> logger.debug("REQ HEADER {}: {}", k, v));
        }
    }

    public static <T> void logResponse(final Logger logger, final HttpResponse<T> response) {
        if (logger.isDebugEnabled()) {
            logger.debug("RESPONSE {} {}", response.request().method(), response.uri());
            logger.debug("STATUS   {}", response.statusCode());
            response.headers().map().forEach((k, v) -> logger.debug("HEADER   {}: {}", k, v));
            final T body = response.body();
            if (body != null) {
                logger.debug("BODY     {}", body);
            }
        }
    }

    public static HttpRequest.BodyPublisher ofFormData(final Map<Object, Object> data) {
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

    static String encodeValue(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String decodeValue(final String value) {
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

    public static AbstractMap.SimpleImmutableEntry<String, String> splitQueryParameter(final String it) {
        final int idx = it.indexOf('=');
        final String key = idx > 0 ? it.substring(0, idx) : it;
        final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;
        return new AbstractMap.SimpleImmutableEntry<>(
                decodeValue(key),
                decodeValue(value)
        );
    }

    // Link can be multi-valued (comma separated) or multi-instance so this builds a list from either form (or both)
    // TODO: This is temporary as a link can contain a comma so this is not safe - a parser is required
    public static List<Link> parseLinkHeaders(final HttpHeaders headers) {
        List<String> links = headers.allValues("Link");
        // TODO: the following must be applied to all link headers, then the whole list flattened
        if (links.size() == 1 && links.get(0).contains(", ")) {
            links = Arrays.asList(links.get(0).split(", "));
        }
        return links.stream().map(link -> Link.valueOf(link)).collect(Collectors.toList());
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    // This method deliberately creates objects in a loop dependent on the structure of the header
    public static Map<String, List<String>> parseWacAllowHeader(final Map<String, List<String>> headers) {
        logger.debug("WAC-Allow: {}", headers.toString());
        final Map<String, Set<String>> permissions = Map.of(
                "user", new HashSet<String>(),
                "public", new HashSet<String>()
        );
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
                entry -> entry.getKey(),
                entry -> List.copyOf(entry.getValue())));
    }

    private HttpUtils() { }
}
