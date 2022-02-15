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
