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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;

public class AuthenticationResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationResource.class);

    private WireMockServer wireMockServer;

    private static final String CLIENT_REGISTRATION = "{\"client_id\": \"CLIENTID\"," +
            "\"client_secret\": \"CLIENTSECRET\"}";

    private static final String ACCESS_TOKEN = "{\"access_token\": \"ACCESS_TOKEN\"}";

    private static final String GOOD_BASIC_AUTH = HttpConstants.PREFIX_BASIC +
            Base64.getEncoder().encodeToString("CLIENTID:CLIENTSECRET".getBytes());

    @Override
    public Map<String, String> start() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        // logging the requests helps when debugging tests
        wireMockServer.addMockServiceRequestListener(AuthenticationResource::requestReceived);

        wireMockServer.start();

        // user registration

        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/400/idp/register"))
                .willReturn(WireMock.aResponse().withStatus(400)));

        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/idp/register"))
                .willReturn(WireMock.aResponse().withStatus(200)));

        // return OIDC configuration
        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/" + HttpConstants.OPENID_CONFIGURATION))
                .willReturn(WireMock.aResponse()
                        .withHeader(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.MEDIA_TYPE_APPLICATION_JSON)
                        .withBody(getDiscoveryDocument(wireMockServer.baseUrl()))));

        // refresh token succeeds with good authentication
        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/token"))
                .withHeader(HttpConstants.HEADER_AUTHORIZATION, equalTo(GOOD_BASIC_AUTH))
                .withRequestBody(containing(HttpConstants.REFRESH_TOKEN))
                .willReturn(WireMock.aResponse()
                        .withHeader(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.MEDIA_TYPE_APPLICATION_JSON)
                        .withBody(ACCESS_TOKEN)));

        // session login succeeds with good password
        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/login/password"))
                .withRequestBody(containing(HttpConstants.PASSWORD + "=PASSWORD"))
                .willReturn(WireMock.aResponse()));

        // register succeeds
        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/register"))
                .withRequestBody(containing("\"redirect_uris\":[\"https://origin"))
                .willReturn(WireMock.aResponse()
                        .withHeader(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.MEDIA_TYPE_APPLICATION_JSON)
                        .withBody(CLIENT_REGISTRATION)));

        // authorization will get immediate response for login with a good form
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/authorization"))
                .withQueryParam(HttpConstants.REDIRECT_URI, equalTo("https://origin/form"))
                .withQueryParam(HttpConstants.CLIENT_ID, equalTo("CLIENTID"))
                .willReturn(WireMock.aResponse()
                        .withHeader(HttpConstants.HEADER_LOCATION, "/idp/login")
                        .withStatus(302)));
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/idp/login"))
                .willReturn(WireMock.aResponse()
                        .withHeader(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.MEDIA_TYPE_TEXT_HTML)
                        .withBody("<form method=\"post\"")));
        // idp login
        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/idp/login"))
                .withRequestBody(containing(HttpConstants.PASSWORD + "=PASSWORD"))
                .willReturn(WireMock.aResponse()
                        .withHeader(HttpConstants.HEADER_LOCATION, "https://origin/form?code=authorized")
                        .withStatus(302)));

        // authorization will get immediate response with a good authorization code
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/authorization"))
                .withQueryParam(HttpConstants.REDIRECT_URI, equalTo("https://origin/goodcode"))
                .withQueryParam(HttpConstants.CLIENT_ID, equalTo("CLIENTID"))
                .willReturn(WireMock.aResponse()
                        .withHeader(HttpConstants.HEADER_LOCATION, "https://origin/goodcode?code=authorized")
                        .withStatus(302)));

        // authorization code token request succeeds
        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/token"))
                .withHeader(HttpConstants.HEADER_AUTHORIZATION, equalTo(GOOD_BASIC_AUTH))
                .withRequestBody(containing(HttpConstants.AUTHORIZATION_CODE_TYPE))
                .withRequestBody(containing(HttpConstants.CODE + "=authorized"))
                .willReturn(WireMock.aResponse()
                        .withHeader(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.MEDIA_TYPE_APPLICATION_JSON)
                        .withBody(ACCESS_TOKEN)));

        // client credentials token request succeeds
        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo("/token"))
                .withHeader(HttpConstants.HEADER_AUTHORIZATION, equalTo(GOOD_BASIC_AUTH))
                .withRequestBody(containing(HttpConstants.GRANT_TYPE + "=" + HttpConstants.CLIENT_CREDENTIALS))
                .willReturn(WireMock.aResponse()
                        .withHeader(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.MEDIA_TYPE_APPLICATION_JSON)
                        .withBody(ACCESS_TOKEN)));

        return Collections.emptyMap();
    }

    protected static void requestReceived(final Request inRequest, final Response inResponse) {
        logger.info("WireMock request at URL: {}\n" +
                        "  request headers: \n{}" +
                        "  request body: {}\n" +
                        "  response headers: \n{}" +
                        "  response body: {}",
                inRequest.getAbsoluteUrl(),
                inRequest.getHeaders(), inRequest.getBodyAsString(),
                inResponse.getHeaders(), inResponse.getBodyAsString());
    }

    @Override
    public void stop() {
        if (null != wireMockServer) {
            wireMockServer.stop();
        }
    }

    @Override
    public void inject(final Object testInstance) {
        // pass the wiremock base uri back into the test so that it can be modified for different scenarios
        if (testInstance instanceof AuthManagerTest) {
            ((AuthManagerTest) testInstance).setBaseUri(URI.create(wireMockServer.baseUrl() + "/"));
        }
    }

    String getDiscoveryDocument(final String baseUrl) {
        return "{" +
                "\"issuer\": \"" + baseUrl + "\"," +
                "\"authorization_endpoint\": \"" + baseUrl + "/authorization\"," +
                "\"token_endpoint\": \"" + baseUrl + "/token\"," +
                "\"registration_endpoint\": \"" + baseUrl + "/register\"," +
                "\"grant_types_supported\":[\"authorization_code\",\"refresh_token\",\"client_credentials\"]" +
                "}";
    }
}
