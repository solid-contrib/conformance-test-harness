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