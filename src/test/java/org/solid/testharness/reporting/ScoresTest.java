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

import org.junit.jupiter.api.Test;
import org.solid.common.vocab.EARL;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoresTest {
    private static final String MISSING = "missing";
    private final Map<String, Scores> scores = Map.of(
            "LEVEL1", new Scores(1, 2, 3, 4, 5),
            "LEVEL2", new Scores(1, 2, 3, 4, 5)
    );

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
    void getCantTell() {
        final Scores scores = new Scores();
        scores.setScore(Scores.CANTTELL, 3);
        assertEquals(3, scores.getCantTell());
    }

    @Test
    void getUntested() {
        final Scores scores = new Scores();
        scores.setScore(Scores.UNTESTED, 4);
        assertEquals(4, scores.getUntested());
    }

    @Test
    void getInapplicable() {
        final Scores scores = new Scores();
        scores.setScore(Scores.INAPPLICABLE, 5);
        assertEquals(5, scores.getInapplicable());
    }

    @Test
    void getScore() {
        final Scores scores = new Scores(1, 2, 3, 4, 5);
        assertEquals(1, scores.getScore(Scores.PASSED));
        assertEquals(2, scores.getScore(Scores.FAILED));
        assertEquals(3, scores.getScore(Scores.CANTTELL));
        assertEquals(4, scores.getScore(Scores.UNTESTED));
        assertEquals(5, scores.getScore(Scores.INAPPLICABLE));
        assertEquals(0, scores.getScore(MISSING));
    }

    @Test
    void getScoreNull() {
        final Scores scores = new Scores();
        assertEquals(0, scores.getScore(Scores.PASSED));
        assertEquals(0, scores.getScore(Scores.FAILED));
        assertEquals(0, scores.getScore(Scores.CANTTELL));
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
    void incrementScores() {
        final Scores scores = new Scores();
        scores.incrementScore(Scores.PASSED);
        scores.incrementScore(Scores.FAILED);
        scores.incrementScore(Scores.CANTTELL);
        scores.incrementScore(Scores.UNTESTED);
        scores.incrementScore(Scores.INAPPLICABLE);
        assertEquals(1, scores.getScore(Scores.PASSED));
        assertEquals(1, scores.getScore(Scores.FAILED));
        assertEquals(1, scores.getScore(Scores.CANTTELL));
        assertEquals(1, scores.getScore(Scores.UNTESTED));
        assertEquals(1, scores.getScore(Scores.INAPPLICABLE));
        scores.incrementScore(Scores.PASSED);
        scores.incrementScore(Scores.FAILED);
        scores.incrementScore(Scores.CANTTELL);
        scores.incrementScore(Scores.UNTESTED);
        scores.incrementScore(Scores.INAPPLICABLE);
        assertEquals(2, scores.getScore(Scores.PASSED));
        assertEquals(2, scores.getScore(Scores.FAILED));
        assertEquals(2, scores.getScore(Scores.CANTTELL));
        assertEquals(2, scores.getScore(Scores.UNTESTED));
        assertEquals(2, scores.getScore(Scores.INAPPLICABLE));
    }

    @Test
    void incrementScoresMissing() {
        final Scores scores = new Scores();
        scores.incrementScore("MISSING");
        assertEquals(0, scores.getTotal());
    }

    @Test
    void getOutcome() {
        assertEquals(EARL.failed, new Scores(1, 1, 1, 1, 1).getOutcome());
        assertEquals(EARL.passed, new Scores(1, 0, 1, 1, 1).getOutcome());
        assertEquals(EARL.inapplicable, new Scores(0, 0, 1, 1, 2).getOutcome());
        assertEquals(EARL.inapplicable, new Scores(0, 0, 1, 1, 1).getOutcome());
        assertEquals(EARL.cantTell, new Scores(0, 0, 2, 1, 1).getOutcome());
        assertEquals(EARL.cantTell, new Scores(0, 0, 2, 2, 1).getOutcome());
        assertEquals(EARL.untested, new Scores(0, 0, 1, 2, 1).getOutcome());
        assertEquals(EARL.untested, new Scores(0, 0, 0, 0, 0).getOutcome());
    }

    @Test
    void getTotal() {
        final Scores scores = new Scores(1, 2, 3, 4, 5);
        assertEquals(15, scores.getTotal());
    }

    @Test
    void getTotalNulls() {
        final Scores scores = new Scores();
        assertEquals(0, scores.getTotal());
    }

    @Test
    void calcScoreLevelOutcome() {
        assertEquals(1, Scores.calcScore(scores, "LEVEL1", Scores.PASSED));
    }

    @Test
    void calcScoreLevelTotal() {
        assertEquals(15, Scores.calcScore(scores, "LEVEL1", null));
        assertEquals(15, Scores.calcScore(scores, "LEVEL1", ""));
    }

    @Test
    void calcScoreAllOutcome() {
        assertEquals(2, Scores.calcScore(scores, null, Scores.PASSED));
        assertEquals(10, Scores.calcScore(scores, "", Scores.INAPPLICABLE));
    }

    @Test
    void calcScoreAllTotal() {
        assertEquals(30, Scores.calcScore(scores, null, null));
        assertEquals(30, Scores.calcScore(scores, "", ""));
    }

    @Test
    void calcScoreMissingLevel() {
        assertEquals(0, Scores.calcScore(scores, "MISSING", null));
    }

    @Test
    void calcScoreNull() {
        assertEquals(0, Scores.calcScore(null, null, null));
    }
}
