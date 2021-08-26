package org.solid.testharness.accesscontrol;

import org.junit.jupiter.api.Test;
import org.solid.common.vocab.ACL;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

class AccessDatasetWacBuilderTest {
    private static final String BASE = "https://example.org";
    private static final String TARGET = "https://example.org/target";
    private static final String AGENT = "https://example.org/me";

    @Test
    void setOwnerAccessResource() {
        final AccessDataset accessDataset = new AccessDatasetWacBuilder()
                .setBaseUri(BASE)
                .setOwnerAccess(TARGET, AGENT)
                .build();
        assertEquals(6, accessDataset.getModel().size());
        assertTrue(accessDataset.getModel().contains(null, ACL.accessTo, iri(TARGET)));
        assertFalse(accessDataset.getModel().contains(null, ACL.default_, iri(TARGET)));
    }

    @Test
    void setOwnerAccessContainer() {
        final AccessDataset accessDataset = new AccessDatasetWacBuilder()
                .setBaseUri(BASE)
                .setOwnerAccess(TARGET + "/", AGENT)
                .build();
        assertEquals(12, accessDataset.getModel().size());
        assertTrue(accessDataset.getModel().contains(null, ACL.accessTo, iri(TARGET + "/")));
        assertTrue(accessDataset.getModel().contains(null, ACL.default_, iri(TARGET + "/")));
    }
}