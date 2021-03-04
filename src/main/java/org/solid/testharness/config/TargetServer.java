package org.solid.testharness.config;

import java.util.Map;

public class TargetServer {
    private Map<String, Boolean> features;
    private String solidIdentityProvider;
    private String serverRoot;
    private int maxThreads;
    private String rootContainer;
    private String testContainer;
    private Map<String, UserCredentials> users;
    private String loginPath;
    private String origin;
    private int aclCachePause;
    private boolean setupRootAcl;

    public Map<String, Boolean> getFeatures() {
        return features;
    }

    public void setFeatures(Map<String, Boolean> features) {
        this.features = features;
    }

    public String getSolidIdentityProvider() {
        return solidIdentityProvider;
    }

    public void setSolidIdentityProvider(String solidIdentityProvider) {
        this.solidIdentityProvider = solidIdentityProvider;
    }

    public String getServerRoot() {
        return serverRoot;
    }

    public void setServerRoot(String serverRoot) {
        this.serverRoot = serverRoot;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public String getRootContainer() {
        return rootContainer;
    }

    public void setRootContainer(String rootContainer) {
        this.rootContainer = rootContainer;
    }

    public String getTestContainer() {
        return testContainer;
    }

    public void setTestContainer(String testContainer) {
        this.testContainer = testContainer;
    }

    public Map<String, UserCredentials> getUsers() {
        return users;
    }

    public void setUsers(Map<String, UserCredentials> users) {
        this.users = users;
    }

    public String getLoginPath() {
        return loginPath;
    }

    public void setLoginPath(String loginPath) {
        this.loginPath = loginPath;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public int getAclCachePause() {
        return aclCachePause;
    }

    public void setAclCachePause(int aclCachePause) {
        this.aclCachePause = aclCachePause;
    }

    public boolean isSetupRootAcl() {
        return setupRootAcl;
    }

    public void setSetupRootAcl(boolean setupRootAcl) {
        this.setupRootAcl = setupRootAcl;
    }


}
