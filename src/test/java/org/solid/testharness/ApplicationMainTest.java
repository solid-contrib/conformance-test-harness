package org.solid.testharness;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ApplicationMainTest {
    @Test
    void main() {
        assertDoesNotThrow(() -> Application.main("--help"));
    }
}
