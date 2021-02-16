package org.solid.testharness;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.fail;

// TODO: Tests not implemented during POC phase but will be going forwards

@Disabled
class RDFUtilsTest {

    private static final File getFile(String fileName) throws IOException
    {
        ClassLoader classLoader = RDFUtilsTest.class.getClassLoader();
        URL resource = classLoader.getResource(fileName);

        if (resource == null) {
            throw new IllegalArgumentException("file is not found!");
        } else {
            return new File(resource.getFile());
        }
    }

    @Test
    void turtleToTripleArray() {
    }

    @Test
    void triplesToTripleArray() {
    }

    @Test
    void jsonLdToTripleArray() {
        fail("Not implemented");
    }

    @Test
    void isTurtle() {
    }

    @Test
    void isJsonLD() {
    }

    @Test
    void isNTriples() {
    }

    @Test
    void getAclLink() {
    }
}