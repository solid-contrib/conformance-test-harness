package org.solid.testharness.config;

import java.util.Map;

public class TestHarnessConfig {
    private String target;
    private Map<String, TargetServer> servers;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Map<String, TargetServer> getServers() {
        return servers;
    }

    public void setServers(Map<String, TargetServer> servers) {
        this.servers = servers;
    }
}
