package org.solid.testharness.http;

public final class HttpConstants {
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String ALICE = "alice";
    public static final String BOB = "bob";

    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_DPOP = "DPoP";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_LOCATION = "Location";
    public static final String HEADER_WAC_ALLOW = "wac-allow";

    public static final String PREFIX_DPOP = "DPoP ";
    public static final String PREFIX_BEARER = "Bearer ";
    public static final String PREFIX_BASIC = "Basic ";

    public static final String MEDIA_TYPE_APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String MEDIA_TYPE_APPLICATION_JSON = "application/json";
    public static final String MEDIA_TYPE_TEXT_PLAIN = "text/plain";
    public static final String MEDIA_TYPE_TEXT_TURTLE = "text/turtle";

    public static final String METHOD_HEAD = "HEAD";
    public static final int STATUS_OK = 200;

    public static final String OPENID_CONFIGURATION = "/.well-known/openid-configuration";
    public static final String USER_AGENT = "User-Agent";

    public static final String GRANT_TYPE = "grant_type";
    public static final String CODE = "code";
    public static final String AUTHORIZATION_METHOD = "client_secret_basic";
    public static final String AUTHORIZATION_CODE_TYPE = "authorization_code";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String RESPONSE_TYPE = "response_type";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String SCOPE = "scope";
    public static final String OPENID = "openid";
    public static final String CLIENT_ID = "client_id";

    private HttpConstants() { }
}
