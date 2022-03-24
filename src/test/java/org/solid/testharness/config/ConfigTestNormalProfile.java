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

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ConfigTestNormalProfile implements QuarkusTestProfile {
    @Override
    public String getConfigProfile() {
        return "test";
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "SOLID_IDENTITY_PROVIDER", "https://idp.example.org",
                "LOGIN_ENDPOINT", "https://example.org/login/password",
                "RESOURCE_SERVER_ROOT", "https://target.example.org",
                "TEST_CONTAINER", "test",
                "USER_REGISTRATION_ENDPOINT", "https://example.org/idp/register"
        );
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return Collections.emptyList();
    }
}
