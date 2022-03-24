/*
 * MIT License
 *
 * Copyright (c) 2021 Solid
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
import org.junit.jupiter.api.Test;
import org.solid.testharness.config.TestSubject;
import org.solid.testharness.http.Client;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractAccessDatasetBuilderTest {
    private static final String TARGET = "https://example.org/target";
    private static final URI TARGET_URI = URI.create(TARGET);
    private static final String AGENT1 = "https://example.org/me";
    private static final String AGENT2 = "https://example.org/you";
    private static final List<String> MODES = List.of("read");

    @Test
    void setBaseUri() {
        final AbstractAccessDatasetBuilder builder = new TestAccessDatasetBuilder();
        builder.setBaseUri("https://example.org/base");
        assertEquals("https://example.org/base", ((TestAccessDatasetBuilder) builder).getBaseUri());
    }

    @Test
    void setAgentAccess() {
        final AbstractAccessDatasetBuilder builder = new TestAccessDatasetBuilder();
        final AccessRule rule = ((TestAccessDataset) builder.setAgentAccess(TARGET, AGENT1, MODES).build())
                .getAccessRules().get(0);
        assertThat(rule, samePropertyValuesAs(new AccessRule(TARGET_URI, false, AccessRule.AgentType.AGENT,
                AGENT1, MODES, Collections.emptyList())));
    }

    @Test
    void setGroupAccess() {
        final AbstractAccessDatasetBuilder builder = new TestAccessDatasetBuilder();
        final AccessRule rule = ((TestAccessDataset) builder.setGroupAccess(TARGET, List.of(AGENT1, AGENT2), MODES)
                .build()).getAccessRules().get(0);
        assertThat(rule, samePropertyValuesAs(new AccessRule(TARGET_URI, false, AccessRule.AgentType.GROUP,
                null, MODES, List.of(AGENT1, AGENT2))));
    }

    @Test
    void setPublicAccess() {
        final AbstractAccessDatasetBuilder builder = new TestAccessDatasetBuilder();
        final AccessRule rule = ((TestAccessDataset) builder.setPublicAccess(TARGET, MODES).build())
                .getAccessRules().get(0);
        assertThat(rule, samePropertyValuesAs(new AccessRule(TARGET_URI, false, AccessRule.AgentType.PUBLIC,
                null, MODES, Collections.emptyList())));
    }

    @Test
    void setAuthenticatedAccess() {
        final AbstractAccessDatasetBuilder builder = new TestAccessDatasetBuilder();
        final AccessRule rule = ((TestAccessDataset) builder.setAuthenticatedAccess(TARGET, MODES).build())
                .getAccessRules().get(0);
        assertThat(rule, samePropertyValuesAs(new AccessRule(TARGET_URI, false,
                AccessRule.AgentType.AUTHENTICATED_USER, null, MODES, Collections.emptyList())));
    }

    @Test
    void setAccessFor() {
        final AbstractAccessDatasetBuilder builder = new TestAccessDatasetBuilder();
        final AccessRule rule = ((TestAccessDataset) builder.setAccessFor(TARGET, AccessRule.AgentType.AGENT,
                AGENT2, MODES).build())
                .getAccessRules().get(0);
        assertThat(rule, samePropertyValuesAs(new AccessRule(TARGET_URI, false,
                AccessRule.AgentType.AGENT, AGENT2, MODES, Collections.emptyList())));
    }

    @Test
    void setAccessForGroup() {
        final AbstractAccessDatasetBuilder builder = new TestAccessDatasetBuilder();
        final AccessRule rule = ((TestAccessDataset) builder.setAccessFor(TARGET, AccessRule.AgentType.GROUP,
                null, MODES, List.of(AGENT2, AGENT1)).build())
                .getAccessRules().get(0);
        assertThat(rule, samePropertyValuesAs(new AccessRule(TARGET_URI, false,
                AccessRule.AgentType.GROUP, null, MODES, List.of(AGENT2, AGENT1))));
    }

    @Test
    void setInheritableAgentAccess() {
        final AbstractAccessDatasetBuilder builder = new TestAccessDatasetBuilder();
        final AccessRule rule = ((TestAccessDataset) builder.setInheritableAgentAccess(TARGET, AGENT1, MODES).build())
                .getAccessRules().get(0);
        assertThat(rule, samePropertyValuesAs(new AccessRule(TARGET_URI, true, AccessRule.AgentType.AGENT,
                AGENT1, MODES, Collections.emptyList())));
    }

    @Test
    void setInheritableGroupAccess() {
        final AbstractAccessDatasetBuilder builder = new TestAccessDatasetBuilder();
        final AccessRule rule = ((TestAccessDataset) builder.setInheritableGroupAccess(TARGET, List.of(AGENT1, AGENT2),
                        MODES)
                .build()).getAccessRules().get(0);
        assertThat(rule, samePropertyValuesAs(new AccessRule(TARGET_URI, true, AccessRule.AgentType.GROUP,
                null, MODES, List.of(AGENT1, AGENT2))));
    }

    @Test
    void setInheritablePublicAccess() {
        final AbstractAccessDatasetBuilder builder = new TestAccessDatasetBuilder();
        final AccessRule rule = ((TestAccessDataset) builder.setInheritablePublicAccess(TARGET, MODES).build())
                .getAccessRules().get(0);
        assertThat(rule, samePropertyValuesAs(new AccessRule(TARGET_URI, true, AccessRule.AgentType.PUBLIC,
                null, MODES, Collections.emptyList())));
    }

    @Test
    void setInheritableAuthenticatedAccess() {
        final AbstractAccessDatasetBuilder builder = new TestAccessDatasetBuilder();
        final AccessRule rule = ((TestAccessDataset) builder.setInheritableAuthenticatedAccess(TARGET, MODES).build())
                .getAccessRules().get(0);
        assertThat(rule, samePropertyValuesAs(new AccessRule(TARGET_URI, true,
                AccessRule.AgentType.AUTHENTICATED_USER, null, MODES, Collections.emptyList())));
    }

    @Test
    void setInheritableAccessFor() {
        final AbstractAccessDatasetBuilder builder = new TestAccessDatasetBuilder();
        final AccessRule rule = ((TestAccessDataset) builder.setInheritableAccessFor(TARGET, AccessRule.AgentType.AGENT,
                AGENT2, MODES).build())
                .getAccessRules().get(0);
        assertThat(rule, samePropertyValuesAs(new AccessRule(TARGET_URI, true,
                AccessRule.AgentType.AGENT, AGENT2, MODES, Collections.emptyList())));
    }

    @Test
    void setInheritableAccessForGroup() {
        final AbstractAccessDatasetBuilder builder = new TestAccessDatasetBuilder();
        final AccessRule rule = ((TestAccessDataset) builder.setInheritableAccessFor(TARGET, AccessRule.AgentType.GROUP,
                null, MODES, List.of(AGENT2, AGENT1)).build())
                .getAccessRules().get(0);
        assertThat(rule, samePropertyValuesAs(new AccessRule(TARGET_URI, true,
                AccessRule.AgentType.GROUP, null, MODES, List.of(AGENT2, AGENT1))));
    }
    @Test
    void build() {
        final AbstractAccessDatasetBuilder builder = new TestAccessDatasetBuilder();
        assertEquals(TestSubject.AccessControlMode.WAC, builder.build().getMode());
    }

    static class TestAccessDataset implements AccessDataset {
        final List<AccessRule> accessRules;

        TestAccessDataset(final List<AccessRule> accessRules) {
            this.accessRules = accessRules;
        }

        public List<AccessRule> getAccessRules() {
            return accessRules;
        }

        @Override
        public TestSubject.AccessControlMode getMode() {
            return TestSubject.AccessControlMode.WAC;
        }

        @Override
        public Model getModel() {
            return null;
        }

        @Override
        public void setModel(final Model model) {
        }

        @Override
        public void apply(final Client client, final URI uri) { }
    }

    static class TestAccessDatasetBuilder extends AbstractAccessDatasetBuilder {
        @Override
        protected AccessDataset internalBuild() {
            return new TestAccessDataset(accessRules);
        }

        @Override
        public AccessDatasetBuilder<TestAccessDataset> setOwnerAccess(final String target, final String owner) {
            return this;
        }

        public String getBaseUri() {
            return baseUri;
        }
    }
}