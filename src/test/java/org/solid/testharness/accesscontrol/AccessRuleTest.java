package org.solid.testharness.accesscontrol;

import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

class AccessRuleTest {
    private static final String TARGET = "https://example.org/target";
    private static final String AGENT = "https://example.org/me";
    private static final String READ = "READ";

    @Test
    void getTargetIri() {
        final AccessRule accessRule = new AccessRule(URI.create(TARGET), false,
                AccessRule.AgentType.AGENT, AGENT, null, null);
        assertEquals(iri(TARGET), accessRule.getTargetIri());
    }

    @Test
    void getTargetIriNull() {
        final AccessRule accessRule = new AccessRule(null, false,
                AccessRule.AgentType.AGENT, AGENT, null, null);
        assertNull(accessRule.getTargetIri());
    }

    @Test
    void isInheritable() {
        final AccessRule accessRule = new AccessRule(null, true,
                AccessRule.AgentType.AGENT, AGENT, null, null);
        assertTrue(accessRule.isInheritable());
    }

    @Test
    void getType() {
        final AccessRule accessRule = new AccessRule(null, false,
                AccessRule.AgentType.AGENT, AGENT, null, null);
        assertEquals(AccessRule.AgentType.AGENT, accessRule.getType());
    }

    @Test
    void getAgent() {
        final AccessRule accessRule = new AccessRule(null, false,
                AccessRule.AgentType.AGENT, AGENT, null, null);
        assertEquals(iri(AGENT), accessRule.getAgent());
    }

    @Test
    void getAccess() {
        final AccessRule accessRule = new AccessRule(null, false,
                AccessRule.AgentType.AGENT, AGENT, List.of(READ), null);
        final List<String> access = accessRule.getAccess();
        assertEquals(1, access.size());
        assertEquals(READ, access.get(0));
    }

    @Test
    void getAccessNull() {
        final AccessRule accessRule = new AccessRule(null, false,
                AccessRule.AgentType.AGENT, AGENT, null, null);
        final List<String> access = accessRule.getAccess();
        assertEquals(0, access.size());
    }

    @Test
    void getMembers() {
        final AccessRule accessRule = new AccessRule(null, false,
                AccessRule.AgentType.AGENT, AGENT, null, List.of(AGENT));
        final List<IRI> members = accessRule.getMembers();
        assertEquals(1, members.size());
        assertEquals(iri(AGENT), members.get(0));
    }
}