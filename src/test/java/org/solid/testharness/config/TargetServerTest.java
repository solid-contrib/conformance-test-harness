package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.*;

import javax.inject.Inject;
import java.net.URL;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class TargetServerTest {
    @Inject
    DataRepository dataRepository;

    @Test
    public void parseTargetServer() throws Exception {
        final URL testFile = TestUtils.getFileUrl("src/test/resources/targetserver-testing-feature.ttl");
        TestData.insertData(dataRepository, testFile);
        final TargetServer targetServer = new TargetServer(iri(TestData.SAMPLE_NS, "testserver"));
        assertAll("targetServer",
                () -> assertNotNull(targetServer.getFeatures()),
                () -> assertEquals(true, targetServer.getFeatures().get("feature1")),
                () -> assertNull(targetServer.getFeatures().get("feature2")),
                () -> assertEquals("https://tester", targetServer.getOrigin()),
                () -> assertEquals(true, targetServer.isSetupRootAcl()),
                () -> assertEquals(4, targetServer.getMaxThreads()),
                () -> assertEquals(false, targetServer.isDisableDPoP())
        );
    }

    @Test
    public void parseTargetServerWithBadThreads() throws Exception {
        final URL testFile = TestUtils.getFileUrl("src/test/resources/targetserver-testing-feature.ttl");
        TestData.insertData(dataRepository, testFile);
        assertThrows(TestHarnessInitializationException.class, () -> new TargetServer(iri(TestData.SAMPLE_NS, "bad")));
    }
}
