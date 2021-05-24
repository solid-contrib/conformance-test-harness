package org.solid.testharness.config;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigLogicTest {
    Config config = new Config();

    @Test
    void getServerRootNoSlash() {
        config.serverRoot = "https://localhost";
        assertEquals(URI.create("https://localhost/"), config.getServerRoot());
    }

    @Test
    void getServerRootWithSlash() {
        config.serverRoot = "https://localhost/";
        assertEquals(URI.create("https://localhost/"), config.getServerRoot());
    }

    @Test
    public void getTestContainerWithSlashes() {
        config.serverRoot = "https://localhost/";
        config.testContainer = "/test/";
        assertEquals("https://localhost/test/", config.getTestContainer());
    }

    @Test
    public void getTestContainerNoSlashes() {
        config.serverRoot = "https://localhost";
        config.testContainer = "test";
        assertEquals("https://localhost/test/", config.getTestContainer());
    }

    @Test
    public void getLoginEndpointNull() {
        config.loginEndpoint = Optional.empty();
        assertEquals(null, config.getLoginEndpoint());
    }
}
