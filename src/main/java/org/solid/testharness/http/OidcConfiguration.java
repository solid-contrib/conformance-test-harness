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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OidcConfiguration {
    private URI issuer;
    private URI authorizeEndpoint;
    private URI tokenEndpoint;
    private URI registrationEndpoint;
    private List<String> grantTypesSupported = Collections.emptyList();

    public URI getIssuer() {
        return issuer;
    }

    @JsonSetter("issuer")
    public void setIssuer(final String issuer) {
        if (!StringUtils.isEmpty(issuer)) {
            this.issuer = URI.create(issuer + "/").normalize();
        }
    }

    public URI getAuthorizeEndpoint() {
        return authorizeEndpoint;
    }

    @JsonSetter("authorization_endpoint")
    public void setAuthorizeEndpoint(final String authorizeEndpoint) {
        this.authorizeEndpoint = URI.create(authorizeEndpoint);
    }

    public URI getTokenEndpoint() {
        return tokenEndpoint;
    }

    @JsonSetter("token_endpoint")
    public void setTokenEndpoint(final String tokenEndpoint) {
        this.tokenEndpoint = URI.create(tokenEndpoint);
    }

    public URI getRegistrationEndpoint() {
        return registrationEndpoint;
    }

    @JsonSetter("registration_endpoint")
    public void setRegistrationEndpoint(final String registrationEndpoint) {
        this.registrationEndpoint = URI.create(registrationEndpoint);
    }

    @JsonSetter("grant_types_supported")
    public void setGrantTypesSupported(final List<String> grantTypesSupported) {
        this.grantTypesSupported = grantTypesSupported;
    }

    public List<String> getGrantTypesSupported() {
        return grantTypesSupported;
    }
}
