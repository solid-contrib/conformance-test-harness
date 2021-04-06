package org.solid.testharness.config;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Models;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.SOLID;
import org.solid.common.vocab.SOLID_TEST;
import org.solid.testharness.utils.DataModelBase;

import javax.validation.constraints.Positive;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class TargetServer extends DataModelBase {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.config.TargetServer");

    private Map<String, Boolean> features;
    private String solidIdentityProvider;
    private String serverRoot;
    @Positive(message = "maxThreads must be >= 1")
    private Integer maxThreads;
    private String rootContainer;
    private String testContainer;
    private Map<String, UserCredentials> users;
    private Map<String, Object> webIds;
    private URI loginEndpoint;
    private String origin;
    private Boolean setupRootAcl;
    private Boolean disableDPoP;

    public TargetServer(IRI subject) {
        super(subject, DEEP);
        logger.debug("Retrieved {} statements for {}", super.size(), subject);
    }

    public Map<String, Boolean> getFeatures() {
        if (features == null) {
            this.features = getLiteralsAsStringSet(SOLID_TEST.features).stream().collect(Collectors.toMap(Object::toString, f -> Boolean.TRUE));
        }
        return features;
    }

    public String getSolidIdentityProvider() {
        if (solidIdentityProvider == null) {
            this.solidIdentityProvider = getIriAsString(SOLID.oidcIssuer);
        }
        return solidIdentityProvider;
    }

    public String getServerRoot() {
        if (serverRoot == null) {
            this.serverRoot = getIriAsString(SOLID_TEST.serverRoot);
        }
        return serverRoot;
    }

    public int getMaxThreads() {
        if (maxThreads == null) {
            this.maxThreads = getLiteralAsInt(SOLID_TEST.maxThreads);
        }
        return maxThreads;
    }

    public String getRootContainer() {
        if (rootContainer == null) {
            this.rootContainer = getIriAsString(SOLID_TEST.podRoot);
        }
        return rootContainer;
    }

    public String getTestContainer() {
        if (testContainer == null) {
            this.testContainer = getLiteralAsString(SOLID_TEST.testContainer);
        }
        return testContainer;
    }

    public Map<String, UserCredentials> getUsers() {
        if (users == null) {
            Optional<Resource> aliceUser = Models.objectResource(model.filter(subject, SOLID_TEST.aliceUser, null));
            Optional<Resource> bobUser = Models.objectResource(model.filter(subject, SOLID_TEST.bobUser, null));
            users = new HashMap<>();
            webIds = new HashMap<>();
            if (aliceUser.isPresent()) {
                UserCredentials cred = new UserCredentials(model, aliceUser.get());
                users.put("alice", cred);
                webIds.put("alice", cred.getWebID());
            }
            if (bobUser.isPresent()) {
                UserCredentials cred = new UserCredentials(model, bobUser.get());
                users.put("bob", cred);
                webIds.put("bob", cred.getWebID());
            }
        }
        return users;
    }

    public Map<String, Object> getWebIds() {
        return webIds;
    }

    public URI getLoginEndpoint() {
        if (loginEndpoint == null) {
            try {
                this.loginEndpoint = new URI(getIriAsString(SOLID.loginEndpoint));
            } catch (URISyntaxException | NullPointerException e) {
                logger.warn("{} is not a valid URI: {}", SOLID.loginEndpoint.stringValue(), e.getMessage());
            }
        }
        return loginEndpoint;
    }

    public String getOrigin() {
        if (origin == null) {
            this.origin = getIriAsString(SOLID_TEST.origin);
        }
        return origin;
    }

    public boolean isSetupRootAcl() {
        if (setupRootAcl == null) {
            this.setupRootAcl = getLiteralAsBoolean(SOLID_TEST.setupRootAcl);
        }
        return setupRootAcl;
    }

    public boolean isDisableDPoP() {
        if (disableDPoP == null) {
            // TODO: Add disableDPoP to vocab
            this.disableDPoP = false; //getLiteralAsBoolean(SOLID_TEST.disableDPoP);
        }
        return disableDPoP;
    }
}
