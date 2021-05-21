package org.solid.testharness.utils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import static org.mockito.Mockito.*;

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

    public static HttpResponse<Void> mockVoidResponse(final int status) {
        final HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(status);
        return mockResponse;
    }

    private TestUtils() { }
}
