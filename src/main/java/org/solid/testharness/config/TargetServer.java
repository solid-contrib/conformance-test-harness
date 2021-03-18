package org.solid.testharness.config;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.SOLID;
import org.solid.common.vocab.SOLID_TEST;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TargetServer {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.config.TargetServer");

    private Model model;
    private IRI testSubject;

    private Map<String, Boolean> features;
    private String solidIdentityProvider;
    private String serverRoot;
    private Integer maxThreads;
    private String rootContainer;
    private String testContainer;
    private Map<String, UserCredentials> users;
    private Map<String, Object> webIds;
    private String loginPath;
    private String origin;
    private Integer aclCachePause = 0;
    private Boolean setupRootAcl;
    private Boolean disableDPoP = false;

    private static final String SERVER_GRAPH = "CONSTRUCT {<%s> ?p ?o. ?o ?p1 ?o1} WHERE {<%s> ?p ?o. OPTIONAL {?o ?p1 ?o1}}";

    public TargetServer(Repository repository, IRI testSubject) {
        try (RepositoryConnection conn = repository.getConnection()) {
            this.testSubject = testSubject;
            model = Repositories.graphQuery(repository, String.format(SERVER_GRAPH, testSubject, testSubject), r -> QueryResults.asModel(r));
            logger.debug("Loaded {} statements for {}", model.size(), testSubject);
        }
    }

    public IRI getTestSubject() {
        return testSubject;
    }

    public Map<String, Boolean> getFeatures() {
        if (features == null && model != null) {
            setFeatures(getLiteralsAsStringSet(SOLID_TEST.features).stream().collect(Collectors.toMap(Object::toString, f -> Boolean.TRUE)));
        }
        return features;
    }

    public void setFeatures(Map<String, Boolean> features) {
        this.features = features;
    }

    public String getSolidIdentityProvider() {
        if (solidIdentityProvider == null && model != null) {
            setSolidIdentityProvider(getIriAsString(SOLID.oidcIssuer));
        }
        return solidIdentityProvider;
    }

    public void setSolidIdentityProvider(String solidIdentityProvider) {
        this.solidIdentityProvider = solidIdentityProvider;
    }

    public String getServerRoot() {
        if (serverRoot == null && model != null) {
            setServerRoot(getIriAsString(SOLID_TEST.serverRoot));
        }
        return serverRoot;
    }

    public void setServerRoot(String serverRoot) {
        this.serverRoot = serverRoot;
    }

    public int getMaxThreads() {
        if (maxThreads == null && model != null) {
            setMaxThreads(getLiteralAsInt(SOLID_TEST.maxThreads));
        }
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        if (maxThreads <= 0) {
            throw new IllegalArgumentException("maxThreads must be >= 1");
        }
        this.maxThreads = maxThreads;
    }

    public String getRootContainer() {
        if (rootContainer == null && model != null) {
            setRootContainer(getIriAsString(SOLID_TEST.podRoot));
        }
        return rootContainer;
    }

    public void setRootContainer(String rootContainer) {
        this.rootContainer = rootContainer;
    }

    public String getTestContainer() {
        if (testContainer == null && model != null) {
            setTestContainer(getLiteralAsString(SOLID_TEST.testContainer));
        }
        return testContainer;
    }

    public void setTestContainer(String testContainer) {
        this.testContainer = testContainer;
    }

    public Map<String, Object> getWebIds() {
        return webIds;
    }

    public Map<String, UserCredentials> getUsers() {
        if (users == null && model != null) {
            Optional<Resource> aliceUser = Models.objectResource(model.filter(testSubject, SOLID_TEST.aliceUser, null));
            Optional<Resource> bobUser = Models.objectResource(model.filter(testSubject, SOLID_TEST.bobUser, null));
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

    public void setUsers(Map<String, UserCredentials> users) {
        this.users = users;
    }

    public String getLoginPath() {
        if (loginPath == null && model != null) {
            setLoginPath(getIriAsString(SOLID.loginEndpoint));
        }
        return loginPath;
    }

    public void setLoginPath(String loginPath) {
        this.loginPath = loginPath;
    }

    public String getOrigin() {
        if (origin == null && model != null) {
            setOrigin(getIriAsString(SOLID_TEST.origin));
        }
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public boolean isSetupRootAcl() {
        if (setupRootAcl == null && model != null) {
            setSetupRootAcl(getLiteralAsBoolean(SOLID_TEST.setupRootAcl));
        }
        return setupRootAcl;
    }

    public void setSetupRootAcl(boolean setupRootAcl) {
        this.setupRootAcl = setupRootAcl;
    }

    public boolean isDisableDPoP() {
        return disableDPoP;
    }

    public void setDisableDPoP(boolean disableDPoP) {
        this.disableDPoP = disableDPoP;
    }

    public int getAclCachePause() {
        return aclCachePause;
    }

    public void setAclCachePause(int aclCachePause) {
        if (aclCachePause <= 0) {
            throw new IllegalArgumentException("aclCachePause must be >= 0");
        }
        this.aclCachePause = aclCachePause;
    }

    private String getIriAsString(IRI predicate) {
        Set<Value> values = model.filter(testSubject, predicate, null).objects();
        if (values.size() > 0) {
            return values.iterator().next().stringValue();
        }
        return null;
    }

    private String getLiteralAsString(IRI predicate) {
        Optional<Literal> value = Models.getPropertyLiteral(model, testSubject, predicate);
        return value.map(Value::stringValue).orElse(null);
    }

    private Set<String> getLiteralsAsStringSet(IRI predicate) {
        Set<Literal> value = Models.getPropertyLiterals(model, testSubject, predicate);
        return value.stream().map(Value::stringValue).collect(Collectors.toSet());
    }

    private int getLiteralAsInt(IRI predicate) {
        Optional<Literal> value = Models.getPropertyLiteral(model, testSubject, predicate);
        return value.map(Literal::intValue).orElse(0);
    }

    private boolean getLiteralAsBoolean(IRI predicate) {
        Optional<Literal> value = Models.getPropertyLiteral(model, testSubject, predicate);
        return value.map(Literal::booleanValue).orElse(false);
    }
}
