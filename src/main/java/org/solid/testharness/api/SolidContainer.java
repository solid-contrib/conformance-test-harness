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

import org.solid.testharness.utils.SolidContainerProvider;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * SolidContainer is extended from <code>SolidResource</code> and represents any container in a Solid server.
 * It is an adapter designed to be used from tests written in Karate and the methods only use basic the types of String,
 * List, Map as these are automatically translated into the Javascript environment that runs the tests. The exceptions
 * to this rule are classes passed within the test safely such as <code>SolidResource</code>,
 * <code>SolidContainer</code>, <code>SolidClient</code>, and <code>AccessDataset</code>. Internally it maps
 * calls through to a <a href="#SolidContainerProvider">SolidContainerProvider</a> object.
 */
public final class SolidContainer extends SolidResource {
    private SolidContainerProvider solidContainerProvider;

    public SolidContainer(final SolidContainerProvider solidContainerProvider) {
        super(solidContainerProvider);
        this.solidContainerProvider = solidContainerProvider;
    }

    /**
     * Create a new <code>SolidContainer</code>.
     * @param solidClient a <code>SolidClient</code> instance
     * @param url the URL for this container
     * @return the container
     */
    public static SolidContainer create(final SolidClient solidClient, final String url) {
        try {
            return new SolidContainer(
                    new SolidContainerProvider(solidClient.solidClientProvider, URI.create(url))
            );
        } catch (Exception e) {
            throw new TestHarnessException("Failed to construct container", e);
        }
    }

    /**
     * Create the reserved container on the server.
     * @return the container
     */
    public SolidContainer instantiate() {
        try {
            solidContainerProvider.instantiate();
            return this;
        } catch (Exception e) {
            throw new TestHarnessException("Failed to instantiate container", e);
        }
    }

    /**
     * Reserve a new container as a child of this one using a random name.
     * @return the new container
     */
    public SolidContainer reserveContainer() {
        try {
            return new SolidContainer(solidContainerProvider.reserveContainer(UUID.randomUUID().toString()));
        } catch (Exception e) {
            throw new TestHarnessException("Failed to reserve container", e);
        }
    }

    /**
     * Create a new container as a child of this one using a random name.
     * @return the new container
     */
    public SolidContainer createContainer() throws TestHarnessException {
        try {
            return new SolidContainer(solidContainerProvider.reserveContainer(UUID.randomUUID().toString())
                    .instantiate());
        } catch (Exception e) {
            throw new TestHarnessException("Failed to create container", e);
        }
    }

    /**
     * Reserve a new resource as a child of this one using a random name and the provided suffix.
     * @param suffix the filename suffix to append to the generated resource name
     * @return the new resource
     */
    public SolidResource reserveResource(final String suffix) {
        try {
            return new SolidResource(solidContainerProvider.reserveResource(UUID.randomUUID() + suffix));
        } catch (Exception e) {
            throw new TestHarnessException("Failed to reserve resource", e);
        }
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
        try {
            return new SolidResource(solidContainerProvider.createResource(UUID.randomUUID() + suffix, body, type));
        } catch (Exception e) {
            throw new TestHarnessException("Failed to create resource", e);
        }
    }

    /**
     * Get a list of the members of this container.
     * @return a list of members
     */
    public List<String> listMembers() {
        try {
            return RDFUtils.parseContainerContents(solidContainerProvider.getContentAsTurtle(), getUrl());
        } catch (Exception e) {
            throw new TestHarnessException("Failed to get container member listing", e);
        }
    }

    /**
     * @deprecated Use <code>RDFUtils.parseContainerContents(data, url)</code>
     * Given container contents, parse to extract the list of members.
     * @param data The contents of this resource in Turtle format
     * @return a list of members
     */
    @Deprecated
    public List<String> parseMembers(final String data) {
        try {
            return RDFUtils.parseContainerContents(data, getUrl());
        } catch (Exception e) {
            throw new TestHarnessException("Failed to parse container contents", e);
        }
    }

    /**
     * Delete the contents of this container and any of their contents recursively but leave this container.
     */
    public void deleteContents() {
        try {
            solidContainerProvider.deleteContents();
        } catch (Exception e) {
            throw new TestHarnessException("Failed to delete container contents", e);
        }
    }
}
