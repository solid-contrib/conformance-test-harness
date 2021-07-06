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
package org.solid.testharness2.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import org.solid.testharness.config.Config;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(ConfigTestBlankProfile.class)
public class ConfigMissingTest {
    @Inject
    Config config;

    @Test
    void getTestSubject() {
        assertNull(config.getTestSubject());
    }

    @Test
    void getSubjectsUrl() {
        assertThrows(TestHarnessInitializationException.class, () -> config.getSubjectsUrl());
    }

    @Test
    void getTestSources() {
        assertThrows(TestHarnessInitializationException.class, () -> config.getTestSources());
    }

    @Test
    void getLoginEndpoint() {
        assertNull(config.getLoginEndpoint());
    }

    @Test
    void getUserRegistrationEndpoint() {
        assertNull(config.getUserRegistrationEndpoint());
    }

    @Test
    void logConfigSettings() {
        assertDoesNotThrow(() -> config.logConfigSettings());
    }

    @Test
    void getAgent() {
        assertEquals("Solid-Conformance-Test-Suite", config.getAgent());
    }

    @Test
    void getConnectTimeout() {
        assertEquals(5000, config.getConnectTimeout());
    }

    @Test
    void getReadTimeout() {
        assertEquals(5000, config.getReadTimeout());
    }
}
