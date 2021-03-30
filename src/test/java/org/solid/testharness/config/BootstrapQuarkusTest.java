package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class BootstrapQuarkusTest {

    @Test
    void getInstance() {
        Bootstrap bootstrap = Bootstrap.getInstance();
        assertNotNull(bootstrap);
        Bootstrap bootstrap2 = Bootstrap.getInstance();
        assertNotNull(bootstrap2);
        assertEquals(bootstrap, bootstrap2);
    }

    @Test
    void getTestHarnessConfig() {
        Bootstrap bootstrap = Bootstrap.getInstance();
        assertNotNull(bootstrap.getTestHarnessConfig());
    }
}
