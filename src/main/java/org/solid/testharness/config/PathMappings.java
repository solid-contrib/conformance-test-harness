package org.solid.testharness.config;

import io.quarkus.arc.config.ConfigProperties;

import java.util.List;

@ConfigProperties(prefix = "feature")
public class PathMappings {
    private List<Mapping> mappings;

    public void setMappings(List<Mapping> mappings) {
        this.mappings = (mappings.size() == 1 && mappings.get(0) == null) ? null : mappings;
    }
    public List<Mapping> getMappings() {
        return mappings;
    }

    @Override
    public String toString() {
        return mappings != null ? mappings.toString() : null;
    }

    public static class Mapping {
        public String prefix;
        public String path;

        public static Mapping create(String prefix, String path) {
            Mapping mapping = new Mapping();
            mapping.prefix = prefix;
            mapping.path = path;
            return mapping;
        }

        @Override
        public String toString() {
            return String.format("%s => %s", prefix, path);
        }
    }
}
