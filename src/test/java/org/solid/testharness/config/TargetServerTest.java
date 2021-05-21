package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.utils.*;

import javax.inject.Inject;
import java.net.URI;
import java.net.URL;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class TargetServerTest {
    @InjectMock
    Config config;

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
                () -> assertEquals("https://inrupt.net", targetServer.getSolidIdentityProvider()),
                () -> assertEquals("https://inrupt.net/login/password", targetServer.getLoginEndpoint().toString()),
                () -> assertEquals(URI.create("http://localhost:3000/"), targetServer.getServerRoot()),
                () -> assertEquals("https://tester", targetServer.getOrigin()),
                () -> assertEquals(true, targetServer.isSetupRootAcl()),
                () -> assertEquals(4, targetServer.getMaxThreads()),
                () -> assertEquals(false, targetServer.isDisableDPoP()),
                () -> assertEquals("http://localhost:3000/test/", targetServer.getTestContainer()),
                () -> assertNotNull(targetServer.getWebIds()),
                () -> assertEquals(2, targetServer.getWebIds().size()),
                () -> assertNotNull(targetServer.getWebIds().get(HttpConstants.ALICE)),
                () -> assertEquals("https://solid-test-suite-alice.inrupt.net/profile/card#me",
                        targetServer.getWebIds().get(HttpConstants.ALICE)),
                () -> assertEquals("https://solid-test-suite-bob.inrupt.net/profile/card#me",
                        targetServer.getWebIds().get(HttpConstants.BOB))
        );
    }

    @Test
    public void parseTargetServerWithMissingElements() throws Exception {
        final URL testFile = TestUtils.getFileUrl("src/test/resources/targetserver-testing-feature.ttl");
        TestData.insertData(dataRepository, testFile);
        assertThrows(TestHarnessInitializationException.class, () -> new TargetServer(iri(TestData.SAMPLE_NS, "bad")));
    }

    @Test
    public void parseTestContainerWithSlashes() throws Exception {
        final URL testFile = TestUtils.getFileUrl("src/test/resources/targetserver-testing-feature.ttl");
        TestData.insertData(dataRepository, testFile);
        final TargetServer targetServer = new TargetServer(iri(TestData.SAMPLE_NS, "test2"));
        assertEquals("http://localhost:3000/test/", targetServer.getTestContainer());
    }

    @Test
    public void parseTestContainerNoSlashes() throws Exception {
        final URL testFile = TestUtils.getFileUrl("src/test/resources/targetserver-testing-feature.ttl");
        TestData.insertData(dataRepository, testFile);
        final TargetServer targetServer = new TargetServer(iri(TestData.SAMPLE_NS, "test3"));
        assertEquals("http://localhost:3000/test/", targetServer.getTestContainer());
    }
}
