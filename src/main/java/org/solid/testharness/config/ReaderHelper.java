package org.solid.testharness.config;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;

public class ReaderHelper {
    private File credentialDirectory;

    public ReaderHelper(File directory) throws Exception {
        if (!directory.exists() || !directory.canRead()) {
            throw new Exception("Credential directory not readable");
        }
        credentialDirectory = directory;
    }

    public Reader getReader(String filename) throws Exception {
        if (StringUtils.isBlank(filename)) {
            throw new IllegalArgumentException("Filename is required");
        }
        File file = new File(credentialDirectory, filename);
        if (!file.exists() || !file.canRead()) {
            throw new Exception("Credential file not readable: " + file.getAbsolutePath());
        }
        return new FileReader(file);
    }
}
