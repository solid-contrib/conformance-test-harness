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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.rdf4j.model.IRI;
import org.hashids.Hashids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.http.HttpUtils;
import org.solid.testharness.utils.TestHarnessInitializationException;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.eclipse.rdf4j.model.util.Values.iri;

@ApplicationScoped
public class Config {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    public static final Integer DEFAULT_TIMEOUT = 5000;

    private IRI testSubject;
    private URL subjectsUrl;
    private List<URL> testSources;
    private File tolerableFailuresFile;
    private File outputDir;
    private Map<String, String> webIds;
    private Hashids hashids;
    private final AtomicLong resourceCount = new AtomicLong();

    public enum RunMode {
        COVERAGE,
        TEST
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
    @ConfigProperty(name = "tolerableFailures")
    Optional<String> tolerableFailuresPath;

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

    // properties normally found in environment variables or the .env file
    @ConfigProperty(name = "SOLID_IDENTITY_PROVIDER")
    Optional<URI> solidIdentityProvider;
    @ConfigProperty(name = "LOGIN_ENDPOINT")
    Optional<String> loginEndpoint;
    @ConfigProperty(name = "RESOURCE_SERVER_ROOT")
    Optional<String> serverRoot;
    @ConfigProperty(name = "TEST_CONTAINER")
    Optional<String> testContainer;
    @ConfigProperty(name = "USER_REGISTRATION_ENDPOINT")
    Optional<String> userRegistrationEndpoint;
    @ConfigProperty(name = "ALLOW_SELF_SIGNED_CERTS")
    Optional<Boolean> allowSelfSignedCerts;

    @Inject
    Users users;

    @Inject
    PathMappings pathMappings;

    @PostConstruct
    void init() {
        hashids = new Hashids(UUID.randomUUID().toString(), 6);
    }

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
            if (subjectsFile.isEmpty()) {
                throw new TestHarnessInitializationException("Missing mandatory option: 'subjects' file location " +
                        "must be provided by command line option or config");
            }
            subjectsUrl = createUrl(subjectsFile.get(), "subjects");
        }
        return subjectsUrl;
    }

    public void setSubjectsUrl(final String subjectsUrl) {
        this.subjectsUrl = createUrl(subjectsUrl, "subjects");
    }

    public File getTolerableFailuresFile() {
        if (tolerableFailuresFile == null && tolerableFailuresPath.isPresent()) {
            tolerableFailuresFile = createFile(tolerableFailuresPath.get(), "tolerableFailures");
        }
        return tolerableFailuresFile;
    }

    public void setTolerableFailuresFile(final String tolerableFailuresPath) {
        this.tolerableFailuresFile = createFile(tolerableFailuresPath, "tolerableFailures");
    }

    public List<URL> getTestSources()  {
        if (testSources == null) {
            if (sourceList.isEmpty()) {
                throw new TestHarnessInitializationException("Missing mandatory option: either 'source' command " +
                        "line option or 'sources' config must be provided");
            }
            testSources = sourceList.get().stream()
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
        if (solidIdentityProvider.isPresent() && !HttpUtils.isHttpProtocol(solidIdentityProvider.orElse(null))) {
            throw new TestHarnessInitializationException(MessageFormat.format(
                    "SOLID_IDENTITY_PROVIDER must be an absolute URL: [{0}]", solidIdentityProvider));
        }
        return solidIdentityProvider.map(u -> u.resolve("/")).orElse(null);
    }

    public URI getLoginEndpoint() {
        return loginEndpoint.map(URI::create).orElse(null);
    }

    public URI getUserRegistrationEndpoint() {
        return userRegistrationEndpoint.map(URI::create).orElse(null);
    }

    public String getServerRoot() {
        return serverRoot.map(HttpUtils::ensureSlashEnd).orElse(null);
    }

    public String getTestContainer() {
        return testContainer.map(HttpUtils::ensureSlashEnd).orElse(null);
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
    public Map<String, String> getWebIds() {
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

    public String generateResourceId() {
        return hashids.encode(resourceCount.getAndIncrement());
    }

    public Boolean isSelfSignedCertsAllowed() {
        return allowSelfSignedCerts.orElse(false);
    }

    public void logConfigSettings(final RunMode mode) {
        if (logger.isInfoEnabled()) {
            logger.info("Sources:            {}", getTestSources());
            logger.info("Path mappings:      {}", pathMappings.stringValue());
            logger.info("Output directory:   {}", getOutputDirectory());
            if (mode == RunMode.TEST) {
                logger.info("Subjects URL:       {}", getSubjectsUrl());
                logger.info("Target server:      {}", getTestSubject());
                logger.info("Connect timeout:    {}", getConnectTimeout());
                logger.info("Read timeout:       {}", getReadTimeout());
                logger.info("Max threads:        {}", getMaxThreads());
                logger.info("Alice WebID:        {}", users.alice().webId());
                logger.info("Alice IDP:          {}", users.alice().getIdp());
                logger.info("Bob WebID:          {}", users.bob().webId());
                logger.info("Bob IDP:            {}", users.bob().getIdp());
                logger.info("Solid IdP:          {}", getSolidIdentityProvider());
                logger.info("Server root:        {}", getServerRoot());
                logger.info("Test container:     {}", getTestContainer());
                logger.info("Tolerable failures: {}", getTolerableFailuresFile());
                logger.info("Allow self-signed:  {}", isSelfSignedCertsAllowed());
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
                throw new TestHarnessInitializationException(
                        MessageFormat.format("Invalid file or url provided for {0}: [{1}]", param, url), e);
            }
        }
        return null;
    }

    private File createFile(final String path, final String param) {
        if (!StringUtils.isBlank(path)) {
            final File file = new File(path);
            if (!file.isFile()) {
                throw new TestHarnessInitializationException(
                        MessageFormat.format("Invalid file provided for {0}: [{1}]", param, path));
            }
            return file;
        }
        return null;
    }
}
