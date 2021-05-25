package org.solid.testharness.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.http.SolidClient;

import java.net.URI;
import java.net.http.HttpHeaders;

public class SolidResource {
    private static final Logger logger = LoggerFactory.getLogger(SolidResource.class);

    protected SolidClient solidClient;
    protected URI url;
    private URI aclUrl;
    private Boolean aclAvailable;
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
                if (headers != null && headers.allValues("Link").size() != 0) {
                    final URI aclLink = solidClient.getAclLink(headers);
                    if (aclLink != null) {
                        logger.debug("ACL LINK {}", aclLink);
                        aclUrl = resourceUri.resolve(aclLink);
                        aclAvailable = true;
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to create resource at {}: {}", url, e);
                return;
            }
        }
        this.url = resourceUri;
        logger.debug("SolidResource: {}", resourceUri);
    }

    public static SolidResource create(final SolidClient solidClient, final String url, final String body,
                                       final String type) {
        return new SolidResource(solidClient, url, body, type);
    }

    public boolean exists() {
        return url != null;
    }

    public boolean setAcl(final String acl) throws Exception {
        if (Boolean.FALSE.equals(aclAvailable)) return false;
        final String url = getAclUrl();
        if (url == null) return false;
        return solidClient.createAcl(URI.create(url), acl);
    }

    public String getUrl() {
        return url != null ? url.toString() : null;
    }

    public String getPath() {
        return url != null ? url.getPath() : null;
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
        if (aclUrl == null && !Boolean.FALSE.equals(aclAvailable)) {
            final URI aclLink = solidClient.getResourceAclLink(url);
            if (aclLink != null) {
                aclUrl = url.resolve(aclLink);
            }
            aclAvailable = aclLink != null;
        }
        return aclUrl != null ? aclUrl.toString() : null;
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
