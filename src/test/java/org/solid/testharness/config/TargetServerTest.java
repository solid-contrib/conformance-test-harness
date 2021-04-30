package org.solid.testharness.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;
import org.solid.testharness.utils.DataRepository;
import org.solid.testharness.utils.TestData;

import javax.inject.Inject;
import java.io.File;
import java.io.StringReader;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@QuarkusTest
public class TargetServerTest {
    @InjectMock
    Config config;

    @Inject
    DataRepository dataRepository;

    @Test
    public void parseTargetServer() throws Exception {
        when(config.getCredentialsDirectory()).thenReturn(new File("src/test/resources").getCanonicalFile());
        StringReader reader = new StringReader("@base <https://example.org/> .\n" +
                "@prefix solid-test: <https://github.com/solid/conformance-test-harness/vocab#> .\n" +
                "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
                "@prefix doap: <http://usefulinc.com/ns/doap#> .\n" +
                "@prefix earl: <http://www.w3.org/ns/earl#> .\n" +
                "@prefix solid: <http://www.w3.org/ns/solid/terms#> ." +
                "<testserver>\n" +
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
                "    solid-test:aliceUser [\n" +
                "        solid-test:webId <https://solid-test-suite-alice.inrupt.net/profile/card#me> ;\n" +
                "        solid-test:credentials \"inrupt-alice.json\"\n" +
                "    ] ;\n" +
                "    solid-test:bobUser [\n" +
                "        solid-test:webId <https://solid-test-suite-bob.inrupt.net/profile/card#me> ;\n" +
                "        solid-test:credentials \"inrupt-bob.json\"\n" +
                "    ] ;\n" +
                "    solid-test:maxThreads 4 ;\n" +
                "    solid-test:features \"feature1\" ;\n" +
                "    solid-test:serverRoot <http://localhost:3000> ;\n" +
                "    solid-test:podRoot <http://localhost:3000/> ;\n" +
                "    solid-test:testContainer \"/test/\" ;\n" +
//                "    solid-test:disableDPoP true ;\n" +
                "    solid-test:setupAclRoot true .");
        TestData.insertData(dataRepository, reader);
        TargetServer targetServer = new TargetServer(iri("https://example.org/testserver"));
        assertAll("targetServer",
                () -> assertNotNull(targetServer.getFeatures()),
                () -> assertEquals(true, targetServer.getFeatures().get("feature1")),
                () -> assertNull(targetServer.getFeatures().get("feature2")),
                () -> assertEquals("https://inrupt.net", targetServer.getSolidIdentityProvider()),
                () -> assertEquals("https://inrupt.net/login/password", targetServer.getLoginEndpoint().toString()),
                () -> assertEquals("http://localhost:3000", targetServer.getServerRoot()),
                () -> assertEquals("https://tester", targetServer.getOrigin()),
                () -> assertEquals(false, targetServer.isSetupRootAcl()),
                () -> assertEquals(4, targetServer.getMaxThreads()),
                () -> assertEquals(false, targetServer.isDisableDPoP()),
                () -> assertEquals("http://localhost:3000/", targetServer.getRootContainer()),
                () -> assertEquals("/test/", targetServer.getTestContainer()),
                () -> assertNotNull(targetServer.getUsers()),
                () -> assertEquals(2, targetServer.getUsers().size()),
                () -> assertNotNull(targetServer.getUsers().get("alice")),
                () -> assertEquals("EXTERNAL_TOKEN", targetServer.getUsers().get("alice").getRefreshToken()),
                () -> assertNotNull(targetServer.getUsers().get("bob")),
                () -> assertNull(targetServer.getUsers().get("bob").getUsername()),
                () -> assertNotNull(targetServer.getWebIds())
                );
        assertAll("targetServerCachedValues",
                () -> assertNotNull(targetServer.getFeatures()),
                () -> assertEquals("https://inrupt.net", targetServer.getSolidIdentityProvider()),
                () -> assertEquals("https://inrupt.net/login/password", targetServer.getLoginEndpoint().toString()),
                () -> assertEquals("http://localhost:3000", targetServer.getServerRoot()),
                () -> assertEquals("https://tester", targetServer.getOrigin()),
                () -> assertEquals(false, targetServer.isSetupRootAcl()),
                () -> assertEquals(4, targetServer.getMaxThreads()),
                () -> assertEquals(false, targetServer.isDisableDPoP()),
                () -> assertEquals("http://localhost:3000/", targetServer.getRootContainer()),
                () -> assertEquals("/test/", targetServer.getTestContainer()),
                () -> assertNotNull(targetServer.getUsers())
        );
    }

    @Test
    public void parseTargetServerWithMissingElements() throws Exception {
        StringReader reader = new StringReader("@base <https://example.org/> .\n" +
                "@prefix solid-test: <https://github.com/solid/conformance-test-harness/vocab#> .\n" +
                "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
                "@prefix doap: <http://usefulinc.com/ns/doap#> .\n" +
                "@prefix earl: <http://www.w3.org/ns/earl#> .\n" +
                "@prefix solid: <http://www.w3.org/ns/solid/terms#> ." +
                "<bad>\n" +
                "    a earl:Software, earl:TestSubject .");
        TestData.insertData(dataRepository, reader);
        TargetServer targetServer = new TargetServer(iri("https://example.org/bad"));
        assertNull(targetServer.getLoginEndpoint());
        assertEquals(0, targetServer.getUsers().size());
    }
}
