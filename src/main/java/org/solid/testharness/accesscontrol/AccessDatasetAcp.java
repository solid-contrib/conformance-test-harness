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

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.solid.common.vocab.ACL;
import org.solid.common.vocab.ACP;
import org.solid.common.vocab.RDF;
import org.solid.common.vocab.VCARD;
import org.solid.testharness.config.Config;
import org.solid.testharness.http.Client;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.http.HttpUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.eclipse.rdf4j.model.util.Values.bnode;
import static org.eclipse.rdf4j.model.util.Values.iri;

public class AccessDatasetAcp implements AccessDataset {
    static final Map<String, IRI> standardModes = Map.of(
            READ, ACL.Read,
            WRITE, ACL.Write,
            APPEND, ACL.Append
    );
    static final Set<String> controlModes = Set.of(CONTROL, CONTROL_READ, CONTROL_WRITE);
    static final String PREFIX = "acr";

    Model model;

    public AccessDatasetAcp(final List<AccessRule> accessRules, final String aclUri) {
        final String namespace = aclUri + "#";
        final IRI accessControlResource = iri(aclUri);
        if (!accessRules.isEmpty()) {
            final ModelBuilder builder = new ModelBuilder();
            // add document namespace and any others needed
            builder.setNamespace(PREFIX, namespace);
            builder.setNamespace(ACP.PREFIX, ACP.NAMESPACE);
            builder.setNamespace(ACL.PREFIX, ACL.NAMESPACE);
            if (accessRules.stream().anyMatch(rule -> rule.getType() == AccessRule.AgentType.GROUP)) {
                builder.setNamespace(VCARD.PREFIX, VCARD.NAMESPACE);
            }

            final AtomicInteger i = new AtomicInteger();
            accessRules.forEach(rule -> {
                i.addAndGet(1);
                final List<String> accessModes = rule.getAccess().stream()
                        .filter(mode -> !controlModes.contains(mode))
                        .collect(Collectors.toList());
                final List<String> controlAccessModes = extractControlModes(rule);
                final IRI accessControlNode = iri(namespace, "accessControl" + i);
                final IRI policyNode = iri(namespace, "policy" + i);
                final IRI ruleNode = iri(namespace, "rule" + i);
                builder.add(accessControlResource, rule.isInheritable() ? ACP.memberAccessControl : ACP.accessControl,
                                accessControlNode)
                        .subject(accessControlNode)
                        .add(RDF.type, ACP.AccessControl)
                        .add(ACP.apply, policyNode)
                        .add(policyNode, RDF.type, ACP.Policy)
                        .add(policyNode, ACP.allOf, ruleNode)
                        .add(ruleNode, RDF.type, ACP.Matcher);
                if (!accessModes.isEmpty()) {
                    accessModes.stream()
                            .map(mode -> standardModes.containsKey(mode) ? standardModes.get(mode) : iri(mode))
                            .distinct()
                            .forEach(mode -> builder.add(policyNode, ACP.allow, mode));
                }
                if (!controlAccessModes.isEmpty()) {
                    final IRI accessPolicyNode = iri(namespace, "accessPolicy" + i);
                    builder.add(accessControlNode, ACP.access, accessPolicyNode)
                            .add(accessPolicyNode, RDF.type, ACP.Policy)
                            .add(accessPolicyNode, ACP.allOf, ruleNode);
                    controlAccessModes.stream()
                            .map(standardModes::get)
                            .forEach(mode -> builder.add(accessPolicyNode, ACP.allow, mode));
                }
                // Jacoco does not report switch coverage correctly
                switch (rule.getType()) {
                    case AGENT:
                        builder.add(ruleNode, ACP.agent, rule.getAgent());
                        break;
                    case PUBLIC:
                        builder.add(ruleNode, ACP.agent, ACP.PublicAgent);
                        break;
                    case AUTHENTICATED_USER:
                        builder.add(ruleNode, ACP.agent, ACP.AuthenticatedAgent);
                        break;
                    case GROUP:
                        final BNode group = bnode();
                        builder.add(ruleNode, ACP.group, group);
                        builder.add(group, RDF.type, VCARD.Group);
                        rule.getMembers().forEach(m -> builder.add(group, VCARD.hasMember, m));
                        break;
                    default:
                        break;
                }
                // TODO support other types of rules
            });
            model = builder.build();
        }
    }

    private List<String> extractControlModes(final AccessRule rule) {
        final List<String> controlModes = new ArrayList<>();
        if (rule.getAccess().contains(CONTROL)) {
            if (rule.getAccess().contains(CONTROL_READ) || rule.getAccess().contains(CONTROL_WRITE)) {
                throw new RuntimeException("For ACP, you cannot use Control and either of " +
                        "ControlRead or ControlWrite");
            } else {
                // in ACP CONTROL can be a shorthand for CONTROL_READ and CONTROL_WRITE
                controlModes.addAll(List.of(READ, WRITE));
            }
        } else {
            if (rule.getAccess().contains(CONTROL_READ)) {
                controlModes.add(READ);
            }
            if (rule.getAccess().contains(CONTROL_WRITE)) {
                controlModes.add(WRITE);
            }
        }
        return controlModes;
    }

    public AccessDatasetAcp(final String acl, final URI baseUri) throws IOException {
        parseTurtle(acl, baseUri.toString());
    }

    @Override
    public void apply(final Client client, final URI uri) throws Exception {
        if (model != null) {
            final HttpResponse<String> response = client.patch(uri, asSparqlInsert(),
                    HttpConstants.MEDIA_TYPE_APPLICATION_SPARQL_UPDATE);
            if (!HttpUtils.isSuccessful(response.statusCode())) {
                throw new Exception("Error response=" + response.statusCode() + " trying to apply ACL");
            }
        }
    }

    @Override
    public Config.AccessControlMode getMode() {
        return Config.AccessControlMode.ACP;
    }

    @Override
    public Model getModel() {
        return model;
    }

    @Override
    public void setModel(final Model model) {
        this.model = model;
    }
}
