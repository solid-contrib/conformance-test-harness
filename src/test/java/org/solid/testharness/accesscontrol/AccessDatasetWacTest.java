package org.solid.testharness.accesscontrol;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.common.vocab.ACL;
import org.solid.testharness.http.Client;
import org.solid.testharness.utils.TestUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessDatasetWacTest {
    private static final URI RESOURCE_URI = URI.create("https://example.org/test");
    private static final URI CONTAINER_URI = URI.create("https://example.org/test/");
    private static final String AGENT1 = "https://example.org/alice#me";
    private static final String AGENT2 = "https://example.org/bob#me";
    private static final URI ACL_URI = URI.create("https://example.org/test.acl");
    private Model testModel;

    @BeforeEach
    void setup() throws IOException {
        testModel = TestUtils.loadTurtleFromFile("src/test/resources/utils/wac.ttl");
    }

    @Test
    void constructFromRules() {
        final List<AccessRule> accessRules = new ArrayList<>();
        accessRules.add(new AccessRule(RESOURCE_URI, false, AccessRule.AgentType.AGENT,
                AGENT1, List.of("read","write","control"), null));
        accessRules.add(new AccessRule(CONTAINER_URI, true, AccessRule.AgentType.AGENT,
                AGENT1, List.of("read","write","control"), null));
        accessRules.add(new AccessRule(RESOURCE_URI, false, AccessRule.AgentType.GROUP,
                null, List.of("read"), List.of(AGENT1, AGENT2)));
        accessRules.add(new AccessRule(RESOURCE_URI, false, AccessRule.AgentType.PUBLIC,
                null, List.of("write"), null));
        accessRules.add(new AccessRule(RESOURCE_URI, false, AccessRule.AgentType.AUTHENTICATED_USER,
                null, List.of("append"), null));
        final AccessDataset accessDataset = new AccessDatasetWac(accessRules, ACL_URI.toString());
        assertTrue(Models.isomorphic(accessDataset.getModel(), testModel));
    }

    @Test
    void constructFromRulesThrows() {
        final List<AccessRule> accessRules = new ArrayList<>();
        accessRules.add(new AccessRule(RESOURCE_URI, false, AccessRule.AgentType.AGENT,
                AGENT1, List.of("controlRead"), null));
        assertThrows(RuntimeException.class, () -> new AccessDatasetWac(accessRules, ACL_URI.toString()));
    }

    @Test
    void constructFromRulesNewMode() {
        final List<AccessRule> accessRules = new ArrayList<>();
        accessRules.add(new AccessRule(RESOURCE_URI, false, AccessRule.AgentType.AGENT,
                AGENT1, List.of("https://example.org/specialMode"), null));
        final AccessDataset accessDataset = new AccessDatasetWac(accessRules, ACL_URI.toString());
        assertTrue(accessDataset.getModel().contains(null, ACL.mode, iri("https://example.org/specialMode")));
    }

    @Test
    void constructFromDoc() throws IOException {
        final AccessDataset accessDataset = new AccessDatasetWac(
                TestUtils.loadStringFromFile("src/test/resources/utils/vcard.ttl"),
                ACL_URI
        );
        assertEquals(1, accessDataset.getModel().size());
    }

    @Test
    void apply() throws IOException, InterruptedException {
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockVoidResponse = TestUtils.mockVoidResponse(200);
        when(mockClient.put(any(), any(), any())).thenReturn(mockVoidResponse);
        final AccessDataset accessDataset = new AccessDatasetWac(
                TestUtils.loadStringFromFile("src/test/resources/utils/vcard.ttl"), ACL_URI
        );
        assertTrue(accessDataset.apply(mockClient, RESOURCE_URI));
    }

    @Test
    void applyFails() throws IOException, InterruptedException {
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockVoidResponse = TestUtils.mockVoidResponse(403);
        when(mockClient.put(any(), any(), any())).thenReturn(mockVoidResponse);
        final AccessDataset accessDataset = new AccessDatasetWac(
                TestUtils.loadStringFromFile("src/test/resources/utils/vcard.ttl"), ACL_URI
        );
        assertFalse(accessDataset.apply(mockClient, RESOURCE_URI));
    }

    @Test
    void applyNull() throws IOException, InterruptedException {
        final Client mockClient = mock(Client.class);
        final AccessDataset accessDataset = new AccessDatasetWac(Collections.emptyList(), ACL_URI.toString());
        assertTrue(accessDataset.apply(mockClient, RESOURCE_URI));
    }

    @Test
    void getMode() {
        final AccessDataset accessDataset = new AccessDatasetWac(Collections.emptyList(), ACL_URI.toString());
        assertEquals("WAC", accessDataset.getMode());
    }

    @Test
    void getSetModel() {
        final AccessDataset accessDataset = new AccessDatasetWac(Collections.emptyList(), ACL_URI.toString());
        assertNull(accessDataset.getModel());
        accessDataset.setModel(new LinkedHashModel());
        assertNotNull(accessDataset.getModel());
    }
}