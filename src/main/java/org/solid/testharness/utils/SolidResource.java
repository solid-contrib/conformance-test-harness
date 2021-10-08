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
package org.solid.testharness.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.accesscontrol.AccessControlFactory;
import org.solid.testharness.accesscontrol.AccessDataset;
import org.solid.testharness.accesscontrol.AccessDatasetBuilder;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.http.SolidClient;

import javax.enterprise.inject.spi.CDI;
import java.net.URI;
import java.net.http.HttpHeaders;

public class SolidResource {
    private static final Logger logger = LoggerFactory.getLogger(SolidResource.class);

    protected SolidClient solidClient;
    protected URI url;
    private URI aclUrl;
    private Boolean aclLinkAvailable;
    private boolean containerType;

    public SolidResource(final SolidClient solidClient, final String url) {
        this(solidClient, url, null, null);
    }

    public SolidResource(final SolidClient solidClient, final String url, final String body, final String type) {
        if (solidClient == null) throw new IllegalArgumentException("Parameter solidClient is required");
        if (StringUtils.isEmpty(url)) throw new IllegalArgumentException("Parameter url is required");
        if (body != null && StringUtils.isEmpty(type)) {
            throw new IllegalArgumentException("Parameter type is required");
        }
        this.solidClient = solidClient;

        final URI resourceUri = URI.create(url);
        if (!resourceUri.isAbsolute()) throw new IllegalArgumentException("The url must be absolute");

        containerType = url.endsWith("/");

        if (body != null) {
            final HttpHeaders headers;
            try {
                headers = solidClient.createResource(resourceUri, body, type);
                if (headers != null && headers.allValues(HttpConstants.HEADER_LINK).size() != 0) {
                    final URI aclLink = solidClient.getAclUri(headers);
                    if (aclLink != null) {
                        logger.debug("ACL LINK {}", aclLink);
                        aclUrl = resourceUri.resolve(aclLink);
                        aclLinkAvailable = true;
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to create resource at {}: {}", url, e);
                return;
            }
        }
        this.url = resourceUri;
        logger.debug("SolidResource proxy for: {}", resourceUri);
    }

    public static SolidResource create(final SolidClient solidClient, final String url, final String body,
                                       final String type) {
        return new SolidResource(solidClient, url, body, type);
    }

    public boolean exists() {
        return url != null;
    }

    public String getUrl() {
        return url != null ? url.toString() : null;
    }

    public String getPath() {
        return url != null ? url.getPath() : null;
    }

    public String getContentAsTurtle() throws Exception {
        return url != null ? solidClient.getContentAsTurtle(url) : "";
    }

    public boolean isContainer() {
        return containerType;
    }

    public SolidContainer getContainer() {
        if (containerType) {
            if ("/".equals(url.getPath())) {
                // already at root
                return null;
            } else {
                return new SolidContainer(solidClient, this.url.resolve("..").toString());
            }
        } else {
            return new SolidContainer(solidClient, this.url.resolve(".").toString());
        }
    }

    public String getAclUrl() throws Exception {
        if (aclUrl == null && !Boolean.FALSE.equals(aclLinkAvailable)) {
            final URI aclLink = solidClient.getAclUri(url);
            if (aclLink != null) {
                aclUrl = url.resolve(aclLink);
            }
            aclLinkAvailable = aclLink != null;
        }
        return aclUrl != null ? aclUrl.toString() : null;
    }

    public AccessDatasetBuilder getAccessDatasetBuilder(final String owner) throws Exception {
        final AccessControlFactory accessControlFactory = CDI.current().select(AccessControlFactory.class).get();
        final AccessDatasetBuilder builder = accessControlFactory.getAccessDatasetBuilder(getAclUrl());
        builder.setOwnerAccess(getUrl(), owner);
        return builder;
    }

    public AccessDataset getAccessDataset() throws Exception {
        return getAclUrl() != null ? solidClient.getAcl(aclUrl) : null;
    }

    public boolean setAccessDataset(final AccessDataset accessDataset) throws Exception {
        if (Boolean.FALSE.equals(aclLinkAvailable)) return false;
        final String url = getAclUrl();
        if (url == null) return false;
        return solidClient.createAcl(URI.create(url), accessDataset);
    }

    public void delete() throws Exception {
        solidClient.deleteResourceRecursively(url);
    }

    @Override
    public String toString() {
        if (containerType) {
            return "SolidContainer: " + url.toString();
        } else {
            return "SolidResource: " + (url != null ? url.toString() : "null");
        }
    }
}
