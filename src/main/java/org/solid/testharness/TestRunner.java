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
package org.solid.testharness;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.solid.testharness.reporting.TestSuiteResults;
import org.solid.testharness.utils.FeatureResultHandler;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class TestRunner {
    @Inject
    FeatureResultHandler featureResultHandler;

    @SuppressWarnings("unchecked")
    // Unavoidable as Runner.builder().path() takes a list or vararg of Strings
    public TestSuiteResults runTests(final List<String> featurePaths, final int threads,
                                     final boolean enableReporting) {
        // we can also create Features which may be useful when fetching from remote resource although this may cause
        // problems with loading other features files due to classpath issues - Karate may need a RemoteResource type
        // that knows how to fetch related resources from the same URL the feature came from
//        List<Feature> featureList = featureFiles.stream()
//              .map(f -> Feature.read(new FileResource(f.toFile()))).collect(Collectors.toList());
//        logger.info("==== FEATURES {}", featureList);
//        Results results = Runner.builder()
//                .features(featureList)
//                .tags(tags)
//                .outputHtmlReport(true)
//                .parallel(8);

        Runner.Builder builder = Runner.builder().path(featurePaths);
        if (enableReporting) {
            builder = builder.outputHtmlReport(true).suiteReports(featureResultHandler);
        }
        final Results results = builder.parallel(threads);
        return new TestSuiteResults(results);
    }
}
