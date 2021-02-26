package org.solid.testharness.http;

import jakarta.ws.rs.core.Link;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class HttpUtils {
    static boolean isSuccessful(int code) {
        return code >= 200 && code < 300;
    }

    public static void logRequest(Logger logger, HttpRequest request) {
        logger.debug("REQUEST {} {}", request.method(), request.uri());
        HttpHeaders headers = request.headers();
        headers.map().forEach((k, v) -> logger.debug("REQ HEADER {}: {}", k, v));
    }

    public static <T> void logResponse(Logger logger, HttpResponse<T> response) {
        logger.debug("RESPONSE {} {}", response.request().method(), response.uri());
        logger.debug("STATUS   {}", response.statusCode());
        HttpHeaders headers = response.headers();
        headers.map().forEach((k, v) -> logger.debug("HEADER   {}: {}", k, v));
        T body = response.body();
        if (body != null) {
            logger.debug("BODY     {}", response.body());
        }
    }

    public static HttpRequest.BodyPublisher ofFormData(Map<Object, Object> data) {
        var builder = new StringBuilder();
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(encodeValue(entry.getKey().toString()));
            builder.append("=");
            builder.append(encodeValue(entry.getValue().toString()));
        }
        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }

    static String encodeValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String decodeValue(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    public static Map<String, List<String>> splitQuery(URI url) {
        if (url == null || url.getQuery() == null || url.getQuery().isEmpty()) {
            return Collections.emptyMap();
        }
        return Arrays.stream(url.getQuery().split("&"))
                .map(HttpUtils::splitQueryParameter)
                .collect(Collectors.groupingBy(AbstractMap.SimpleImmutableEntry::getKey, LinkedHashMap::new, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    public static AbstractMap.SimpleImmutableEntry<String, String> splitQueryParameter(String it) {
        final int idx = it.indexOf("=");
        final String key = idx > 0 ? it.substring(0, idx) : it;
        final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;
        return new AbstractMap.SimpleImmutableEntry<>(
                decodeValue(key),
                decodeValue(value)
        );
    }

    // Link can be multi-valued (comma separated) or multi-instance so this builds a list from either form (or both)
    // TODO: This is temporary as a link can contain a comma so this is not safe - a parser is required
    public static List<Link> parseLinkHeaders(HttpHeaders headers) {
        List<String> links = headers.allValues("Link");
        // TODO: the following must be applied to all link headers, then the whole list flattened
        if (links.size() == 1 && links.get(0).contains(", ")) {
            links = Arrays.asList(links.get(0).split(", "));
        }
        return links.stream().map(link -> Link.valueOf(link)).collect(Collectors.toList());
    }
}
