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

import java.util.Optional;

@SuppressWarnings({"PMD.AvoidFieldNameMatchingMethodName", "OptionalUsedAsFieldOrParameterType"})
public class TestCredentials implements UserCredentials {
    public String webId;
    public Optional<String> refreshToken = Optional.empty();
    public Optional<String> clientId = Optional.empty();
    public Optional<String> clientSecret = Optional.empty();
    public Optional<String> username = Optional.empty();
    public Optional<String> password = Optional.empty();

    @Override
    public String webId() {
        return webId;
    }

    @Override
    public Optional<String> refreshToken() {
        return refreshToken;
    }

    @Override
    public Optional<String> clientId() {
        return clientId;
    }

    @Override
    public Optional<String> clientSecret() {
        return clientSecret;
    }

    @Override
    public Optional<String> username() {
        return username;
    }

    @Override
    public Optional<String> password() {
        return password;
    }
}
