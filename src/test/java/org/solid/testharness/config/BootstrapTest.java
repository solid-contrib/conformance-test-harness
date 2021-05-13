package org.solid.testharness.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class BootstrapTest {
    @Test
    void getInstance() {
        final Bootstrap bootstrap = Bootstrap.getInstance();
        assertNull(bootstrap);
    }
}
