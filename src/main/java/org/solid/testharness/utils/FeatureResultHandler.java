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
package org.solid.testharness.utils;

import com.intuit.karate.Suite;
import com.intuit.karate.core.FeatureResult;
import com.intuit.karate.report.Report;
import com.intuit.karate.report.SuiteReports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.config.PathMappings;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;

import static org.eclipse.rdf4j.model.util.Values.iri;

@ApplicationScoped
public class FeatureResultHandler implements SuiteReports {
    private static final Logger logger = LoggerFactory.getLogger(FeatureResultHandler.class);

    @Inject
    DataRepository dataRepository;
    @Inject
    PathMappings pathMappings;

    @Override
    @SuppressWarnings("java:S112")
    public Report featureReport(final Suite suite, final FeatureResult fr) {
        final String featurePath = fr.getDisplayName();
        final String featureIri = pathMappings.unmapFeaturePath(featurePath);
        final FeatureFileParser featureFileParser;
        try {
            featureFileParser = new FeatureFileParser(
                    fr.getFeature().getResource().getFile().toPath()
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to read feature file when reporting its results", e);
        }

        if (featureIri != null) {
            dataRepository.addFeatureResult(suite, fr, iri(featureIri), featureFileParser);
        } else {
            logger.warn("The feature {} could not be mapped back to an IRI", featurePath);
        }
        return SuiteReports.super.featureReport(suite, fr);
    }
}
