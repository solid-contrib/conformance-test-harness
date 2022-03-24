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

import io.quarkus.arc.Unremovable;
import org.solid.testharness.config.TestSubject;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;

@ApplicationScoped
@Unremovable
public class AccessControlFactory {
    @Inject
    TestSubject testSubject;

    public AccessDatasetBuilder getAccessDatasetBuilder(final String uri) {
        if (TestSubject.AccessControlMode.WAC.equals(testSubject.getAccessControlMode())) {
            return new AccessDatasetWacBuilder().setBaseUri(uri);
        } else if (TestSubject.AccessControlMode.ACP.equals(testSubject.getAccessControlMode())) {
            return new AccessDatasetAcpBuilder().setBaseUri(uri);
        } else if (TestSubject.AccessControlMode.ACP_LEGACY.equals(testSubject.getAccessControlMode())) {
            return new AccessDatasetAcpLegacyBuilder().setBaseUri(uri);
        } else {
            return null;
        }
    }

    public AccessDataset createAccessDataset(final String acl, final URI baseUri) throws IOException {
        if (TestSubject.AccessControlMode.WAC.equals(testSubject.getAccessControlMode())) {
            return new AccessDatasetWac(acl, baseUri);
        } else if (TestSubject.AccessControlMode.ACP.equals(testSubject.getAccessControlMode())) {
            return new AccessDatasetAcp(acl, baseUri);
        } else if (TestSubject.AccessControlMode.ACP_LEGACY.equals(testSubject.getAccessControlMode())) {
            return new AccessDatasetAcpLegacy(acl, baseUri);
        } else {
            return null;
        }
    }
}
