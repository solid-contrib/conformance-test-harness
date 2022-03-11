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
import org.solid.common.vocab.FOAF;
import org.solid.common.vocab.RDF;
import org.solid.common.vocab.VCARD;
import org.solid.testharness.config.TestSubject;
import org.solid.testharness.http.Client;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.http.HttpUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.eclipse.rdf4j.model.util.Values.bnode;
import static org.eclipse.rdf4j.model.util.Values.iri;

public class AccessDatasetWac implements AccessDataset {
    static final Map<String, IRI> standardModes = Map.of(
            READ, ACL.Read,
            WRITE, ACL.Write,
            APPEND, ACL.Append,
            CONTROL, ACL.Control,
            CONTROL_READ, ACL.Control,
            CONTROL_WRITE, ACL.Control
    );

    Model model;

    @SuppressWarnings({"PMD.UnusedFormalParameter", "PMD.SwitchStmtsShouldHaveDefault"})
    // aclUri is only used in the ACP implementation
    // switch uses an enum so cannot test a default option
    public AccessDatasetWac(final List<AccessRule> accessRules, final String aclUri) {
        if (!accessRules.isEmpty()) {
            final ModelBuilder builder = new ModelBuilder();
            builder.setNamespace(ACL.PREFIX, ACL.NAMESPACE);
            if (accessRules.stream().anyMatch(rule -> rule.getType() == AccessRule.AgentType.PUBLIC)) {
                builder.setNamespace(FOAF.PREFIX, FOAF.NAMESPACE);
            }
            if (accessRules.stream().anyMatch(rule -> rule.getType() == AccessRule.AgentType.GROUP)) {
                builder.setNamespace(VCARD.PREFIX, VCARD.NAMESPACE);
            }
            // TODO: Pair rules if same agent and modes?
            accessRules.forEach(rule -> {
                if (rule.getAccess().contains(CONTROL_READ) ^ rule.getAccess().contains(CONTROL_WRITE)) {
                    throw new RuntimeException("For WAC, you must either use Control, or " +
                            "ControlRead and ControlWrite must be the same");
                }
                builder.subject(bnode())
                        .add(RDF.type, ACL.Authorization)
                        .add(rule.isInheritable() ? ACL.default_ : ACL.accessTo, rule.getTargetIri());
                // Jacoco does not report switch coverage correctly
                switch (rule.getType()) {
                    case AGENT:
                        builder.add(ACL.agent, rule.getAgent());
                        break;
                    case PUBLIC:
                        builder.add(ACL.agentClass, FOAF.Agent);
                        break;
                    case AUTHENTICATED_USER:
                        builder.add(ACL.agentClass, ACL.AuthenticatedAgent);
                        break;
                    case GROUP:
                        final BNode group = bnode();
                        builder.add(ACL.agentGroup, group);
                        builder.add(group, RDF.type, VCARD.Group);
                        rule.getMembers().forEach(m -> builder.add(group, VCARD.hasMember, m));
                        break;
                }
                rule.getAccess().stream()
                        .map(mode -> standardModes.containsKey(mode) ? standardModes.get(mode) : iri(mode))
                        .distinct()
                        .forEach(mode -> builder.add(ACL.mode, mode));
                // TODO support other types of rules
            });
            model = builder.build();
        }
    }

    public AccessDatasetWac(final String acl, final URI baseUri) throws IOException {
        parseTurtle(acl, baseUri.toString());
    }

    @Override
    public void apply(final Client client, final URI uri) throws Exception {
        if (model != null) {
            final HttpResponse<Void> response = client.put(uri, asTurtle(),
                    HttpConstants.MEDIA_TYPE_TEXT_TURTLE);
            if (!HttpUtils.isSuccessful(response.statusCode())) {
                throw new Exception("Error response=" + response.statusCode() + " trying to apply ACL");
            }
        }
    }

    @Override
    public TestSubject.AccessControlMode getMode() {
        return TestSubject.AccessControlMode.WAC;
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
