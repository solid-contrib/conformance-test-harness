package org.solid.testharness.accesscontrol;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.solid.testharness.config.Config;
import org.solid.testharness.http.SolidClient;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class AccessControlFactoryTest {
    private static final String BASE = "https://example.org";

    @Inject
    Config config;

    @Inject
    AccessControlFactory accessControlFactory;

    @Test
    void getAccessDatasetBuilderWac() {
        config.setAccessControlMode(SolidClient.WAC_MODE);
        final AccessDatasetBuilder accessDatasetBuilder = accessControlFactory.getAccessDatasetBuilder(BASE);
        assertEquals(SolidClient.WAC_MODE, accessDatasetBuilder.build().getMode());
    }

    @Test
    void getAccessDatasetBuilderAcp() {
        config.setAccessControlMode(SolidClient.ACP_MODE);
        final AccessDatasetBuilder accessDatasetBuilder = accessControlFactory.getAccessDatasetBuilder(BASE);
        assertEquals(SolidClient.ACP_MODE, accessDatasetBuilder.build().getMode());
    }

    @Test
    void getAccessDatasetBuilderNull() {
        assertNull(accessControlFactory.getAccessDatasetBuilder(BASE));
    }
}