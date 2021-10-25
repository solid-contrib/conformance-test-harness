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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.rdf4j.model.util.Values.iri;

@ApplicationScoped
public class Config {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    private static final String SLASH = "/";
    private static final List<String> TRUSTED_HOSTS = List.of("localhost", "server");
    public static final Integer DEFAULT_TIMEOUT = 5000;

    private IRI testSubject;
    private URL subjectsUrl;
    private List<URL> testSources;
    private File outputDir;
    private Map<String, Object> webIds;
    private boolean skipTearDown;
    private AccessControlMode accessControlMode;

    public enum AccessControlMode {
        ACP_LEGACY,
        ACP,
        WAC
    }

    // the settings are taken in the following order of preference:
    //   system property
    //   env variable
    //   .env file in cwd
    //   config/application.yaml (local)
    //   src/main/resources/application.yaml (project)
    // @seeAlso: https://quarkus.io/guides/config-reference#configuration_sources

    @ConfigProperty(name = "target")
    Optional<String> target;
    @ConfigProperty(name = "subjects")
    Optional<String> subjectsFile;
    @ConfigProperty(name = "sources")
    Optional<List<String>> sourceList;

    @ConfigProperty(name = "agent", defaultValue = "Solid-Conformance-Test-Suite")
    String agent;
    @ConfigProperty(name = "connectTimeout", defaultValue = "5000")
    Integer connectTimeout;
    @ConfigProperty(name = "readTimeout", defaultValue = "5000")
    Integer readTimeout;
    @ConfigProperty(name = "maxThreads", defaultValue = "8")
    Integer maxThreads;
    @ConfigProperty(name = "origin", defaultValue = "https://tester")
    String origin;
    @ConfigProperty(name = "setupRootAcl", defaultValue = "false")
    Boolean setupRootAcl;

    // properties normally found in environment variables or the .env file
    @ConfigProperty(name = "SOLID_IDENTITY_PROVIDER")
    String solidIdentityProvider;
    @ConfigProperty(name = "LOGIN_ENDPOINT")
    Optional<String> loginEndpoint;
    @ConfigProperty(name = "RESOURCE_SERVER_ROOT")
    String serverRoot;
    @ConfigProperty(name = "TEST_CONTAINER")
    String testContainer;
    @ConfigProperty(name = "USER_REGISTRATION_ENDPOINT")
    Optional<String> userRegistrationEndpoint;

    @Inject
    Users users;

    @Inject
    PathMappings pathMappings;

    public IRI getTestSubject() {
        if (testSubject == null && target.isPresent()) {
            logger.debug("Use config to set target: {}", target.get());
            final String subjectsBaseUri = iri(getSubjectsUrl().toString()).getNamespace();
            testSubject = target.get().contains(":")
                    ? iri(target.get())
                    : iri(subjectsBaseUri, target.get());
        }
        return testSubject;
    }

    public void setTestSubject(final IRI testSubject) {
        this.testSubject = testSubject;
    }

    public URL getSubjectsUrl() {
        if (subjectsUrl == null) {
            if (!subjectsFile.isPresent()) {
                throw new TestHarnessInitializationException("config option or subjects config is required");
            }
            this.subjectsUrl = createUrl(subjectsFile.get(), "subjects");
        }
        return subjectsUrl;
    }

    public void setSubjectsUrl(final String subjectsUrl) {
        this.subjectsUrl = createUrl(subjectsUrl, "subjects");
    }

    public List<URL> getTestSources()  {
        if (testSources == null) {
            if (!sourceList.isPresent()) {
                throw new TestHarnessInitializationException("source option or sources config is required");
            }
            this.testSources = sourceList.get().stream()
                    .map(ts -> createUrl(ts, "sources")).collect(Collectors.toList());
        }
        return testSources;
    }

    public void setTestSources(final List<String> testSourceList) {
        Objects.requireNonNull(testSourceList, "testSourceList is required");
        this.testSources = testSourceList.stream().map(ts -> createUrl(ts, "source")).collect(Collectors.toList());
    }

    public File getOutputDirectory() {
        return outputDir;
    }

    public void setOutputDirectory(final File outputDir) {
        this.outputDir = outputDir;
    }

    public URI getSolidIdentityProvider() {
        return URI.create(solidIdentityProvider).resolve(SLASH);
    }

    public URI getLoginEndpoint() {
        return loginEndpoint.map(URI::create).orElse(null);
    }

    public URI getUserRegistrationEndpoint() {
        return userRegistrationEndpoint.map(URI::create).orElse(null);
    }

    public URI getServerRoot() {
        return URI.create(serverRoot).resolve(SLASH);
    }

    public boolean overridingTrust() {
        return TRUSTED_HOSTS.contains(getServerRoot().getHost());
    }

    public String getTestContainer() {
        if (!testContainer.endsWith(SLASH)) {
            testContainer += SLASH;
        }
        return getServerRoot().resolve(testContainer).toString();
    }

    public UserCredentials getCredentials(final String user) {
        switch (user) {
            case HttpConstants.ALICE:
                return users.alice();
            case HttpConstants.BOB:
                return users.bob();
            default:
                return null;
        }
    }

    // used to provide the test features with the web IDs
    public Map<String, Object> getWebIds() {
        if (webIds == null) {
            webIds = Map.of(
                    HttpConstants.ALICE, users.alice().webId(),
                    HttpConstants.BOB, users.bob().webId()
            );
        }
        return webIds;
    }

    public String getAgent() {
        return agent;
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public Integer getReadTimeout() {
        return readTimeout;
    }

    public Integer getMaxThreads() {
        return maxThreads;
    }

    public String getOrigin() {
        return origin;
    }

    public boolean isSetupRootAcl() {
        return setupRootAcl;
    }

    public void setSkipTearDown(final boolean skipTearDown) {
        this.skipTearDown = skipTearDown;
    }

    public boolean isSkipTearDown() {
        return skipTearDown;
    }

    public AccessControlMode getAccessControlMode() {
        return accessControlMode;
    }

    public void setAccessControlMode(final AccessControlMode accessControlMode) {
        this.accessControlMode = accessControlMode;
    }

    public void logConfigSettings(final boolean withTests) {
        if (logger.isInfoEnabled()) {
            logger.info("Sources:        {}", getTestSources().toString());
            logger.info("Path mappings:  {}", pathMappings.stringValue());
            if (withTests) {
                logger.info("Subjects URL:   {}", getSubjectsUrl());
                logger.info("Target server:  {}", getTestSubject());
            }
        }
    }

    private URL createUrl(final String url, final String param) {
        if (!StringUtils.isBlank(url)) {
            try {
                if (url.startsWith("file:") || url.startsWith("http:") || url.startsWith("https:")) {
                    return new URL(url);
                } else {
                    return Path.of(url).toAbsolutePath().normalize().toUri().toURL();
                }
            } catch (MalformedURLException e) {
                throw (TestHarnessInitializationException) new TestHarnessInitializationException(
                        "%s - %s is not a valid file or URL: %s",
                        param, url,
                        e.toString()
                ).initCause(e);
            }
        }
        return null;
    }
}
