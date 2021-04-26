package org.solid.testharness.utils;

import java.util.Formatter;

public class TestHarnessInitializationException extends RuntimeException {
    public TestHarnessInitializationException(String message, String... args) {
        super((args != null && args.length > 0) ? new Formatter().format(message, args).toString() : message);
    }
}
