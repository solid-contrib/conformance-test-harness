package org.solid.testharness.accesscontrol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class AccessDatasetAcpBuilderTest {
    private static final String BASE = "https://example.org";
    private static final String TARGET = "https://example.org/target";
    private static final String AGENT = "https://example.org/me";

    @Test
    void setOwnerAccess() {
        final AccessDataset accessDataset = new AccessDatasetAcpBuilder()
                .setBaseUri(BASE)
                .setOwnerAccess(TARGET, AGENT)
                .build();
        assertNull(accessDataset.getModel());
    }
}