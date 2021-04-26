package org.solid.testharness.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

public class TestUtils {
    public static URL getFileUrl(String file) throws MalformedURLException {
        return Path.of(file).normalize().toUri().toURL();
    }
}
