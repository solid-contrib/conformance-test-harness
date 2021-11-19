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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.http.HttpUtils;
import org.solid.testharness.http.SolidClient;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

public class SolidContainerProvider extends SolidResourceProvider {
    private static final Logger logger = LoggerFactory.getLogger(SolidContainerProvider.class);

    public SolidContainerProvider(final SolidClient solidClient, final String url) throws IllegalArgumentException {
        super(solidClient, validateUrl(url), null, null);
    }

    public static SolidContainerProvider create(final SolidClient solidClient, final String url) {
        return new SolidContainerProvider(solidClient, url);
    }

    public List<String> listMembers() throws Exception {
        return parseMembers(solidClient.getContentAsTurtle(url));
    }

    public List<String> parseMembers(final String data) throws Exception {
        return solidClient.parseMembers(data, url);
    }

    private static String validateUrl(final String url) {
        if (url == null || !url.endsWith("/")) {
            throw new IllegalArgumentException("A container url must end with /");
        }
        return url;
    }

    public SolidContainerProvider instantiate() {
        try {
            final HttpResponse<String> response = solidClient.createContainer(this.url);
            if (HttpUtils.isSuccessful(response.statusCode())) {
                return this;
            } else {
                logger.error("Failed to create " + url.toString() + ", response = " + response.statusCode());
                logger.error("Response: " + response.body());
                return null;
            }
        } catch (Exception e) {
            logger.error("createContainer: " + url.toString() + " failed", e);
            return null;
        }
    }

    public SolidContainerProvider createContainer() {
        return new SolidContainerProvider(super.solidClient, url.resolve(UUID.randomUUID() + "/").toString())
                .instantiate();
    }

    public SolidContainerProvider reserveContainer() {
        return new SolidContainerProvider(super.solidClient, url.resolve(UUID.randomUUID() + "/").toString());
    }

    public SolidResourceProvider reserveResource(final String suffix) {
        return new SolidResourceProvider(super.solidClient, url.resolve(UUID.randomUUID() + suffix).toString());
    }

    public SolidResourceProvider createResource(final String suffix, final String body, final String type) {
        try {
            final URI childUrl = url.resolve(UUID.randomUUID() + suffix);
            logger.info("Create child in {}: {}", url, childUrl);
            return new SolidResourceProvider(super.solidClient, childUrl.toString(), body, type);
        } catch (Exception e) {
            logger.error("createResource in " + url.toString() + " failed", e);
        }
        return null;
    }

    public void deleteContents() throws Exception {
        solidClient.deleteContentsRecursively(url);
    }
}
