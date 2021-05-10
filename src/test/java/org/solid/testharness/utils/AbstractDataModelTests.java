package org.solid.testharness.utils;

import io.quarkus.test.junit.QuarkusMock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.io.StringReader;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractDataModelTests {
    protected static String NS = "http://example.org/";

    public abstract String getData();

    @BeforeAll
    void setup() throws IOException {
        final StringReader reader = new StringReader(getData());
        final DataRepository repository = new DataRepository();
        repository.postConstruct();
        TestData.insertData(repository, reader);
        QuarkusMock.installMockForType(repository, DataRepository.class);
    }
}
