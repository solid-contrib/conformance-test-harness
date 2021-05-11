package org.solid.testharness.utils;

import java.util.Formatter;

public class TestHarnessInitializationException extends RuntimeException {
    private static final long serialVersionUID = -3676037765799217561L;

    public TestHarnessInitializationException(final String message, final String... args) {
        super((args != null && args.length > 0) ? new Formatter().format(message, args).toString() : message);
    }
}
