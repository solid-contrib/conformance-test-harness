package org.solid.testharness.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.solid.testharness.config.ReaderHelper;
import org.solid.testharness.config.UserCredentials;

import java.io.BufferedReader;
import java.io.File;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;


class StepTest {
    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void parseStep() throws Exception {
        String json = "{" +
                "  \"name\": \"NAME\"," +
                "  \"result\": {" +
                "    \"duration\": 1234," +
                "    \"status\": \"passed\"" +
                "  }," +
                "  \"match\": {" +
                "    \"location\": \"karate\"," +
                "    \"arguments\": []" +
                "  }," +
                "  \"keyword\": \"KEYWORD\"," +
                "  \"line\": 12" +
                "}";
        Step step = objectMapper.readValue(json, Step.class);
        assertAll("step",
                () -> assertEquals("NAME", step.getName()),
                () -> assertEquals("KEYWORD", step.getKeyword()),
                () -> assertEquals("passed", step.getStatus()),
                () -> assertEquals(1234, step.getDuration()),
                () -> assertEquals(12, step.getLine())
        );
    }
}

