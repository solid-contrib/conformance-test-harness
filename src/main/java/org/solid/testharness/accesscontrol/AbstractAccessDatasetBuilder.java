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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAccessDatasetBuilder<T extends AccessDataset> implements AccessDatasetBuilder<T> {
    final List<AccessRule> accessRules = new ArrayList<>();
    final List<String> ownerAccess = List.of("read", "write", "control");
    String baseUri;

    @Override
    public AccessDatasetBuilder<T> setBaseUri(final String uri) {
        this.baseUri = uri;
        return this;
    }

    @Override
    public AccessDatasetBuilder<T> setAgentAccess(final String target, final String webId, final List<String> access) {
        return setAccessFor(target, AccessRule.AgentType.AGENT, webId, access);
    }

    @Override
    public AccessDatasetBuilder<T> setGroupAccess(final String target, final List<String> members,
                                                  final List<String> access) {
        return setAccessFor(target, AccessRule.AgentType.GROUP, null, access, members);
    }

    @Override
    public AccessDatasetBuilder<T> setPublicAccess(final String target, final List<String> access) {
        return setAccessFor(target, AccessRule.AgentType.PUBLIC, null, access);
    }

    @Override
    public AccessDatasetBuilder<T> setAuthenticatedAccess(final String target, final List<String> access) {
        return setAccessFor(target, AccessRule.AgentType.AUTHENTICATED_USER, null, access);
    }

    public AccessDatasetBuilder<T> setAccessFor(final String target, final AccessRule.AgentType actorType,
                                                final String actor, final List<String> access) {
        return setAccessFor(target, actorType, actor, access, null);
    }

    @Override
    public AccessDatasetBuilder<T> setAccessFor(final String target, final AccessRule.AgentType actorType,
                                                final String actor, final List<String> access,
                                                final List<String> members) {
        accessRules.add(new AccessRule(URI.create(target), false, actorType, actor, access, members));
        return this;
    }

    @Override
    public AccessDatasetBuilder<T> setInheritableAgentAccess(final String target, final String webId,
                                                             final List<String> access) {
        return setInheritableAccessFor(target, AccessRule.AgentType.AGENT, webId, access);
    }

    @Override
    public AccessDatasetBuilder<T> setInheritableGroupAccess(final String target, final List<String> members,
                                                             final List<String> access) {
        return setInheritableAccessFor(target, AccessRule.AgentType.GROUP, null, access, members);
    }

    @Override
    public AccessDatasetBuilder<T> setInheritablePublicAccess(final String target, final List<String> access) {
        return setInheritableAccessFor(target, AccessRule.AgentType.PUBLIC, null, access);
    }

    @Override
    public AccessDatasetBuilder<T> setInheritableAuthenticatedAccess(final String target, final List<String> access) {
        return setInheritableAccessFor(target, AccessRule.AgentType.AUTHENTICATED_USER, null, access);
    }

    public AccessDatasetBuilder<T> setInheritableAccessFor(final String target, final AccessRule.AgentType actorType,
                                                           final String actor, final List<String> access) {
        return setInheritableAccessFor(target, actorType, actor, access, null);
    }

    @Override
    public AccessDatasetBuilder<T> setInheritableAccessFor(final String target, final AccessRule.AgentType actorType,
                                                           final String actor, final List<String> access,
                                                           final List<String> members) {
        accessRules.add(new AccessRule(URI.create(target), true, actorType, actor, access, members));
        return this;
    }

    @Override
    public T build() {
        return internalBuild();
    }

    protected abstract T internalBuild();
}
