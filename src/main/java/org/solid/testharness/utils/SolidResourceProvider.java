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
package org.solid.testharness.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.accesscontrol.AccessDataset;
import org.solid.testharness.accesscontrol.AccessDatasetBuilder;
import org.solid.testharness.config.Config;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.http.SolidClientProvider;

import javax.enterprise.inject.spi.CDI;
import java.net.URI;
import java.net.http.HttpHeaders;

public class SolidResourceProvider {
    private static final Logger logger = LoggerFactory.getLogger(SolidResourceProvider.class);
    private static final String ROOT = "/";

    protected SolidClientProvider solidClientProvider;
    protected URI url;
    private URI aclUrl;
    private Boolean aclLinkAvailable;
    private final boolean containerType;
    private final Config config;

    public SolidResourceProvider(final SolidClientProvider solidClientProvider, final URI url)
            throws TestHarnessException {
        this(solidClientProvider, url, null, null);
    }

    public SolidResourceProvider(final SolidClientProvider solidClientProvider, final URI url,
                                 final String body, final String type) throws TestHarnessException {
        if (solidClientProvider == null)
            throw new IllegalArgumentException("Parameter solidClientProvider is required");
        if (url == null) throw new IllegalArgumentException("Parameter url is required");
        if (!url.isAbsolute()) throw new IllegalArgumentException("The url must be absolute");
        if (body != null && StringUtils.isEmpty(type)) {
            throw new IllegalArgumentException("Parameter type is required");
        }
        this.solidClientProvider = solidClientProvider;
        config = CDI.current().select(Config.class).get();

        containerType = url.toString().endsWith(ROOT);

        if (body != null) {
            final HttpHeaders headers = solidClientProvider.createResource(url, body, type);
            getAclUrl(url, headers);
        }
        this.url = url;
        logger.debug("SolidResourceProvider proxy for: {}", url);
    }

    public URI getUrl() {
        return url;
    }

    public String getContentAsTurtle() throws TestHarnessException {
        return solidClientProvider.getContentAsTurtle(url);
    }

    public boolean isContainer() {
        return containerType;
    }

    protected void instantiateContainer() throws TestHarnessException {
        final HttpHeaders headers = solidClientProvider.createContainer(url);
        getAclUrl(url, headers);
    }

    @SuppressWarnings("java:S1130") // false-positive
    public SolidContainerProvider getContainer() throws TestHarnessException {
        SolidContainerProvider solidContainerProvider = null;
        if (containerType) {
            if (!ROOT.equals(url.getPath())) {
                solidContainerProvider = new SolidContainerProvider(solidClientProvider, this.url.resolve(".."));
            }
        } else {
            solidContainerProvider = new SolidContainerProvider(solidClientProvider, this.url.resolve("."));
        }
        return solidContainerProvider;
    }

    private void getAclUrl(final URI url, final HttpHeaders headers) {
        if (headers != null && !headers.allValues(HttpConstants.HEADER_LINK).isEmpty()) {
            final URI aclLink = solidClientProvider.getAclUri(headers);
            if (aclLink != null) {
                logger.debug("ACL LINK {}", aclLink);
                aclUrl = url.resolve(aclLink);
                aclLinkAvailable = true;
            }
        }
    }

    public URI getAclUrl() {
        if (aclUrl == null && !Boolean.FALSE.equals(aclLinkAvailable)) {
            final URI aclLink = solidClientProvider.getAclUri(url);
            if (aclLink != null) {
                aclUrl = url.resolve(aclLink);
            }
            aclLinkAvailable = aclLink != null;
        }
        return aclUrl;
    }

    public <T extends AccessDataset> AccessDatasetBuilder<T> getAccessDatasetBuilder() {
        final AccessDatasetBuilder<T> builder = solidClientProvider.getAccessDatasetBuilder(getAclUrl());
        final String owner = config.getWebIds().get(solidClientProvider.getClient().getUser());
        builder.setOwnerAccess(url.toString(), owner);
        return builder;
    }

    public AccessDataset getAccessDataset() {
        return getAclUrl() != null ? solidClientProvider.getAcl(aclUrl) : null;
    }

    public void setAccessDataset(final AccessDataset accessDataset) throws TestHarnessException {
        final URI uri = getAclUrl();
        if (uri == null) throw new TestHarnessException("Failed to find ACL Link for this resource");
        solidClientProvider.createAcl(uri, accessDataset);
    }

    @SuppressWarnings("java:S1130") // false-positive
    public SolidContainerProvider findStorage() throws TestHarnessException {
        URI testUri = url;
        boolean linkFound = false;
        boolean rootTested = false;
        while (!linkFound && !rootTested) {
            linkFound = solidClientProvider.hasStorageType(testUri);
            if (!linkFound) {
                final boolean atRoot = ROOT.equals(testUri.getPath());
                if (!atRoot) {
                    // haven't got to the root so keep going
                    testUri = testUri.getPath().endsWith(ROOT) ? testUri.resolve("..") : testUri.resolve(".");
                } else {
                    rootTested = true;
                }
            }
        }
        return linkFound ? new SolidContainerProvider(solidClientProvider, testUri) : null;
    }

    public boolean isStorageType() {
        return solidClientProvider.hasStorageType(url);
    }

    public void delete() {
        solidClientProvider.deleteResourceRecursively(url);
    }

    public String generateId() {
        return config.generateResourceId();
    }

    @Override
    public String toString() {
        return (containerType ? "SolidContainerProvider: " : "SolidResourceProvider: ") + url;
    }
}
