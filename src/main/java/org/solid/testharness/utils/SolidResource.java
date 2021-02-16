package org.solid.testharness.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpHeaders;

public class SolidResource {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.utils.SolidResource");

    private HttpUtils httpUtils = null;
    private URI url = null;
    private URI parentUrl = null;
    private URI aclUrl = null;
    private URI parentAclUrl = null;
    private Boolean aclsAvailable = null;

    public SolidResource(HttpUtils httpUtils, String url, String body, String type) throws Exception {
        if (httpUtils == null) throw new IllegalArgumentException("Parameter httpUtils is required");
        if (url == null || url.isEmpty()) throw new IllegalArgumentException("Parameter url is required");
        if (body == null || body.isEmpty()) throw new IllegalArgumentException("Parameter body is required");
        if (type == null || type.isEmpty()) throw new IllegalArgumentException("Parameter type is required");
        this.httpUtils = httpUtils;
        this.url = URI.create(url);
        if (!this.url.isAbsolute()) throw new IllegalArgumentException("The url must be absolute");
        parentUrl = this.url.getPath().endsWith("/") ? this.url.resolve("..") : this.url.resolve(".");
//        logger.debug("PARENT URL {}", parentUrl);

        HttpHeaders headers = httpUtils.createResource(this.url, body, type);
        if (headers.allValues("Link").size() != 0) {
            URI aclLink = httpUtils.getAclLink(headers);
            if (aclLink != null) {
                logger.debug("ACL LINK {}", aclLink);
                aclUrl = this.url.resolve(aclLink);
                aclsAvailable = true;
            }
        }
    }

    public boolean setAcl(String acl) throws IOException, InterruptedException {
        if (Boolean.FALSE.equals(aclsAvailable)) return false;
        URI url = getAclUrl();
        if (url == null) return false;
//        logger.debug("Adding ACL to {}:\n{}", url, acl);
        return httpUtils.createAcl(url, acl);
    }

    public boolean setParentAcl(String acl) throws IOException, InterruptedException {
        if (Boolean.FALSE.equals(aclsAvailable)) return false;
        URI url = getParentAclUrl();
        if (url == null) return false;
        return httpUtils.createAcl(url, acl);
    }

    public URI getUrl() {
        return url;
    }

    public URI getAclUrl() throws IOException, InterruptedException {
        if (aclUrl == null && !Boolean.FALSE.equals(aclsAvailable)) {
            URI aclLink = httpUtils.getResourceAclLink(url.toString());
            if (aclLink != null) {
                aclUrl = url.resolve(aclLink);
            }
            aclsAvailable = aclLink != null;
        }
        return aclUrl;
    }

    public URI getParentUrl() {
        return parentUrl;
    }

    public String getParentPath() {
        return parentUrl.getPath();
    }

    public URI getParentAclUrl() throws IOException, InterruptedException {
//        logger.debug("GET PARENT ACL {} {} {}", parentUrl, parentAclUrl, aclsAvailable);
        if (parentAclUrl == null && !Boolean.FALSE.equals(aclsAvailable)) {
            URI aclLink = httpUtils.getResourceAclLink(parentUrl.toString());
//            logger.debug("PARENT ACL {}", aclLink);
            if (aclLink != null) {
                parentAclUrl = url.resolve(aclLink);
            }
            aclsAvailable = aclLink != null;
        }
        return parentAclUrl;
    }
}
