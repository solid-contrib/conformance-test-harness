package org.solid.testharness.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.http.SolidClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpHeaders;

public class SolidResource {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.utils.SolidResource");

    private SolidClient solidClient = null;
    private URI url = null;
    private URI parentUrl = null;
    private URI aclUrl = null;
    private URI parentAclUrl = null;
    private Boolean aclsAvailable = null;

    public SolidResource(SolidClient solidClient, String url, String body, String type) {
        if (solidClient == null) throw new IllegalArgumentException("Parameter solidClient is required");
        if (url == null || url.isEmpty()) throw new IllegalArgumentException("Parameter url is required");
        if (body == null || body.isEmpty()) throw new IllegalArgumentException("Parameter body is required");
        if (type == null || type.isEmpty()) throw new IllegalArgumentException("Parameter type is required");
        this.solidClient = solidClient;
        this.url = URI.create(url);
        if (!this.url.isAbsolute()) throw new IllegalArgumentException("The url must be absolute");
        parentUrl = this.url.getPath().endsWith("/") ? this.url.resolve("..") : this.url.resolve(".");
//        logger.debug("PARENT URL {}", parentUrl);

        HttpHeaders headers = null;
        try {
            headers = solidClient.createResource(this.url, body, type);
            if (headers.allValues("Link").size() != 0) {
                URI aclLink = solidClient.getAclLink(headers);
                if (aclLink != null) {
                    logger.debug("ACL LINK {}", aclLink);
                    aclUrl = this.url.resolve(aclLink);
                    aclsAvailable = true;
                }
            }
        } catch (Exception e) {
            this.url = null;
        }
    }

    public boolean exists() {
        return url != null;
    }

    public boolean setAcl(String acl) throws Exception {
        if (Boolean.FALSE.equals(aclsAvailable)) return false;
        String url = getAclUrl();
        if (url == null) return false;
        return solidClient.createAcl(URI.create(url), acl);
    }

    public boolean setParentAcl(String acl) throws Exception {
        if (Boolean.FALSE.equals(aclsAvailable)) return false;
        String url = getParentAclUrl();
        if (url == null) return false;
        return solidClient.createAcl(URI.create(url), acl);
    }

    public String getUrl() {
        return url.toString();
    }

    public String getAclUrl() throws Exception {
        if (aclUrl == null && !Boolean.FALSE.equals(aclsAvailable)) {
            URI aclLink = solidClient.getResourceAclLink(url.toString());
            if (aclLink != null) {
                aclUrl = url.resolve(aclLink);
            }
            aclsAvailable = aclLink != null;
        }
        return aclUrl != null ? aclUrl.toString() : null;
    }

    public String getParentUrl() {
        return parentUrl.toString();
    }

    public String getParentPath() {
        return parentUrl.getPath();
    }

    public String getParentAclUrl() throws Exception {
//        logger.debug("GET PARENT ACL {} {} {}", parentUrl, parentAclUrl, aclsAvailable);
        if (parentAclUrl == null && !Boolean.FALSE.equals(aclsAvailable)) {
            URI aclLink = solidClient.getResourceAclLink(parentUrl.toString());
//            logger.debug("PARENT ACL {}", aclLink);
            if (aclLink != null) {
                parentAclUrl = url.resolve(aclLink);
            }
            aclsAvailable = aclLink != null;
        }
        return parentAclUrl != null ? parentAclUrl.toString() : null;
    }
}
