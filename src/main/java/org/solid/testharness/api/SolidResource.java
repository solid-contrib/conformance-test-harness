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

import org.solid.testharness.accesscontrol.AccessDataset;
import org.solid.testharness.accesscontrol.AccessDatasetBuilder;
import org.solid.testharness.utils.SolidContainerProvider;
import org.solid.testharness.utils.SolidResourceProvider;

import java.net.URI;

/**
 * SolidResource is the base class that represents any resource (and by extension any container) in a Solid server.
 * It is an adapter designed to be used from tests written in Karate and the methods only use basic the types of String,
 * List, Map as these are automatically translated into the Javascript environment that runs the tests. The exceptions
 * to this rule are classes passed within the test safely such as <code>SolidResource</code>,
 * <code>SolidContainer</code>, <code>SolidClient</code>, and <code>AccessDataset</code>. Internally it maps
 * calls through to a <a href="#SolidResourceProvider">SolidResourceProvider</a> object.
 */
public class SolidResource {
    protected SolidResourceProvider solidResourceProvider;

    protected SolidResource(final SolidResourceProvider solidResourceProvider) {
        this.solidResourceProvider = solidResourceProvider;
    }

    /**
     * Create a new <code>SolidResource</code> at the provided <code>url</code>. The <code>body</code>
     * is uploaded to the resource with the content type set as <code>type</code>.
     * @param solidClient a <code>SolidClient</code> instance
     * @param url the URL for this resource
     * @param body the content of the resource
     * @param type the content type of the resource
     * @return the resource
     */
    public static SolidResource create(final SolidClient solidClient, final String url,
                                       final String body, final String type) {
        try {
            return new SolidResource(
                    new SolidResourceProvider(solidClient.solidClientProvider, URI.create(url), body, type)
            );
        } catch (Exception e) {
            throw new TestHarnessException("Failed to construct resource", e);
        }
    }

    /**
     * Returns the url of this resource.
     * @return the url
     */
    public String getUrl() {
        final URI url = solidResourceProvider.getUrl();
        return url != null ? url.toString() : null;
    }

    /**
     * Get the container for this resource.
     * @return A SolidContainer representing the container or null if the resource is the server root.
     */
    public SolidContainer getContainer() {
        final SolidContainerProvider solidContainerProvider = solidResourceProvider.getContainer();
        return solidContainerProvider != null ? new SolidContainer(solidContainerProvider) : null;
    }

    /**
     * Return the URL of the ACL related to this resource.
     * @return ACL url
     */
    public String getAclUrl() {
        try {
            final URI aclUrl = solidResourceProvider.getAclUrl();
            return aclUrl != null ? aclUrl.toString() : null;
        } catch (Exception e) {
            throw new TestHarnessException("Failed to get ACL url", e);
        }
    }

    /**
     * Search up the path hierarchy to attempt to find the container which is marked as the storage for this resource.
     * @return the SolidResource representing the storage container for this resource, if found.
     */
    public SolidResource findStorage() {
        try {
            final SolidResourceProvider storage = solidResourceProvider.findStorage();
            return storage != null ? new SolidResource(storage) : null;
        } catch (Exception e) {
            throw new TestHarnessException("Failed to find storage for this resource", e);
        }
    }

    /**
     * Returns a boolean indicating if the resource is a storage root.
     * @return <code>true</code> if the resource has a <code>pim:Storage</code> link; <code>false</code> otherwise.
     */
    public boolean isStorageType() {
        try {
            return solidResourceProvider.isStorageType();
        } catch (Exception e) {
            throw new TestHarnessException("Failed to find storage link header", e);
        }
    }

    /**
     * Return an <code>AccessDatasetBuilder</code>.
     * @return an access dataset builder
     */
    public AccessDatasetBuilder getAccessDatasetBuilder() {
        try {
            return solidResourceProvider.getAccessDatasetBuilder();
        } catch (Exception e) {
            throw new TestHarnessException("Failed to create AccessDatasetBuilder", e);
        }
    }

    /**
     * Return the <code>AccessDataset</code> associated with this resource.
     * @return an access dataset
     */
    public AccessDataset getAccessDataset() {
        try {
            return solidResourceProvider.getAccessDataset();
        } catch (Exception e) {
            throw new TestHarnessException("Failed to get the access dataset", e);
        }
    }

    /**
     * Apply an <code>AccessDataset</code> to the resource.
     * @param accessDataset the <code>AccessDataset</code> to be applied to the resource
     */
    public void setAccessDataset(final AccessDataset accessDataset) {
        try {
            solidResourceProvider.setAccessDataset(accessDataset);
        } catch (Exception e) {
            throw new TestHarnessException("Failed to apply the ACL", e);
        }
    }

    /**
     * Delete this resource (and any contents if it is a container).
     */
    public void delete() {
        try {
            solidResourceProvider.delete();
        } catch (Exception e) {
            throw new TestHarnessException("Failed to delete the resource", e);
        }
    }

    @Override
    public String toString() {
        if (solidResourceProvider.isContainer()) {
            return "SolidContainer: " + solidResourceProvider.getUrl();
        } else {
            return "SolidResource: " + solidResourceProvider.getUrl();
        }
    }
}
