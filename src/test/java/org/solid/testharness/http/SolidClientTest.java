package org.solid.testharness.http;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

// TODO: Tests not implemented during POC phase but will be going forwards

class SolidClientTest {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.http.SolidClientTest");

    @Test
    @Disabled
    void checkLogin() {
        SolidClient solidClient = new SolidClient("alice");
        HttpClient client = solidClient.getHttpClient();
        URI uri = URI.create("https://server/login");
        Map<Object, Object> data = new HashMap<>();
        data.put("username", "alice");
        data.put("password", "alice123");
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(ofFormData(data))
                    .uri(uri);
            HttpRequest request = builder.build();
            logger.debug("REQUEST {}", request);
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            logResponse(response);
            String cookie = response.headers().firstValue("set-cookie").get();

            builder = HttpRequest.newBuilder()
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(ofFormData(data))
                    .uri(uri);

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void logResponse(HttpResponse response) {
        logger.debug("REQUEST {} {}", response.request().method(), response.uri());
        logger.debug("STATUS  {}", response.statusCode());
        HttpHeaders headers = response.headers();
        headers.map().forEach((k, v) -> logger.debug("HEADER  {}: {}", k, v));
    }

    public static HttpRequest.BodyPublisher ofFormData(Map<Object, Object> data) {
        var builder = new StringBuilder();
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }
        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }

    @Test
    @Disabled
    void checkGetHeaders() {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        URI uri = URI.create("https://httpbin.org/headers");
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .GET()
                    .uri(uri)
                    .header("Accept", "application/json");
            HttpRequest request = builder.build();
            logger.debug("REQUEST {}", request);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.debug("RESPONSE {}", response.body());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Test
    @Disabled
    void getWithoutContent() {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        URI uri = URI.create("http://localhost:3000/private/022c4325-42c3-4b65-9601-8241ec4e97ed/");
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .GET()
                    .uri(uri)
                    .header("Content-Type", "text/plain")
                    .header("Accept", "text/turtle");
            HttpRequest request = builder.build();
            logger.debug("HEADERS {}", request.headers());
            logger.debug("REQUEST {}", request);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.debug("RESPONSE {}", response.body());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }
}