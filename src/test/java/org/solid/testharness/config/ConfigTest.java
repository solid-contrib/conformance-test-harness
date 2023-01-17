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

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.utils.TestUtils;

import javax.inject.Inject;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(ConfigTestNormalProfile.class)
class ConfigTest {
    @Inject
    Config config;

    @Test
    void getTestSubjectDefault() {
        assertEquals(iri(iri(config.getSubjectsUrl().toString()).getNamespace(), "testserver"),
                config.getTestSubject());
    }

    @Test
    void getSubjectsUrlDefault() throws MalformedURLException {
        assertEquals(TestUtils.getFileUrl("src/test/resources/config/config-sample.ttl"), config.getSubjectsUrl());
    }

    @Test
    void getTolerableFailuresFileDefault() {
        assertEquals("src/test/resources/config/tolerable-failures.txt", config.getTolerableFailuresFile().toString());
    }

    @Test
    void getTestSourcesDefault() {
        assertEquals("https://example.org/test-manifest-sample-1.ttl", config.getTestSources().get(0).toString());
        assertEquals("https://example.org/test-manifest-sample-2.ttl", config.getTestSources().get(1).toString());
        assertEquals("https://example.org/specification-sample-1.ttl", config.getTestSources().get(2).toString());
        assertEquals("https://example.org/specification-sample-2.ttl", config.getTestSources().get(3).toString());
    }

    @Test
    void getOutputDirectory() {
        assertNull(config.getOutputDirectory());
        final File file = new File("target");
        config.setOutputDirectory(file);
        assertEquals(file, config.getOutputDirectory());
    }

    @Test
    void getSolidIdentityProvider() {
        assertEquals(URI.create("https://idp.example.org/"), config.getSolidIdentityProvider());
    }

    @Test
    void getLoginEndpoint() {
        assertEquals(URI.create("https://example.org/login/password"), config.getLoginEndpoint());
    }

    @Test
    void getUserRegistrationEndpoint() {
        assertEquals(URI.create("https://example.org/idp/register"), config.getUserRegistrationEndpoint());
    }

    @Test
    void isSelfSignedCertsAllowed() {
        assertTrue(config.isSelfSignedCertsAllowed());
    }

    @Test
    void getServerRoot() {
        assertEquals("https://target.example.org/", config.getServerRoot());
    }

    @Test
    void getTestContainer() {
        assertEquals("test/", config.getTestContainer());
    }

    @Test
    void getWebIds() {
        assertEquals("https://example.org/solid-test-suite-alice/profile/card#me",
                config.getWebIds().get(HttpConstants.ALICE));
        assertEquals("https://example.org/solid-test-suite-bob/profile/card#me",
                config.getWebIds().get(HttpConstants.BOB));
    }

    @Test
    void getCredentialsAlice() {
        assertNotNull(config.getCredentials(HttpConstants.ALICE));
    }

    @Test
    void getCredentialsBob() {
        assertNotNull(config.getCredentials(HttpConstants.BOB));
    }

    @Test
    void getCredentialsCharlie() {
        assertNull(config.getCredentials("charlie"));
    }

    @Test
    void logConfigSettings() {
        assertDoesNotThrow(() -> config.logConfigSettings(Config.RunMode.TEST));
    }

    @Test
    void logConfigSettingsNoTests() {
        assertDoesNotThrow(() -> config.logConfigSettings(Config.RunMode.COVERAGE));
    }

    @Test
    void getAgent() {
        assertEquals("AGENT", config.getAgent());
    }

    @Test
    void getConnectTimeout() {
        assertEquals(1000, config.getConnectTimeout());
    }

    @Test
    void getReadTimeout() {
        assertEquals(1000, config.getReadTimeout());
    }

    @Test
    void getMaxThreads() {
        assertEquals(4, config.getMaxThreads());
    }

    @Test
    void getOrigin() {
        assertEquals("https://testharness", config.getOrigin());
    }

    @Test
    void generateResourceId() {
        final String id = config.generateResourceId();
        assertNotNull(id);
        assertNotEquals(id, config.generateResourceId());

    }
}
