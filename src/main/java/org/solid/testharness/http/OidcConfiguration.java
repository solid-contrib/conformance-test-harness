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

@JsonIgnoreProperties(ignoreUnknown = true)
public class OidcConfiguration {
    private String issuer;
    private String authorizeEndpoint;
    private String tokenEndpoint;
    private String registrationEndpoint;

    public String getIssuer() {
        return issuer != null && !issuer.endsWith("/") ? issuer + "/" : issuer;
    }

    @JsonSetter("issuer")
    public void setIssuer(final String issuer) {
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
