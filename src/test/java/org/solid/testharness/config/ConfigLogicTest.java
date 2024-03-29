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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

class ConfigLogicTest {

    @Test
    void getTestSubjectsAbsolute() {
        final Config config = new Config();
        config.target = Optional.of("https://example.org/testserver");
        config.setSubjectsUrl("https://example2.org/subjects.ttl");
        assertEquals(iri("https://example.org/testserver"), config.getTestSubject());
    }

    @Test
    void getTestSubjectRelative() {
        final Config config = new Config();
        config.target = Optional.of("testserver");
        config.setSubjectsUrl("https://example.org/subjects.ttl");
        assertEquals(iri("https://example.org/testserver"), config.getTestSubject());
    }

    @Test
    void getSubjectsUrlNoConfigException() {
        final Config config = new Config();
        config.subjectsFile = Optional.empty();
        assertThrows(TestHarnessInitializationException.class, config::getSubjectsUrl);
    }

    @Test
    void getSubjectsUrlUseConfigHttp() throws MalformedURLException {
        final Config config = new Config();
        config.subjectsFile = Optional.of("http://example.org");
        assertEquals(new URL("http://example.org"), config.getSubjectsUrl());
    }

    @Test
    void getSubjectsUrlUseConfigHttps() throws MalformedURLException {
        final Config config = new Config();
        config.subjectsFile = Optional.of(TestUtils.SAMPLE_BASE);
        assertEquals(new URL(TestUtils.SAMPLE_BASE), config.getSubjectsUrl());
    }

    @Test
    void getSubjectsUrlUseConfigFile() throws MalformedURLException {
        final Config config = new Config();
        config.subjectsFile = Optional.of("file:/subjectFile");
        assertEquals(new URL("file:/subjectFile"), config.getSubjectsUrl());
    }

    @Test
    void getSubjectsUrlUseConfigPath() throws MalformedURLException {
        final Config config = new Config();
        config.subjectsFile = Optional.of("subjectFile");
        final URL url = Path.of("subjectFile").toAbsolutePath().normalize().toUri().toURL();
        assertEquals(url, config.getSubjectsUrl());
    }

    @Test
    void getSubjectsUrlUseConfigEmpty() {
        final Config config = new Config();
        config.subjectsFile = Optional.of("  ");
        assertNull(config.getSubjectsUrl());
    }

    @Test
    void getSubjectsUrlUseConfigBad() {
        final Config config = new Config();
        config.subjectsFile = Optional.of("http://domain:-100/invalid");
        assertThrows(TestHarnessInitializationException.class, config::getSubjectsUrl);
    }

    @Test
    void getSubjectsUrlUseSet() throws MalformedURLException {
        final Config config = new Config();
        config.setSubjectsUrl(TestUtils.SAMPLE_BASE);
        assertEquals(new URL(TestUtils.SAMPLE_BASE), config.getSubjectsUrl());
    }

    @Test
    void getTolerableFailuresFileEmpty() {
        final Config config = new Config();
        config.tolerableFailuresPath = Optional.of("  ");
        assertNull(config.getTolerableFailuresFile());
    }

    @Test
    void getTolerableFailuresFileFails() {
        final Config config = new Config();
        assertThrows(TestHarnessInitializationException.class,
                () -> config.setTolerableFailuresFile("missing-file.txt"));
    }

    @Test
    void getTestSourcesNoConfigException() {
        final Config config = new Config();
        config.sourceList = Optional.empty();
        assertThrows(TestHarnessInitializationException.class, config::getTestSources);
    }

    @Test
    void getTestSourcesUseConfigOne() throws MalformedURLException {
        final Config config = new Config();
        config.sourceList = Optional.of(List.of(TestUtils.SAMPLE_BASE));
        final List<URL> list = config.getTestSources();
        assertEquals(1, list.size());
        assertEquals(new URL(TestUtils.SAMPLE_BASE), list.get(0));
    }

    @Test
    void getTestSourcesUseConfigTwo() throws MalformedURLException {
        final Config config = new Config();
        config.sourceList = Optional.of(List.of("https://example.org", "http://example.org"));
        final List<URL> list = config.getTestSources();
        assertEquals(2, list.size());
        assertEquals(new URL("https://example.org"), list.get(0));
        assertEquals(new URL("http://example.org"), list.get(1));
    }

    @Test
    void getTestSourcesUseConfigBad() {
        final Config config = new Config();
        config.sourceList = Optional.of(List.of("http://domain:-100/invalid"));
        assertThrows(TestHarnessInitializationException.class, config::getTestSources);
    }

    @Test
    void getTestSourcesUseSet() throws MalformedURLException {
        final Config config = new Config();
        config.setTestSources(List.of("https://example.org", "http://example.org"));
        final List<URL> list = config.getTestSources();
        assertEquals(2, list.size());
        assertEquals(new URL("https://example.org"), list.get(0));
        assertEquals(new URL("http://example.org"), list.get(1));
    }

    @Test
    void getTestSourcesUseSetEmpty() {
        final Config config = new Config();
        config.setTestSources(Collections.emptyList());
        final List<URL> list = config.getTestSources();
        assertTrue(list.isEmpty());
    }

    @Test
    void getTestSourcesUseSetNull() {
        final Config config = new Config();
        assertThrows(NullPointerException.class, () ->config.setTestSources(null));
    }

    @Test
    void getServerRootNoSlash() {
        final Config config = new Config();
        config.serverRoot = Optional.of("https://localhost");
        assertEquals("https://localhost/", config.getServerRoot());
    }

    @Test
    void getServerRootWithSlash() {
        final Config config = new Config();
        config.serverRoot = Optional.of("https://localhost/");
        assertEquals("https://localhost/", config.getServerRoot());
    }

    @Test
    void getTestContainerWithSlashes() {
        final Config config = new Config();
        config.testContainer = Optional.of("/test/");
        assertEquals("/test/", config.getTestContainer());
    }

    @Test
    void getTestContainerNoSlashes() {
        final Config config = new Config();
        config.testContainer = Optional.of("test");
        assertEquals("test/", config.getTestContainer());
    }

    @Test
    void getSolidIdentityProviderNull() {
        final Config config = new Config();
        config.solidIdentityProvider = Optional.empty();
        assertNull(config.getSolidIdentityProvider());
    }

    @Test
    void getSolidIdentityProviderInvalid() {
        final Config config = new Config();
        config.solidIdentityProvider = Optional.of(URI.create("test"));
        assertThrows(TestHarnessInitializationException.class, config::getSolidIdentityProvider);
    }

    @Test
    void getLoginEndpointNull() {
        final Config config = new Config();
        config.loginEndpoint = Optional.empty();
        assertNull(config.getLoginEndpoint());
    }

    @Test
    void getUserRegistrationEndpointNull() {
        final Config config = new Config();
        config.userRegistrationEndpoint = Optional.empty();
        assertNull(config.getUserRegistrationEndpoint());
    }
}
