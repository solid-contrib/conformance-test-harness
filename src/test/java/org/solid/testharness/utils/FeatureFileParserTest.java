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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FeatureFileParserTest {
    private static final String FEATURE_COMMENTS = "# Feature comment 1\n\n# Feature comment 2";
    private static final String BACKGROUND_COMMENTS = "# Background comment 1";
    private static final String SCENARIO_1_COMMENTS = BACKGROUND_COMMENTS + "\n\n" +
            "# Scenario1 comment 1\n" +
            "\n" +
            "# Scenario1 comment 2";
    private static final String SCENARIO_2_COMMENTS = BACKGROUND_COMMENTS + "\n\n" +
            "# Scenario2 comment 1\n" +
            "# Scenario2 comment 2";
    private static final String SCENARIO_3_COMMENTS = "# Scenario1 comment 1\n" +
            "# Scenario1 comment 2";

    @Test
    void parseFeatureReadException() {
        assertThrows(IOException.class,
                () -> new FeatureFileParser(Path.of("src/test/resources/test-features/missing")));
    }

    @Test
    void parseFeature1() throws IOException {
        final FeatureFileParser featureFileParser = new FeatureFileParser(
                Path.of("src/test/resources/test-features/group1/feature1")
        );
        assertNull(featureFileParser.getFeatureComments());
        assertEquals(SCENARIO_3_COMMENTS, featureFileParser.getScenarioComments(0));
        assertEquals("", featureFileParser.getScenarioComments(1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> featureFileParser.getScenarioComments(-1));
    }

    @Test
    void parseFeature2() throws IOException {
        final FeatureFileParser featureFileParser = new FeatureFileParser(
                Path.of("src/test/resources/test-features/group1/feature2")
        );
        assertEquals(FEATURE_COMMENTS, featureFileParser.getFeatureComments());
        assertEquals(SCENARIO_1_COMMENTS, featureFileParser.getScenarioComments(0));
        assertEquals(SCENARIO_2_COMMENTS, featureFileParser.getScenarioComments(1));
        assertEquals(BACKGROUND_COMMENTS, featureFileParser.getScenarioComments(2));
    }

    @Test
    void parseFeature3() throws IOException {
        final FeatureFileParser featureFileParser = new FeatureFileParser(
                Path.of("src/test/resources/test-features/group1/feature3")
        );
        assertNull(featureFileParser.getFeatureComments());
        assertEquals("", featureFileParser.getScenarioComments(0));
        assertEquals(SCENARIO_3_COMMENTS, featureFileParser.getScenarioComments(1));
    }

    @Test
    void parseFeature4() throws IOException {
        final FeatureFileParser featureFileParser = new FeatureFileParser(
                Path.of("src/test/resources/test-features/otherExample/feature1")
        );
        assertNull(featureFileParser.getFeatureComments());
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> featureFileParser.getScenarioComments(-1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> featureFileParser.getScenarioComments(0));
    }

    @Test
    void getFeatureTitle() {
        assertEquals("Feature 1 title", FeatureFileParser.getFeatureTitle(
                Path.of("src/test/resources/test-features/group1/feature1").toFile()
        ));
    }

    @Test
    void getFeatureTitleNoComment() {
        assertEquals("Feature 2 title", FeatureFileParser.getFeatureTitle(
                Path.of("src/test/resources/test-features/group1/feature2").toFile()
        ));
    }

    @Test
    void getFeatureTitleMissing() {
        assertNull(FeatureFileParser.getFeatureTitle(
                Path.of("src/test/resources/test-features/otherExample/feature1").toFile()
        ));
    }
}
