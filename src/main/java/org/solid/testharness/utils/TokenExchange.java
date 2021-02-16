package org.solid.testharness.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

public class TokenExchange {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.utils.TokenExchange");

    public static final String exchangeToken(String endpoint, Map<String, String> config) throws Exception {
//        logger.debug("Exchange token at {} with {}", endpoint, config);
        String basicAuth = TokenExchange.base64Encode(config.get("clientId") + ':' + config.get("clientSecret"));
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + basicAuth)
                .header("Accept", "application/json")
                .POST(BodyPublishers.ofString("grant_type=refresh_token&refresh_token=" + config.get("refreshToken")))
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//            logger.debug("Response {}", response.body());
            if (response.statusCode() == 200) {
                Map<String, String> json = objectMapper.readValue(response.body(), new TypeReference<>() {
                });
//                logger.debug("JSON {}", json);
                return json.get("access_token");
            } else {
                logger.error("FAILED TO GET TOKEN {}", response.body());
                throw new Exception("Token exchange failed - do you need a new refresh_token");
            }
        } catch (IOException e) {
            logger.error("IOException", e);
        } catch (InterruptedException e) {
            logger.error("InterruptedException", e);
        }

        return null;
    }

    public static final String base64Encode(String data) {
        return new String(Base64.getEncoder().encode(data.getBytes()));
    }
}
