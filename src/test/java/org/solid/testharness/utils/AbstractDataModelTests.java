package org.solid.testharness.utils;

import io.quarkus.test.junit.QuarkusMock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractDataModelTests {
    protected static String NS = TestData.SAMPLE_NS;

    public abstract String getTestFile();

    @BeforeAll
    void setup() throws IOException {
        try (Reader reader = Files.newBufferedReader(Path.of(getTestFile()))) {
            final DataRepository repository = new DataRepository();
            repository.postConstruct();
            TestData.insertData(repository, reader);
            QuarkusMock.installMockForType(repository, DataRepository.class);
        }
    }
}
