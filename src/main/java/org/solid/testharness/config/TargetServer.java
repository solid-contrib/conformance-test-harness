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
package org.solid.testharness.config;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.SOLID_TEST;
import org.solid.testharness.utils.DataModelBase;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.validation.constraints.Positive;
import java.util.Map;
import java.util.stream.Collectors;

public class TargetServer extends DataModelBase {
    private static final Logger logger = LoggerFactory.getLogger(TargetServer.class);

    private Map<String, Boolean> features;
    @Positive(message = "maxThreads must be >= 1")
    private Integer maxThreads;
    private String origin;
    private Boolean setupRootAcl;
    private Boolean disableDPoP;

    public TargetServer(final IRI subject, final String origin, final Map<String, Boolean> features) {
        super(subject, ConstructMode.DEEP);
        this.origin = origin;
        this.features = features;
        this.maxThreads = 8;
        this.setupRootAcl = false;
        this.disableDPoP = false;
    }

    public TargetServer(final IRI subject) {
        super(subject, ConstructMode.DEEP);
        logger.debug("Retrieved {} statements for {}", super.size(), subject);
        features = getLiteralsAsStringSet(SOLID_TEST.features).stream()
                .collect(Collectors.toMap(Object::toString, f -> Boolean.TRUE));
        maxThreads = getLiteralAsInt(SOLID_TEST.maxThreads);
        if (maxThreads <= 0) {
            throw new TestHarnessInitializationException("maxThreads must be >= 1");
        }
        origin = getIriAsString(SOLID_TEST.origin);
        setupRootAcl = getLiteralAsBoolean(SOLID_TEST.setupRootAcl);
        // TODO: Add disableDPoP to vocab
        disableDPoP = false; //getLiteralAsBoolean(SOLID_TEST.disableDPoP);
    }

    public Map<String, Boolean> getFeatures() {
        return features;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public String getOrigin() {
        return origin;
    }

    public boolean isSetupRootAcl() {
        return setupRootAcl;
    }

    public boolean isDisableDPoP() {
        return disableDPoP;
    }
}
