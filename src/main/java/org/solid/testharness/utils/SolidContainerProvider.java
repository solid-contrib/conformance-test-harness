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
package org.solid.testharness.utils;

import org.solid.testharness.http.HttpUtils;
import org.solid.testharness.http.SolidClientProvider;

import java.net.URI;

public class SolidContainerProvider extends SolidResourceProvider {
    public SolidContainerProvider(final SolidClientProvider solidClientProvider, final URI url) {
        super(solidClientProvider, validateUrl(url), null, null);
    }

    private static URI validateUrl(final URI url) {
        if (url == null || !url.toString().endsWith("/")) {
            throw new IllegalArgumentException("A container url must end with /");
        }
        return url;
    }

    public SolidContainerProvider instantiate() throws Exception {
        solidClientProvider.createContainer(this.url);
        return this;
    }

    public SolidContainerProvider reserveContainer(final String name) {
        return new SolidContainerProvider(super.solidClientProvider, url.resolve(HttpUtils.ensureSlashEnd(name)));
    }

    public SolidResourceProvider reserveResource(final String name) {
        return new SolidResourceProvider(super.solidClientProvider, url.resolve(HttpUtils.ensureNoSlashEnd(name)));
    }

    public SolidResourceProvider createResource(final String name, final String body, final String type) {
        final URI childUrl = url.resolve(HttpUtils.ensureNoSlashEnd(name));
        return new SolidResourceProvider(super.solidClientProvider, childUrl, body, type);
    }

    public void deleteContents() throws Exception {
        solidClientProvider.deleteContentsRecursively(url);
    }
}
