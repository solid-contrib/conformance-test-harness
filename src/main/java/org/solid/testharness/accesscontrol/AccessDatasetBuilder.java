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

import java.util.List;

public interface AccessDatasetBuilder<T extends AccessDataset> {
    AccessDatasetBuilder<T> setOwnerAccess(String target, String owner);
    AccessDatasetBuilder<T> setBaseUri(String uri);

    AccessDatasetBuilder<T> setAgentAccess(String target, String webId, List<String> access);
    AccessDatasetBuilder<T> setGroupAccess(String target, List<String> members, List<String> access);
    AccessDatasetBuilder<T> setPublicAccess(String target, List<String> access);
    AccessDatasetBuilder<T> setAuthenticatedAccess(String target, List<String> access);
    AccessDatasetBuilder<T> setAccessFor(String target, AccessRule.AgentType actorType, String actor,
                                         List<String> access, List<String> members);

    AccessDatasetBuilder<T> setInheritableAgentAccess(String target, String webId, List<String> access);
    AccessDatasetBuilder<T> setInheritableGroupAccess(String target, List<String> members, List<String> access);
    AccessDatasetBuilder<T> setInheritablePublicAccess(String target, List<String> access);
    AccessDatasetBuilder<T> setInheritableAuthenticatedAccess(String target, List<String> access);
    AccessDatasetBuilder<T> setInheritableAccessFor(String target, AccessRule.AgentType actorType, String actor,
                                                    List<String> access, List<String> members);

    T build();
}