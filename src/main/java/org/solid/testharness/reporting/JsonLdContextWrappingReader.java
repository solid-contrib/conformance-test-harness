package org.solid.testharness.reporting;

import org.apache.commons.lang3.StringUtils;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

class JsonLdContextWrappingReader extends FilterReader {
    private StringReader prefix;
    private StringReader suffix = new StringReader("}");
    private boolean prefixDone = false;
    private boolean suffixDone = false;

    JsonLdContextWrappingReader(Reader in, String context) {
        super(in);
        if (StringUtils.isBlank(context)) {
            throw new IllegalArgumentException("The JSON-LD context is required");
        }
        prefix = new StringReader("{\"@context\": " + context + ", \"results\":");
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int count = -1;
        if (!prefixDone) {
            count = prefix.read(cbuf, off, len);
            if (count == -1) {
                prefixDone = true;
            }
        }
        if (count == -1) {
            count = in.read(cbuf, off, len);
            if (count == -1 && !suffixDone) {
                count = suffix.read(cbuf, off, len);
                if (count == -1) {
                    suffixDone = true;
                }
            }
        }
        return count;
    }
}

