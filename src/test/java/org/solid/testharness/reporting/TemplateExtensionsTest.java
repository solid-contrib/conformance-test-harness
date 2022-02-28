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
package org.solid.testharness.reporting;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TemplateExtensionsTest {
    private final Map<String, Scores> scores = Map.of(
            "LEVEL1", new Scores(1, 2, 3, 4, 5),
            "LEVEL2", new Scores(1, 2, 3, 4, 5)
    );

    @Test
    void countLevelOutcome() {
        assertEquals(1, TemplateExtensions.count(scores, "LEVEL1", Scores.PASSED));
    }

    @Test
    void countLevelTotal() {
        assertEquals(15, TemplateExtensions.count(scores, "LEVEL1", null));
        assertEquals(15, TemplateExtensions.count(scores, "LEVEL1", ""));
    }

    @Test
    void countAllOutcome() {
        assertEquals(2, TemplateExtensions.count(scores, null, Scores.PASSED));
        assertEquals(10, TemplateExtensions.count(scores, "", Scores.INAPPLICABLE));
    }

    @Test
    void countAllTotal() {
        assertEquals(30, TemplateExtensions.count(scores, null, null));
        assertEquals(30, TemplateExtensions.count(scores, "", ""));
    }

    @Test
    void countMissingLevel() {
        assertEquals(0, TemplateExtensions.count(scores, "MISSING", null));
    }

    @Test
    void countNull() {
        assertEquals(0, TemplateExtensions.count(null, null, null));
    }

    @Test
    void orTotal() {
        assertEquals("LEVEL", TemplateExtensions.orTotal("LEVEL"));
        assertEquals("Totals", TemplateExtensions.orTotal(null));
        assertEquals("Totals", TemplateExtensions.orTotal(""));
    }
}
