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
package org.solid.testharness.config;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.SOLID_TEST;
import org.solid.testharness.utils.DataModelBase;

import java.util.List;

public class TargetServer extends DataModelBase {
    private static final Logger logger = LoggerFactory.getLogger(TargetServer.class);

    private final List<String> features;
    private final List<String> skipTags;

    public TargetServer(final IRI subject) {
        super(subject, ConstructMode.DEEP);
        logger.debug("Retrieved {} statements for {}", super.size(), subject);
        features = getLiteralsAsStringList(SOLID_TEST.features);
        skipTags = getLiteralsAsStringList(SOLID_TEST.skip);
    }

    public List<String> getFeatures() {
        return features;
    }

    public List<String> getSkipTags() {
        return skipTags;
    }
}
