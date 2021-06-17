package org.solid.testharness.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MaskingLogModifierTest {
    @Test
    void enableForUri() {
        assertTrue(MaskingLogModifier.INSTANCE.enableForUri(""));
    }

    @Test
    void uri() {
        assertEquals("uri", MaskingLogModifier.INSTANCE.uri("uri"));
    }

    @Test
    void authorizationHeader() {
        assertEquals("Bearer ***abcdef", MaskingLogModifier.INSTANCE.header("AUTHORIZATION", "Bearer xxxabcdef"));
    }

    @Test
    void dpopHeader() {
        assertEquals("***abcdef", MaskingLogModifier.INSTANCE.header("DPOP", "xxxabcdef"));
    }

    @Test
    void otherHeader() {
        assertEquals("Other", MaskingLogModifier.INSTANCE.header("OTHER", "Other"));
    }

    @Test
    void request() {
        assertEquals("Request", MaskingLogModifier.INSTANCE.request("URI", "Request"));
    }

    @Test
    void response() {
        assertEquals("Response", MaskingLogModifier.INSTANCE.response("URI", "Response"));
    }
}
