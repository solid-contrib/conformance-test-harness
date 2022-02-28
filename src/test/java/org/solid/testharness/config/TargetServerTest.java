/*
 * MIT License
 *
 * Copyright (c) 2021 Solid
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.TestUtils;

import javax.inject.Inject;
import java.net.URL;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class TargetServerTest {
    @Inject
    DataRepository dataRepository;

    @BeforeEach
    void setUp() {
        try (RepositoryConnection conn = dataRepository.getConnection()) {
            conn.clear();
        }
    }

    @Test
    public void parseTargetServer() throws Exception {
        final URL testFile = TestUtils.getFileUrl("src/test/resources/config/targetserver-testing-feature.ttl");
        TestUtils.insertData(dataRepository, testFile);
        final TargetServer targetServer = new TargetServer(iri(TestUtils.SAMPLE_NS, "testserver"));
        assertAll("targetServer",
                () -> assertNotNull(targetServer.getFeatures()),
                () -> assertTrue(targetServer.getFeatures().contains("feature1")),
                () -> assertFalse(targetServer.getFeatures().contains("feature2")),
                () -> assertNotNull(targetServer.getSkipTags()),
                () -> assertTrue(targetServer.getSkipTags().contains("tag1")),
                () -> assertFalse(targetServer.getSkipTags().contains("tag2"))
        );
    }

    @Test
    public void parseTargetServerNoFeatures() throws Exception {
        final URL testFile = TestUtils.getFileUrl("src/test/resources/config/targetserver-testing-feature.ttl");
        TestUtils.insertData(dataRepository, testFile);
        final TargetServer targetServer = new TargetServer(iri(TestUtils.SAMPLE_NS, "testserver2"));
        assertAll("targetServer",
                () -> assertNotNull(targetServer.getFeatures()),
                () -> assertTrue(targetServer.getFeatures().isEmpty()),
                () -> assertNotNull(targetServer.getSkipTags()),
                () -> assertTrue(targetServer.getSkipTags().isEmpty())
        );
    }
}
