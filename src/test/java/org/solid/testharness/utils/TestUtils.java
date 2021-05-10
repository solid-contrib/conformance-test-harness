package org.solid.testharness.utils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

public final class TestUtils {
    public static URL getFileUrl(final String file) throws MalformedURLException {
        return Path.of(file).normalize().toUri().toURL();
    }

    public static URI getPathUri(final String path) {
        String uri = Path.of(path).toAbsolutePath().normalize().toUri().toString();
        if (uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        return URI.create(uri);
    }

    private TestUtils() { }
}
