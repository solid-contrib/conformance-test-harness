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
package org.solid.testharness.config;

import io.smallrye.config.WithName;
import org.solid.testharness.http.HttpUtils;
import org.solid.testharness.utils.TestHarnessInitializationException;

import java.net.URI;
import java.util.Optional;

public interface UserCredentials {
    @WithName("webid")
    String webId();

    @WithName("refreshtoken")
    Optional<String> refreshToken();

    @WithName("clientid")
    Optional<String> clientId();

    @WithName("clientsecret")
    Optional<String> clientSecret();

    Optional<URI> idp();

    Optional<String> username();

    Optional<String> password();

    default URI getWebId() {
        final URI uri;
        try {
            uri = URI.create(webId());
        } catch (Exception e) {
            throw new TestHarnessInitializationException("The webId " + webId() + " is missing or invalid", e);
        }
        if (!HttpUtils.isHttpProtocol(uri.getScheme())) {
            throw new TestHarnessInitializationException("The webId " + webId() + " must be an absolute URL");
        }
        return uri;
    }

    default URI getIdp() {
        if (idp().isPresent() && !HttpUtils.isHttpProtocol(idp().get().getScheme())) {
            throw new TestHarnessInitializationException("The IDP " + idp() + " be an absolute URL");
        }
        return idp().orElse(null);
    }

    default boolean isUsingUsernamePassword() {
        return username().isPresent() && password().isPresent();
    }

    default boolean isUsingRefreshToken() {
        return refreshToken().isPresent() && clientId().isPresent() && clientSecret().isPresent();
    }

    default boolean isUsingClientCredentials() {
        return refreshToken().isEmpty() && clientId().isPresent() && clientSecret().isPresent();
    }

    default String stringValue() {
        if (isUsingUsernamePassword()) {
            return String.format("UserCredentials: username=%s, password=%s, idp=%s",
                    mask(username()), mask(password()), getIdp()
            );
        } else if (isUsingRefreshToken()) {
            return String.format("UserCredentials: refreshToken=%s, clientId=%s, clientSecret=%s, idp=%s",
                    mask(refreshToken()), mask(clientId()), mask(clientSecret()), getIdp()
            );
        } else if (isUsingClientCredentials()) {
            return String.format("UserCredentials: clientId=%s, clientSecret=%s, idp=%s",
                    mask(clientId()), mask(clientSecret()), getIdp()
            );
        } else {
            return String.format("UserCredentials: username=%s, password=%s, " +
                            "refreshToken=%s, clientId=%s, clientSecret=%s, idp=%s",
                    mask(username()), mask(password()),
                    mask(refreshToken()), mask(clientId()), mask(clientSecret()), getIdp()
            );
        }
    }

    private String mask(final Optional<String> value) {
        return value.isPresent() ? "***" : "null";
    }
}

