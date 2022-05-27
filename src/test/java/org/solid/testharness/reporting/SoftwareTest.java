/*
 * MIT License
 *
 * Copyright (c) 2019 - 2022 W3C Solid Community Group
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
package org.solid.testharness.reporting;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.model.datatypes.XMLDateTime;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.AbstractDataModelTests;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SoftwareTest extends AbstractDataModelTests {
    @Override
    public String getTestFile() {
        return "src/test/resources/reporting/software-testing-feature.ttl";
    }

    @Test
    void getSoftwareName() {
        final Software software = new Software(iri(NS, "software1"));
        assertEquals("SOFTWARE", software.getSoftwareName());
    }

    @Test
    void getDescription() {
        final Software software = new Software(iri(NS, "software1"));
        assertEquals("DESCRIPTION", software.getDescription());
    }

    @Test
    void getCreatedDate() {
        final Software software = new Software(iri(NS, "software1"));
        assertEquals(0, software.getCreatedDate().compareTo(new XMLDateTime(("2021-01-02T00:00:00.000Z"))));
    }

    @Test
    void getProgrammingLanguage() {
        final Software software = new Software(iri(NS, "software1"));
        assertEquals("LANG", software.getProgrammingLanguage());
    }

    @Test
    void getDeveloper() {
        final Software software = new Software(iri(NS, "software1"));
        assertEquals("https://example.org/profile/card/#us", software.getDeveloper());
    }

    @Test
    void getHomepage() {
        final Software software = new Software(iri(NS, "software1"));
        assertEquals("https://example.org/software1", software.getHomepage());
    }

    @Test
    void getRevision() {
        final Software software = new Software(iri(NS, "software1"));
        assertEquals("0.0.1-SNAPSHOT", software.getRevision());
    }

    @Test
    void getReleaseName() {
        final Software software = new Software(iri(NS, "software1"));
        assertEquals("RELEASE_NAME", software.getReleaseName());
    }

    @Test
    void getReleaseDate() {
        final Software software = new Software(iri(NS, "software1"));
        assertEquals(0, software.getReleaseDate().compareTo(new XMLDateTime(("2021-03-05T00:00:00.000Z"))));
    }

    @Test
    void getCreatedDateNull() {
        final Software software = new Software(iri(NS, "software2"));
        assertNull(software.getCreatedDate());
    }

    @Test
    void getCreatedDateAsStringNull() {
        final Software software = new Software(iri(NS, "software2"));
        assertEquals("", software.getCreatedDateAsDate());
    }

    @Test
    void getReleaseNameNull() {
        final Software software = new Software(iri(NS, "software2"));
        assertNull(software.getReleaseName());
    }

    @Test
    void getRevisionNull() {
        final Software software = new Software(iri(NS, "software2"));
        assertNull(software.getRevision());
    }
}
