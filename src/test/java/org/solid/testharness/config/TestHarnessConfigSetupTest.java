package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
@TestProfile(ConfigSetupTestProfile.class)
public class TestHarnessConfigSetupTest {

    @Inject
    TestHarnessConfig testHarnessConfig;

    @Test
    void getTargetServerNoConfig() {
        TargetServer targetServer = testHarnessConfig.getTargetServer();
        assertNull(targetServer);
    }

    @Test
    void getClientsNotRegistered() {
        assertNull(testHarnessConfig.getClients());
    }
}
