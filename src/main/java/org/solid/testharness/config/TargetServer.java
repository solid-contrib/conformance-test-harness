package org.solid.testharness.config;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.SOLID;
import org.solid.common.vocab.SOLID_TEST;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.utils.DataModelBase;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class TargetServer extends DataModelBase {
    private static final Logger logger = LoggerFactory.getLogger(TargetServer.class);

    private Map<String, Boolean> features;
    private String solidIdentityProvider;
    @NotNull
    private URI serverRoot;
    @Positive(message = "maxThreads must be >= 1")
    private Integer maxThreads;
    @NotNull
    private String testContainer;
    private Map<String, Object> webIds = new HashMap<>();
    private String loginEndpoint;
    private String origin;
    private Boolean setupRootAcl;
    private Boolean disableDPoP;

    public TargetServer(final IRI subject) {
        super(subject, ConstructMode.DEEP);
        logger.debug("Retrieved {} statements for {}", super.size(), subject);
        features = getLiteralsAsStringSet(SOLID_TEST.features).stream()
                .collect(Collectors.toMap(Object::toString, f -> Boolean.TRUE));
        solidIdentityProvider = getIriAsString(SOLID.oidcIssuer);
        final String root = getIriAsString(SOLID_TEST.serverRoot);
        if (StringUtils.isEmpty(root)) {
            throw new TestHarnessInitializationException("serverRoot must be defined");
        }
        serverRoot = URI.create(root).resolve("/");
        maxThreads = getLiteralAsInt(SOLID_TEST.maxThreads);
        String testContainer = getLiteralAsString(SOLID_TEST.testContainer);
        if (StringUtils.isEmpty(testContainer)) {
            throw new TestHarnessInitializationException("testContainer must be defined");
        }
        if (!testContainer.endsWith("/")) {
            testContainer += "/";
        }
        this.testContainer = serverRoot.resolve(testContainer).toString();
        final String alice = getIriAsString(SOLID_TEST.aliceUser);
        final String bob = getIriAsString(SOLID_TEST.bobUser);
        if (alice != null) {
            webIds.put(HttpConstants.ALICE, alice);
        }
        if (bob != null) {
            webIds.put(HttpConstants.BOB, bob);
        }
        loginEndpoint = getIriAsString(SOLID.loginEndpoint);
        origin = getIriAsString(SOLID_TEST.origin);
        setupRootAcl = getLiteralAsBoolean(SOLID_TEST.setupRootAcl);
        // TODO: Add disableDPoP to vocab
        disableDPoP = false; //getLiteralAsBoolean(SOLID_TEST.disableDPoP);
    }

    public Map<String, Boolean> getFeatures() {
        return features;
    }

    public String getSolidIdentityProvider() {
        return solidIdentityProvider;
    }

    public URI getServerRoot() {
        return serverRoot;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public String getTestContainer() {
        return testContainer;
    }

    public Map<String, Object> getWebIds() {
        return webIds;
    }

    public String getLoginEndpoint() {
        return loginEndpoint;
    }

    public String getOrigin() {
        return origin;
    }

    public boolean isSetupRootAcl() {
        return setupRootAcl;
    }

    public boolean isDisableDPoP() {
        return disableDPoP;
    }
}
