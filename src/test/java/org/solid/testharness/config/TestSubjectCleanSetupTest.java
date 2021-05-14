package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class TestSubjectCleanSetupTest {
    @InjectMock
    Config config;

    @Inject
    TestSubject testSubject;

    @Test
    void registerWithoutServer() {
        assertThrows(TestHarnessInitializationException.class, () -> testSubject.registerClients());
    }
}
