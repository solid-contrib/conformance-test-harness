package org.solid.testharness.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OidcConfiguration {
    private String issuer;
    private String authorizeEndpoint;
    private String tokenEndpoint;
    private String registrationEndpoint;

    public String getIssuer() {
        return issuer;
    }

    public void set(final String issuer) {
        this.issuer = issuer;
    }

    public String getAuthorizeEndpoint() {
        return authorizeEndpoint;
    }

    @JsonSetter("authorization_endpoint")
    public void setAuthorizeEndpoint(final String authorizeEndpoint) {
        this.authorizeEndpoint = authorizeEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    @JsonSetter("token_endpoint")
    public void setTokenEndpoint(final String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getRegistrationEndpoint() {
        return registrationEndpoint;
    }

    @JsonSetter("registration_endpoint")
    public void setRegistrationEndpoint(final String registrationEndpoint) {
        this.registrationEndpoint = registrationEndpoint;
    }
}
