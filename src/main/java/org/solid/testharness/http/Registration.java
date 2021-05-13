package org.solid.testharness.http;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Registration {
    private String clientId;
    private String clientSecret;
    private String applicationType;
    private List<String> redirectUris;
    private String tokenEndpointAuthMethod;

    public String getClientId() {
        return clientId;
    }

    @JsonSetter("client_id")
    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    @JsonSetter("client_secret")
    public void setClientSecret(final String clientSecret) {
        this.clientSecret = clientSecret;
    }

    @JsonGetter("application_type")
    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(final String applicationType) {
        this.applicationType = applicationType;
    }

    @JsonGetter("redirect_uris")
    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(final List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    @JsonGetter("token_endpoint_auth_method")
    public String getTokenEndpointAuthMethod() {
        return tokenEndpointAuthMethod;
    }

    public void setTokenEndpointAuthMethod(final String tokenEndpointAuthMethod) {
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    }
}
