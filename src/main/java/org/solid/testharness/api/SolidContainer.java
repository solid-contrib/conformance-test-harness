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
package org.solid.testharness.api;

import org.solid.testharness.http.SolidClient;
import org.solid.testharness.utils.RDFUtils;
import org.solid.testharness.utils.SolidContainerProvider;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * SolidContainer is extended from <code>SolidResource</code> and represents any container in a Solid server.
 * It is an adapter designed to be used from tests written in Karate and the methods only use basic the types of String,
 * List, Map as these are automatically translated into the Javascript environment that runs the tests. The exceptions
 * to this rule are classes passed within the test safely such as <code>SolidResource</code>,
 * <code>SolidContainer</code>, <code>SolidClient</code>, and <code>AccessDataset</code>. Internally it maps calls
 * through to a <a href="#SolidContainerProvider">SolidContainerProvider</a> object.
 */
public final class SolidContainer extends SolidResource {
    private SolidContainerProvider solidContainerProvider;

    SolidContainer(final SolidContainerProvider solidContainerProvider) {
        super(solidContainerProvider);
        this.solidContainerProvider = solidContainerProvider;
    }

    /**
     * Create a <code>SolidContainer</code> from a <code>SolidContainerProvider</code>.
     * @param solidContainerProvider the <code>SolidContainerProvider</code> being adapted
     * @return the container
     */
    public static SolidContainer from(final SolidContainerProvider solidContainerProvider) {
        return new SolidContainer(solidContainerProvider);
    }

    /**
     * Create a new <code>SolidContainer</code>.
     * @param solidClient a <code>SolidClient</code> instance
     * @param url the URL for this container
     * @return the container
     */
    public static SolidContainer create(final SolidClient solidClient, final String url) {
        return new SolidContainer(new SolidContainerProvider(solidClient, URI.create(url)));
    }

    /**
     * Create the reserved container on the server.
     * @return the container
     */
    public SolidContainer instantiate() {
        solidContainerProvider.instantiate();
        return this;
    }

    /**
     * Reserve a new container as a child of this one using a random name.
     * @return the new container
     */
    public SolidContainer reserveContainer() {
        return SolidContainer.from(solidContainerProvider.reserveContainer(UUID.randomUUID().toString()));
    }

    /**
     * Create a new container as a child of this one using a random name.
     * @return the new container
     */
    public SolidContainer createContainer() {
        return SolidContainer.from(solidContainerProvider.reserveContainer(UUID.randomUUID().toString()).instantiate());
    }

    /**
     * Reserve a new resource as a child of this one using a random name and the provided suffix.
     * @param suffix the filename suffix to append to the generated resource name
     * @return the new resource
     */
    public SolidResource reserveResource(final String suffix) {
        return SolidResource.from(solidContainerProvider.reserveResource(UUID.randomUUID() + suffix));
    }

    /**
     * Create a new resource as a child of this one using a random name and the provided suffix. The <code>body</code>
     * is uploaded to the resource with the content type set as <code>type</code>.
     * @param suffix the filename suffix to append to the generated resource name
     * @param body the content of the resource
     * @param type the content type of the resource
     * @return the new resource
     */
    public SolidResource createResource(final String suffix, final String body, final String type) {
        return SolidResource.from(solidContainerProvider.createResource(UUID.randomUUID() + suffix, body, type));
    }

    /**
     * Get a list of the members of this container.
     * @return a list of members
     * @throws Exception
     */
    public List<String> listMembers() throws Exception {
        return RDFUtils.parseContainerContents(solidContainerProvider.getContentAsTurtle(), getUrl());
    }

    /**
     * @deprecated Use <code>RDFUtils.parseContainerContents(data, url)</code>
     * Given container contents, parse to extract the list of members.
     * @param data The contents of this resource in Turtle format
     * @return a list of members
     * @throws Exception
     */
    @Deprecated
    public List<String> parseMembers(final String data) throws Exception {
        return RDFUtils.parseContainerContents(data, getUrl());
    }

    /**
     * Delete the contents of this container and any of their contents recursively but leave this container.
     * @throws Exception
     */
    public void deleteContents() throws Exception {
        solidContainerProvider.deleteContents();
    }
}
