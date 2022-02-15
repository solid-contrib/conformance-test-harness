package org.solid.testharness.reporting;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Scores {
    public static final String PASSED = "passed";
    public static final String FAILED = "failed";
    public static final String UNTESTED = "untested";
    public static final String INAPPLICABLE = "inapplicable";
    private Integer passed;
    private Integer failed;
    private Integer untested;
    private Integer inapplicable;

    public Integer getPassed() {
        return passed;
    }

    public Integer getFailed() {
        return failed;
    }

    public Integer getUntested() {
        return untested;
    }

    public Integer getInapplicable() {
        return inapplicable;
    }

    public int getScore(final String outcome) {
        switch (outcome) {
            case PASSED: return passed != null ? passed : 0;
            case FAILED: return failed != null ? failed : 0;
            case UNTESTED: return untested != null ? untested : 0;
            case INAPPLICABLE: return inapplicable != null ? inapplicable : 0;
            default: return 0;
        }
    }

    public void setScore(final String outcome, final int score) {
        switch (outcome) {
            case PASSED:
                passed = score;
                break;
            case FAILED:
                failed = score;
                break;
            case UNTESTED:
                untested = score;
                break;
            case INAPPLICABLE:
                inapplicable = score;
                break;
            default:
                break;
        }
    }

    public int getTotal() {
        return Optional.ofNullable(passed).orElse(0) +
                Optional.ofNullable(failed).orElse(0) +
                Optional.ofNullable(untested).orElse(0) +
                Optional.ofNullable(inapplicable).orElse(0);
    }
}
