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

import io.quarkus.arc.config.ConfigProperties;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@ConfigProperties(prefix = "alice")
public class UserCredentials {
    @NotNull
    public Optional<String> refreshToken;
    @NotNull
    public Optional<String> clientId;
    @NotNull
    public Optional<String> clientSecret;
    @NotNull
    public Optional<String> username;
    @NotNull
    public Optional<String> password;

    public UserCredentials() {
        refreshToken = Optional.empty();
        clientId = Optional.empty();
        clientSecret = Optional.empty();
        username = Optional.empty();
        password = Optional.empty();
    }

    public boolean isUsingUsernamePassword() {
        return username.isPresent() && password.isPresent();
    }

    public boolean isUsingRefreshToken() {
        return refreshToken.isPresent() && clientId.isPresent() && clientSecret.isPresent();
    }

    public boolean isUsingClientCredentials() {
        return clientSecret.isPresent() && !clientId.isPresent();
    }

    @Override
    public String toString() {
        if (isUsingUsernamePassword()) {
            return String.format("UserCredentials: username=%s, password=%s",
                    mask(username), mask(password)
            );
        } else if (isUsingRefreshToken()) {
            return String.format("UserCredentials: refreshToken=%s, clientId=%s, clientSecret=%s",
                    mask(refreshToken), mask(clientId), mask(clientSecret)
            );
        } else if (isUsingClientCredentials()) {
            return String.format("UserCredentials: clientSecret=%s",
                    mask(clientSecret)
            );
        } else {
            return String.format("UserCredentials: username=%s, password=%s, " +
                            "refreshToken=%s, clientId=%s, clientSecret=%s",
                    mask(username), mask(password),
                    mask(refreshToken), mask(clientId), mask(clientSecret)
            );
        }
    }

    private String mask(final Optional<String> value) {
        return value.isPresent() ? "***" : "null";
    }
}
