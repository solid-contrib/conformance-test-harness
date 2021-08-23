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

public final class HttpConstants {
    public static final String USERNAME = "username";
    public static final String EMAIL = "email";
    public static final String PASSWORD = "password";
    public static final String CONFIRM_PASSWORD = "confirmPassword";
    public static final String ALICE = "alice";
    public static final String BOB = "bob";

    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_DPOP = "DPoP";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_LOCATION = "Location";
    public static final String HEADER_WAC_ALLOW = "wac-allow";
    public static final String HEADER_LINK = "Link";

    public static final String PREFIX_DPOP = "DPoP ";
    public static final String PREFIX_BEARER = "Bearer ";
    public static final String PREFIX_BASIC = "Basic ";

    public static final String MEDIA_TYPE_APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String MEDIA_TYPE_APPLICATION_JSON = "application/json";
    public static final String MEDIA_TYPE_APPLICATION_SPARQL_UPDATE = "application/sparql-update";
    public static final String MEDIA_TYPE_TEXT_PLAIN = "text/plain";
    public static final String MEDIA_TYPE_TEXT_TURTLE = "text/turtle";
    public static final String MEDIA_TYPE_TEXT_HTML = "text/html";

    public static final String METHOD_HEAD = "HEAD";
    public static final String METHOD_PATCH = "PATCH";
    public static final int STATUS_OK = 200;

    public static final String OPENID_CONFIGURATION = ".well-known/openid-configuration";
    public static final String USER_AGENT = "User-Agent";

    public static final String GRANT_TYPE = "grant_type";
    public static final String CODE = "code";
    public static final String AUTHORIZATION_METHOD = "client_secret_basic";
    public static final String AUTHORIZATION_CODE_TYPE = "authorization_code";
    public static final String CLIENT_CREDENTIALS = "client_credentials";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String RESPONSE_TYPE = "response_type";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String SCOPE = "scope";
    public static final String OPENID = "openid";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";

    public static final String CONTAINER_LINK = "<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"type\"";

    private HttpConstants() { }
}
