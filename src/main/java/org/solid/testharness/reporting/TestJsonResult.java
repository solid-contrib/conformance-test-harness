package org.solid.testharness.reporting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestJsonResult {
    private int line;
    private List<Element> elements;
    private String uri;
    private String description;
    private String id;

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public List<Element> getElements() {
        return elements;
    }

    public void setElements(List<Element> elements) {
        this.elements = elements;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
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

    public int countPassedScenarios() {
        return (int) elements.stream().filter(e -> e.isScenario() && e.isPassed()).count();
    }
    public int countScenarios() {
        return (int) elements.stream().filter(e -> e.isScenario()).count();
    }
}

