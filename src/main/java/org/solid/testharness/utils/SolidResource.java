package org.solid.testharness.utils;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.http.SolidClient;

import java.net.URI;
import java.net.http.HttpHeaders;

public class SolidResource {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.utils.SolidResource");

    protected SolidClient solidClient;
    protected URI url;
    private URI aclUrl = null;
    private Boolean aclAvailable = null;
    private boolean isContainer;

    public SolidResource(SolidClient solidClient, String url) {
        this(solidClient, url, null, null);
    }

    public SolidResource(SolidClient solidClient, String url, String body, String type) {
        if (solidClient == null) throw new IllegalArgumentException("Parameter solidClient is required");
        if (StringUtils.isEmpty(url)) throw new IllegalArgumentException("Parameter url is required");
        if (body != null && StringUtils.isEmpty(type)) {
            throw new IllegalArgumentException("Parameter type is required");
        }
        this.solidClient = solidClient;
        this.url = URI.create(url);
        if (!this.url.isAbsolute()) throw new IllegalArgumentException("The url must be absolute");

        isContainer = url.endsWith("/");

        if (body != null) {
            HttpHeaders headers;
            try {
                headers = solidClient.createResource(this.url, body, type);
                if (headers != null && headers.allValues("Link").size() != 0) {
                    URI aclLink = solidClient.getAclLink(headers);
                    if (aclLink != null) {
                        logger.debug("ACL LINK {}", aclLink);
                        aclUrl = this.url.resolve(aclLink);
                        aclAvailable = true;
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to create resource at " + url, e);
                this.url = null;
            }
        }
        logger.debug("SolidResource: {}", this.url);
    }

    public static SolidResource create(SolidClient solidClient, String url, String body, String type) {
        return new SolidResource(solidClient, url, body, type);
    }

    public boolean exists() {
        return url != null;
    }

    public boolean setAcl(String acl) throws Exception {
        if (Boolean.FALSE.equals(aclAvailable)) return false;
        String url = getAclUrl();
        if (url == null) return false;
        return solidClient.createAcl(URI.create(url), acl);
    }

    public String getUrl() {
        return url.toString();
    }

    public String getPath() {
        return url.getPath();
    }

    public boolean isContainer() {
        return isContainer;
    }

    public SolidContainer getContainer() {
        if (isContainer) {
            if (url.getPath().equals("/")) {
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
            URI aclLink = solidClient.getResourceAclLink(url.toString());
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
        if (isContainer) {
            return "SolidContainer: " + (url != null ? url.toString() : "null");
        } else {
            return "SolidResource: " + (url != null ? url.toString() : "null");
        }
    }
}
