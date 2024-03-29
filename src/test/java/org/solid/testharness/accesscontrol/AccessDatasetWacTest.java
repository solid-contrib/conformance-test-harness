/*
 * MIT License
 *
 * Copyright (c) 2019 - 2022 W3C Solid Community Group
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.solid.testharness.accesscontrol;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.common.vocab.ACL;
import org.solid.testharness.config.TestSubject;
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
    private static final String ACL_URI_STR = "https://example.org/test.acl";
    private static final URI ACL_URI = URI.create(ACL_URI_STR);
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
        final AccessDataset accessDataset = new AccessDatasetWac(accessRules, ACL_URI_STR);
        assertTrue(Models.isomorphic(accessDataset.getModel(), testModel));
    }

    @Test
    void constructFromRulesThrows() {
        final List<AccessRule> accessRules = new ArrayList<>();
        accessRules.add(new AccessRule(RESOURCE_URI, false, AccessRule.AgentType.AGENT,
                AGENT1, List.of("controlRead"), null));
        assertThrows(RuntimeException.class, () -> new AccessDatasetWac(accessRules, ACL_URI_STR));
    }

    @Test
    void constructFromRulesNewMode() {
        final List<AccessRule> accessRules = new ArrayList<>();
        accessRules.add(new AccessRule(RESOURCE_URI, false, AccessRule.AgentType.AGENT,
                AGENT1, List.of("https://example.org/specialMode"), null));
        final AccessDataset accessDataset = new AccessDatasetWac(accessRules, ACL_URI_STR);
        assertTrue(accessDataset.getModel().contains(null, ACL.mode, iri("https://example.org/specialMode")));
    }

    @Test
    void constructFromDoc() throws Exception {
        final AccessDataset accessDataset = new AccessDatasetWac(
                TestUtils.loadStringFromFile("src/test/resources/utils/vcard.ttl"),
                ACL_URI
        );
        assertEquals(1, accessDataset.getModel().size());
    }

    @Test
    void apply() throws Exception {
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockVoidResponse = TestUtils.mockVoidResponse(200);
        when(mockClient.put(any(), any(), any())).thenReturn(mockVoidResponse);
        final AccessDataset accessDataset = new AccessDatasetWac(
                TestUtils.loadStringFromFile("src/test/resources/utils/vcard.ttl"), ACL_URI
        );
        assertDoesNotThrow(() -> accessDataset.apply(mockClient, RESOURCE_URI));
    }

    @Test
    void applyFails() throws Exception {
        final Client mockClient = mock(Client.class);
        final HttpResponse<Void> mockVoidResponse = TestUtils.mockVoidResponse(403);
        when(mockClient.put(any(), any(), any())).thenReturn(mockVoidResponse);
        final AccessDataset accessDataset = new AccessDatasetWac(
                TestUtils.loadStringFromFile("src/test/resources/utils/vcard.ttl"), ACL_URI
        );
        assertThrows(Exception.class, () -> accessDataset.apply(mockClient, RESOURCE_URI));
    }

    @Test
    void applyNull() {
        final Client mockClient = mock(Client.class);
        final AccessDataset accessDataset = new AccessDatasetWac(Collections.emptyList(), ACL_URI_STR);
        assertDoesNotThrow(() -> accessDataset.apply(mockClient, RESOURCE_URI));
    }

    @Test
    void getMode() {
        final AccessDataset accessDataset = new AccessDatasetWac(Collections.emptyList(), ACL_URI_STR);
        assertEquals(TestSubject.AccessControlMode.WAC, accessDataset.getMode());
    }

    @Test
    void getSetModel() {
        final AccessDataset accessDataset = new AccessDatasetWac(Collections.emptyList(), ACL_URI_STR);
        assertNull(accessDataset.getModel());
        accessDataset.setModel(new LinkedHashModel());
        assertNotNull(accessDataset.getModel());
    }
}