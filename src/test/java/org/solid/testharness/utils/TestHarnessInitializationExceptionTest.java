package org.solid.testharness.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestHarnessInitializationExceptionTest {
    @Test
    void simpleMessage() {
        final TestHarnessInitializationException exception = new TestHarnessInitializationException("message");
        assertEquals("message", exception.getMessage());
    }

    @Test
    void simpleMessageNullArgs() {
        final TestHarnessInitializationException exception = new TestHarnessInitializationException("message", null);
        assertEquals("message", exception.getMessage());
    }

    @Test
    void simpleMessageArgs1() {
        final TestHarnessInitializationException exception = new TestHarnessInitializationException("message %s",
                "arg1");
        assertEquals("message arg1", exception.getMessage());
    }

    @Test
    void simpleMessageArgs2() {
        final TestHarnessInitializationException exception = new TestHarnessInitializationException("message %s %s",
                "arg1", "arg2");
        assertEquals("message arg1 arg2", exception.getMessage());
    }
}
