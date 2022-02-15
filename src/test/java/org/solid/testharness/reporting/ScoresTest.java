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