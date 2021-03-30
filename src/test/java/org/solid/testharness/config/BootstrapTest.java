package org.solid.testharness.config;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertNull;

class BootstrapTest {
    @Test
    void getInstance() {
        Bootstrap bootstrap = Bootstrap.getInstance();
        assertNull(bootstrap);
    }
}
