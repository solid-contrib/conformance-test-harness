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
import org.eclipse.rdf4j.model.util.Values;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.eclipse.rdf4j.model.util.Values.iri;

public class AccessRule {
    public enum AgentType {
        AUTHENTICATED_USER,
        PUBLIC,
        GROUP,
        AGENT
    }

    private final URI target;
    private final boolean inheritable;
    private final AgentType type;
    private final IRI agent;
    private final List<IRI> members;
    private final List<String> access;

    public IRI getTargetIri() {
        return target != null ? iri(target.toString()) : null;
    }

    public boolean isInheritable() {
        return inheritable;
    }

    public AgentType getType() {
        return type;
    }

    public IRI getAgent() {
        return agent;
    }

    public List<String> getAccess() {
        return access;
    }

    public List<IRI> getMembers() {
        return members;
    }

    public AccessRule(final URI target, final boolean inheritable, final AgentType type,
                      final String agent, final List<String> access, final List<String> members) {
        this.target = target;
        this.inheritable = inheritable;
        this.type = type;
        this.agent = agent != null ? iri(agent) : null;
        this.access = access != null ? access : Collections.emptyList();
        this.members = members != null
                ? members.stream().map(Values::iri).collect(Collectors.toList())
                : Collections.emptyList();
    }
}
