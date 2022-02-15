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

import static org.junit.jupiter.api.Assertions.*;

class ScoresTest {
    private static final String MISSING = "missing";

    @Test
    void getPassed() {
        final Scores scores = new Scores();
        scores.setScore(Scores.PASSED, 1);
        assertEquals(1, scores.getPassed());
    }

    @Test
    void getFailed() {
        final Scores scores = new Scores();
        scores.setScore(Scores.FAILED, 2);
        assertEquals(2, scores.getFailed());
    }

    @Test
    void getUntested() {
        final Scores scores = new Scores();
        scores.setScore(Scores.UNTESTED, 3);
        assertEquals(3, scores.getUntested());
    }

    @Test
    void getInapplicable() {
        final Scores scores = new Scores();
        scores.setScore(Scores.INAPPLICABLE, 4);
        assertEquals(4, scores.getInapplicable());
    }

    @Test
    void getScore() {
        final Scores scores = new Scores();
        scores.setScore(Scores.PASSED, 1);
        scores.setScore(Scores.FAILED, 2);
        scores.setScore(Scores.UNTESTED, 3);
        scores.setScore(Scores.INAPPLICABLE, 4);
        assertEquals(1, scores.getScore(Scores.PASSED));
        assertEquals(2, scores.getScore(Scores.FAILED));
        assertEquals(3, scores.getScore(Scores.UNTESTED));
        assertEquals(4, scores.getScore(Scores.INAPPLICABLE));
        assertEquals(0, scores.getScore(MISSING));
    }

    @Test
    void getScoreNull() {
        final Scores scores = new Scores();
        assertEquals(0, scores.getScore(Scores.PASSED));
        assertEquals(0, scores.getScore(Scores.FAILED));
        assertEquals(0, scores.getScore(Scores.UNTESTED));
        assertEquals(0, scores.getScore(Scores.INAPPLICABLE));
        assertEquals(0, scores.getScore(MISSING));
    }

    @Test
    void setScoreMissing() {
        final Scores scores = new Scores();
        scores.setScore(MISSING, 1);
        assertEquals(0, scores.getScore(MISSING));
    }

    @Test
    void getTotal() {
        final Scores scores = new Scores();
        scores.setScore(Scores.PASSED, 1);
        scores.setScore(Scores.FAILED, 2);
        scores.setScore(Scores.UNTESTED, 3);
        scores.setScore(Scores.INAPPLICABLE, 4);
        assertEquals(10, scores.getTotal());
    }

    @Test
    void getTotalNulls() {
        final Scores scores = new Scores();
        assertEquals(0, scores.getTotal());
    }
}