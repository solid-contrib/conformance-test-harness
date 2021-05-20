package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.solid.testharness.http.HttpConstants;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.TestData;

import javax.inject.Inject;
import java.io.StringReader;

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
        final StringReader reader = new StringReader(TestData.PREFIXES +
                "ex:testserver\n" +
                "    a earl:Software, earl:TestSubject ;\n" +
                "    doap:name \"Enterprise Solid Server (Web Access Control version)\";\n" +
                "    doap:release [\n" +
                "        doap:name \"ESS 1.0.9\";\n" +
                "        doap:revision \"1.0.9\";\n" +
                "        doap:created \"2021-03-05\"^^xsd:date\n" +
                "    ];\n" +
                "    doap:developer <https://inrupt.com/profile/card/#us>;\n" +
                "    doap:homepage <https://inrupt.com/products/enterprise-solid-server>;\n" +
                "    doap:description \"A production-grade Solid server produced and supported by Inrupt.\"@en;\n" +
                "    doap:programming-language \"Java\" ;\n" +
                "    solid:oidcIssuer <https://inrupt.net> ;\n" +
                "    solid:loginEndpoint <https://inrupt.net/login/password> ;\n" +
                "    solid-test:origin <https://tester> ;\n" +
                "    solid-test:aliceUser <https://solid-test-suite-alice.inrupt.net/profile/card#me> ;\n" +
                "    solid-test:bobUser <https://solid-test-suite-bob.inrupt.net/profile/card#me> ;\n" +
                "    solid-test:maxThreads 4 ;\n" +
                "    solid-test:features \"feature1\" ;\n" +
                "    solid-test:serverRoot <http://localhost:3000> ;\n" +
                "    solid-test:podRoot <http://localhost:3000/> ;\n" +
                "    solid-test:testContainer \"/test/\" ;\n" +
//                "    solid-test:disableDPoP true ;\n" +
                "    solid-test:setupRootAcl true .");
        TestData.insertData(dataRepository, reader);
        final TargetServer targetServer = new TargetServer(iri(TestData.SAMPLE_NS, "testserver"));
        assertAll("targetServer",
                () -> assertNotNull(targetServer.getFeatures()),
                () -> assertEquals(true, targetServer.getFeatures().get("feature1")),
                () -> assertNull(targetServer.getFeatures().get("feature2")),
                () -> assertEquals("https://inrupt.net", targetServer.getSolidIdentityProvider()),
                () -> assertEquals("https://inrupt.net/login/password", targetServer.getLoginEndpoint().toString()),
                () -> assertEquals("http://localhost:3000", targetServer.getServerRoot()),
                () -> assertEquals("https://tester", targetServer.getOrigin()),
                () -> assertEquals(true, targetServer.isSetupRootAcl()),
                () -> assertEquals(4, targetServer.getMaxThreads()),
                () -> assertEquals(false, targetServer.isDisableDPoP()),
                () -> assertEquals("http://localhost:3000/", targetServer.getRootContainer()),
                () -> assertEquals("/test/", targetServer.getTestContainer()),
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
        final StringReader reader = new StringReader(TestData.PREFIXES + "ex:bad a earl:Software, earl:TestSubject.");
        TestData.insertData(dataRepository, reader);
        final TargetServer targetServer = new TargetServer(iri(TestData.SAMPLE_NS, "bad"));
        assertNull(targetServer.getLoginEndpoint());
        assertEquals(0, targetServer.getWebIds().size());
    }
}
