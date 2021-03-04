package org.solid.testharness.config;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;


class ReaderHelperTest {
    @Test
    void testExceptions() {
    }

    @Test
    void getReader() throws Exception {
        String filename = "alice-credentials.json";
        String directory =  getResourceDirectory(filename);
        ReaderHelper readerHelper = new ReaderHelper(directory);
        Reader reader = readerHelper.getReader(filename);
        BufferedReader in = new BufferedReader(reader);
        String data = in.lines().collect(Collectors.joining());
        assertTrue(data.contains("EXTERNAL_TOKEN"));
    }

    public static String getResourceDirectory(String filename) throws Exception {
        ClassLoader classLoader = ReaderHelperTest.class.getClassLoader();
        URL url = classLoader.getResource(filename);
        if (url == null || !"file".equals(url.getProtocol())) {
            return null;
        }
        File file = Paths.get(url.toURI()).toFile();
        return file.getParent();
    }
}

