/*
 * MIT License
 *
 * Copyright (c) 2019 - 2022 W3C Solid Community Group
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
import org.solid.testharness.utils.TestHarnessInitializationException;
import org.solid.testharness.utils.TestUtils;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserCredentialsTest {
    private static final String USERNAME = "USERNAME";
    private static final String PASSWORD = "PASSWORD";
    private static final String CLIENT_ID = "CLIENT_ID";
    private static final String CLIENT_SECRET = "CLIENT_SECRET";
    private static final String TOKEN = "TOKEN";

    @Test
    void loginCredentials() {
        final TestCredentials userCredentials = new TestCredentials();
        userCredentials.username = Optional.of(USERNAME);
        userCredentials.password = Optional.of(PASSWORD);

        assertFalse(userCredentials.isUsingRefreshToken());
        assertTrue(userCredentials.isUsingUsernamePassword());
        assertFalse(userCredentials.isUsingClientCredentials());
        assertEquals("UserCredentials: username=***, password=***, idp=null", userCredentials.stringValue());
    }

    @Test
    void getWebId() {
        final TestCredentials userCredentials = new TestCredentials();
        userCredentials.webId = TestUtils.SAMPLE_BASE;
        assertEquals(URI.create(TestUtils.SAMPLE_BASE), userCredentials.getWebId());
    }

    @Test
    void getWebIdNull() {
        final TestCredentials userCredentials = new TestCredentials();
        assertThrows(TestHarnessInitializationException.class, userCredentials::getWebId);
    }

    @Test
    void getWebIdBad() {
        final TestCredentials userCredentials = new TestCredentials();
        userCredentials.webId = "file://example.org";
        assertThrows(TestHarnessInitializationException.class, userCredentials::getWebId);
    }

    @Test
    void partialLoginCredentials() {
        final TestCredentials userCredentials = new TestCredentials();
        userCredentials.username = Optional.of(USERNAME);
        userCredentials.password = Optional.empty();
        assertFalse(userCredentials.isUsingUsernamePassword());
        userCredentials.username = Optional.empty();
        userCredentials.password = Optional.of(PASSWORD);
        assertFalse(userCredentials.isUsingUsernamePassword());
    }

    @Test
    void refreshCredentials() {
        final TestCredentials userCredentials = new TestCredentials();
        userCredentials.refreshToken = Optional.of(TOKEN);
        userCredentials.clientId = Optional.of(CLIENT_ID);
        userCredentials.clientSecret = Optional.of(CLIENT_SECRET);
        userCredentials.idp = Optional.of(URI.create(TestUtils.SAMPLE_BASE));
        assertTrue(userCredentials.isUsingRefreshToken());
        assertFalse(userCredentials.isUsingUsernamePassword());
        assertFalse(userCredentials.isUsingClientCredentials());
        assertEquals("UserCredentials: refreshToken=***, clientId=***, clientSecret=***, idp=" +
                        TestUtils.SAMPLE_BASE, userCredentials.stringValue());
    }

    @Test
    void partialRefreshCredentials() {
        final TestCredentials userCredentials = new TestCredentials();
        userCredentials.refreshToken = Optional.of(TOKEN);
        userCredentials.clientId = Optional.of(CLIENT_ID);
        userCredentials.clientSecret = Optional.empty();
        assertFalse(userCredentials.isUsingRefreshToken());
        userCredentials.refreshToken = Optional.of(TOKEN);
        userCredentials.clientId = Optional.empty();
        userCredentials.clientSecret = Optional.of(CLIENT_SECRET);
        assertFalse(userCredentials.isUsingRefreshToken());
    }

    @Test
    void clientCredentials() {
        final TestCredentials userCredentials = new TestCredentials();
        userCredentials.clientId = Optional.of(CLIENT_ID);
        userCredentials.clientSecret = Optional.of(CLIENT_SECRET);
        assertTrue(userCredentials.isUsingClientCredentials());
        assertFalse(userCredentials.isUsingRefreshToken());
        assertFalse(userCredentials.isUsingUsernamePassword());
        assertEquals("UserCredentials: clientId=***, clientSecret=***, idp=null", userCredentials.stringValue());
    }

    @Test
    void partialClientCredentials() {
        final TestCredentials userCredentials = new TestCredentials();
        userCredentials.clientId = Optional.empty();
        userCredentials.clientSecret = Optional.of(CLIENT_SECRET);
        assertFalse(userCredentials.isUsingClientCredentials());
        assertFalse(userCredentials.isUsingRefreshToken());
        assertFalse(userCredentials.isUsingUsernamePassword());
        userCredentials.clientId = Optional.of(CLIENT_ID);
        userCredentials.clientSecret = Optional.empty();
        assertFalse(userCredentials.isUsingClientCredentials());
        assertFalse(userCredentials.isUsingRefreshToken());
        assertFalse(userCredentials.isUsingUsernamePassword());
    }

    @Test
    void emptyCredentials() {
        final TestCredentials userCredentials = new TestCredentials();
        assertFalse(userCredentials.isUsingRefreshToken());
        assertFalse(userCredentials.isUsingUsernamePassword());
        assertEquals("UserCredentials: username=null, password=null, " +
                "refreshToken=null, clientId=null, clientSecret=null, idp=null", userCredentials.stringValue());
    }

    @Test
    void getIdp() {
        final TestCredentials userCredentials = new TestCredentials();
        userCredentials.idp = Optional.of(URI.create(TestUtils.SAMPLE_BASE));
        assertEquals(URI.create(TestUtils.SAMPLE_BASE), userCredentials.getIdp());
    }
    @Test
    void getIdpInvalid() {
        final TestCredentials userCredentials = new TestCredentials();
        userCredentials.idp = Optional.of(URI.create("test"));
        assertThrows(TestHarnessInitializationException.class, userCredentials::getIdp);
    }
}
