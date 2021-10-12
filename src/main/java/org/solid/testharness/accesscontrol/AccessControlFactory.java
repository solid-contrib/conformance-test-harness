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

import org.solid.testharness.config.Config;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;

@ApplicationScoped
public class AccessControlFactory {
    @Inject
    Config config;

    public AccessDatasetBuilder getAccessDatasetBuilder(final String uri) {
        if (Config.AccessControlMode.WAC.equals(config.getAccessControlMode())) {
            return new AccessDatasetWacBuilder().setBaseUri(uri);
        } else if (Config.AccessControlMode.ACP.equals(config.getAccessControlMode())) {
            return new AccessDatasetAcpBuilder().setBaseUri(uri);
        } else if (Config.AccessControlMode.ACP_LEGACY.equals(config.getAccessControlMode())) {
            return new AccessDatasetAcpLegacyBuilder().setBaseUri(uri);
        } else {
            return null;
        }
    }

    public AccessDataset createAccessDataset(final String acl, final URI baseUri) throws IOException {
        if (Config.AccessControlMode.WAC.equals(config.getAccessControlMode())) {
            return new AccessDatasetWac(acl, baseUri);
        } else if (Config.AccessControlMode.ACP.equals(config.getAccessControlMode())) {
            return new AccessDatasetAcp(acl, baseUri);
        } else if (Config.AccessControlMode.ACP_LEGACY.equals(config.getAccessControlMode())) {
            return new AccessDatasetAcpLegacy(acl, baseUri);
        } else {
            return null;
        }
    }
}
