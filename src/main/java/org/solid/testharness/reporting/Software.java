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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.datatypes.XMLDateTime;
import org.solid.common.vocab.DOAP;
import org.solid.testharness.utils.DataModelBase;

public class Software extends DataModelBase  {
    public Software(final IRI subject) {
        super(subject, ConstructMode.DEEP);
    }

    public String getSoftwareName() {
        return getLiteralAsString(DOAP.name);
    }

    public String getDescription() {
        return getLiteralAsString(DOAP.description);
    }

    public String getProgrammingLanguage() {
        return getLiteralAsString(DOAP.programming_language);
    }

    public String getDeveloper() {
        return getIriAsString(DOAP.developer);
    }

    public String getHomepage() {
        return getIriAsString(DOAP.homepage);
    }

    public XMLDateTime getCreatedDate() {
        return getLiteralAsDateTime(DOAP.created);
    }

    public String getCreatedDateAsDate() {
        return toDateString(getCreatedDate());
    }

    public String getReleaseName() {
        final IRI release = getAsIri(DOAP.release);
        if (release == null) {
            return null;
        }
        return getLiteralAsString(release, DOAP.name);
    }

    public String getRevision() {
        final IRI release = getAsIri(DOAP.release);
        if (release == null) {
            return null;
        }
        return getLiteralAsString(release, DOAP.revision);
    }

    public XMLDateTime getReleaseDate() {
        final IRI release = getAsIri(DOAP.release);
        if (release == null) {
            return null;
        }
        return getLiteralAsDateTime(release, DOAP.created);
    }

    public String getReleaseDateAsDate() {
        return toDateString(getReleaseDate());
    }

    private String toDateString(final XMLDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.toString().split("T")[0];
    }

}
