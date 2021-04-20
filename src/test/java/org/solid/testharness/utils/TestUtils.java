package org.solid.testharness.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

public class TestUtils {
    public static URL getFileUrl(String file) throws MalformedURLException {
        return Paths.get(file).normalize().toUri().toURL();
    }
}
