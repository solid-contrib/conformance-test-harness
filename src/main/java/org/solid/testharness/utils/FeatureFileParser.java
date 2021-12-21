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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FeatureFileParser {
    private static final Logger logger = LoggerFactory.getLogger(FeatureFileParser.class);

    private String featureComments;
    private String backgroundComments;
    private List<String> sectionComments = new ArrayList<>();

    public FeatureFileParser(final Path file) throws IOException {
        final var state = new Object() {
            boolean beforeFeature = true;
            boolean beforeScenarios = true;
        };
        final List<String> commentLines = new ArrayList<>();
        try (Stream<String> lines = Files.lines(file)) {
            lines.map(String::strip).filter(line -> line.length() == 0 || line.charAt(0) != '@').forEach(line -> {
                if (state.beforeFeature) {
                    if (line.startsWith("Feature")) {
                        if (!commentLines.isEmpty()) {
                            featureComments = String.join("\n", commentLines);
                        }
                        state.beforeFeature = false;
                    }
                } else if (state.beforeScenarios) {
                    if (line.startsWith("Background")) {
                        backgroundComments = String.join("\n", commentLines);
                        state.beforeScenarios = false;
                    } else if (line.startsWith("Scenario")) {
                        sectionComments.add(!commentLines.isEmpty() ? String.join("\n", commentLines) : "");
                        state.beforeScenarios = false;
                    }
                } else if (line.startsWith("Scenario")) {
                    sectionComments.add(!commentLines.isEmpty() ? String.join("\n", commentLines) : "");
                }
                if ((line.length() == 0 && !commentLines.isEmpty()) || (line.length() > 0 && line.charAt(0) == '#')) {
                    commentLines.add(line);
                } else {
                    commentLines.clear();
                }
            });
        }
    }

    public String getFeatureComments() {
        return featureComments;
    }

    public String getScenarioComments(final int index) {
        if (index < 0 || index >= sectionComments.size()) {
            throw new ArrayIndexOutOfBoundsException("The section index is out of bounds");
        }
        if (backgroundComments != null) {
            if (sectionComments.get(index).length() == 0) {
                return backgroundComments;
            } else {
                return backgroundComments + "\n\n" + sectionComments.get(index);
            }
        } else {
            return sectionComments.get(index);
        }
    }

    @SuppressWarnings("PMD.AssignmentInOperand") // this is a common pattern and changing it makes it less readable
    public static String getFeatureTitle(final File file) {
        try (Stream<String> lines = Files.lines(file.toPath())) {
            final String featureLine = lines.map(String::strip)
                    .filter(line -> line.startsWith("Feature:"))
                    .findFirst()
                    .orElse(null);
            if (featureLine != null) {
                return featureLine.split("Feature:", 2)[1].split("#",2)[0].strip();
            }
            logger.warn("FILE DOES NOT START WITH 'Feature:' {}", file.toPath());
        } catch (Exception e) { // jacoco will not show full coverage for this try-with-resources line
            logger.warn("FEATURE NOT READABLE: {} - {}", file.toPath(), e.getMessage());
        }
        return null;
    }
}
