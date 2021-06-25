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

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserCredentialsTest {
    @Test
    public void loginCredentials() {
        final TestCredentials userCredentials = new TestCredentials();
        userCredentials.username = Optional.of("USERNAME");
        userCredentials.password = Optional.of("PASSWORD");

        assertFalse(userCredentials.isUsingRefreshToken());
        assertTrue(userCredentials.isUsingUsernamePassword());
        assertFalse(userCredentials.isUsingClientCredentials());
        assertEquals("UserCredentials: username=***, password=***", userCredentials.stringValue());
    }

    @Test
    public void partialLoginCredentials() {
        final TestCredentials userCredentials = new TestCredentials();
        userCredentials.username = Optional.of("USERNAME");
        userCredentials.password = Optional.empty();
        assertFalse(userCredentials.isUsingUsernamePassword());
        userCredentials.username = Optional.empty();
        userCredentials.password = Optional.of("PASSWORD");
        assertFalse(userCredentials.isUsingUsernamePassword());
    }

    @Test
    public void refreshCredentials() {
        final TestCredentials userCredentials = new TestCredentials();
        userCredentials.refreshToken = Optional.of("TOKEN");
        userCredentials.clientId = Optional.of("CLIENT_ID");
        userCredentials.clientSecret = Optional.of("CLIENT_SECRET");
        assertTrue(userCredentials.isUsingRefreshToken());
        assertFalse(userCredentials.isUsingUsernamePassword());
        assertFalse(userCredentials.isUsingClientCredentials());
        assertEquals("UserCredentials: refreshToken=***, clientId=***, clientSecret=***",
                userCredentials.stringValue());
    }

    @Test
    public void partialRefreshCredentials() {
        final TestCredentials userCredentials = new TestCredentials();
        userCredentials.refreshToken = Optional.of("TOKEN");
        userCredentials.clientId = Optional.of("CLIENT_ID");
        userCredentials.clientSecret = Optional.empty();
        assertFalse(userCredentials.isUsingRefreshToken());
        userCredentials.refreshToken = Optional.of("TOKEN");
        userCredentials.clientId = Optional.empty();
        userCredentials.clientSecret = Optional.of("CLIENT_SECRET");
        assertFalse(userCredentials.isUsingRefreshToken());
        userCredentials.refreshToken = Optional.empty();
        userCredentials.clientId = Optional.of("CLIENT_ID");
        userCredentials.clientSecret = Optional.of("CLIENT_SECRET");
        assertFalse(userCredentials.isUsingRefreshToken());
    }

    @Test
    public void clientCredentials() {
        final TestCredentials userCredentials = new TestCredentials();
        userCredentials.clientSecret = Optional.of("CLIENT_SECRET");
        assertTrue(userCredentials.isUsingClientCredentials());
        assertFalse(userCredentials.isUsingRefreshToken());
        assertFalse(userCredentials.isUsingUsernamePassword());
        assertEquals("UserCredentials: clientSecret=***", userCredentials.stringValue());
    }

    @Test
    public void emptyCredentials() {
        final TestCredentials userCredentials = new TestCredentials();
        assertFalse(userCredentials.isUsingRefreshToken());
        assertFalse(userCredentials.isUsingUsernamePassword());
        assertEquals("UserCredentials: username=null, password=null, " +
                "refreshToken=null, clientId=null, clientSecret=null", userCredentials.stringValue());
    }
}
