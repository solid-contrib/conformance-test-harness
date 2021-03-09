package org.solid.testharness.reporting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.io.File;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Element {
    enum Type {
        BACKGROUND,
        SCENARIO
    }
    private int line;
    private String name;
    private String description;
    private String id;
    private Type type;
    private List<Step> steps;
    private List<String> tags;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    @JsonSetter("keyword")
    public void setType(String keyword) {
        type = Type.valueOf(keyword.toUpperCase());
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public boolean isPassed() {
        return !steps.stream().filter(e -> e.getStatus() != Step.Status.PASSED).findAny().isPresent();
    }

    public boolean isScenario() {
        return type == Type.SCENARIO;
    }
}
