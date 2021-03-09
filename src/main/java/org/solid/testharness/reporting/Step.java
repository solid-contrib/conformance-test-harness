package org.solid.testharness.reporting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Step {
    enum Status {
        PASSED,
        SKIPPED,
        FAILED
    }

    private int line;
    private String name;
    private String keyword; // * Given And When Then
    private int duration;
    private Status status; // passed
    private String error;
    private String output;

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public int getDuration() {
        return duration;
    }

    public Status getStatus() {
        return status;
    }

    @JsonProperty("result")
    private void unpackResult(Map<String, String> result) {
        duration = Integer.valueOf(result.get("duration"));
        error = result.get("error_message");
        if (result.containsKey("status")) {
            status = Status.valueOf(result.get("status").toUpperCase());
        }
    }

    @JsonProperty("doc_string")
    private void unpackDocString(Map<String, String> docString) {
        output = docString.get("value");
    }

}
