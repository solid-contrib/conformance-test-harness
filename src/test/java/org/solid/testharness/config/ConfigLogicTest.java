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

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigLogicTest {
    Config config = new Config();

    @Test
    void getServerRootNoSlash() {
        config.serverRoot = "https://localhost";
        assertEquals(URI.create("https://localhost/"), config.getServerRoot());
    }

    @Test
    void getServerRootWithSlash() {
        config.serverRoot = "https://localhost/";
        assertEquals(URI.create("https://localhost/"), config.getServerRoot());
    }

    @Test
    public void getTestContainerWithSlashes() {
        config.serverRoot = "https://localhost/";
        config.testContainer = "/test/";
        assertEquals("https://localhost/test/", config.getTestContainer());
    }

    @Test
    public void getTestContainerNoSlashes() {
        config.serverRoot = "https://localhost";
        config.testContainer = "test";
        assertEquals("https://localhost/test/", config.getTestContainer());
    }

    @Test
    public void getLoginEndpointNull() {
        config.loginEndpoint = Optional.empty();
        assertEquals(null, config.getLoginEndpoint());
    }
}
