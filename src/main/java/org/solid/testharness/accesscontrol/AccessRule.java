package org.solid.testharness.accesscontrol;

import org.eclipse.rdf4j.model.IRI;

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

    private URI target;
    private boolean inheritable;
    private AgentType type;
    private IRI agent;
    private List<IRI> members;
    private List<String> access;

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
                ? members.stream().map(m -> iri(m)).collect(Collectors.toList())
                : Collections.emptyList();
    }
}
