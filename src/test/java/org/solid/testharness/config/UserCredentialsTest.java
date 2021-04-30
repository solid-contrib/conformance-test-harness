package org.solid.testharness.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class UserCredentialsTest {
    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void createFromModel() throws IOException {
        Resource user = iri("http://example.org/alice");
        Reader reader = new StringReader("@prefix solid-test: <https://github.com/solid/conformance-test-harness/vocab#> ." +
                "<http://example.org/alice> solid-test:webId <http://example.org/alice/profile/card#me>." +
                "<http://example.org/alice> solid-test:credentials \"inrupt-alice.json\".");
        Model model = Rio.parse(reader, RDFFormat.TURTLE);
        UserCredentials userCredentials = new UserCredentials(model, user);
        assertAll("userCredentials",
                () -> assertEquals("http://example.org/alice/profile/card#me", userCredentials.getWebID()),
                () -> assertEquals("EXTERNAL_USERNAME", userCredentials.getUsername()),
                () -> assertEquals("EXTERNAL_PASSWORD", userCredentials.getPassword()),
                () -> assertEquals("EXTERNAL_TOKEN", userCredentials.getRefreshToken()),
                () -> assertEquals("EXTERNAL_CLIENT_ID", userCredentials.getClientId()),
                () -> assertEquals("EXTERNAL_CLIENT_SECRET", userCredentials.getClientSecret()),
                () -> assertEquals("inrupt-alice.json", userCredentials.getCredentials()),
                () -> assertTrue(userCredentials.isUsingRefreshToken()),
                () -> assertTrue(userCredentials.isUsingUsernamePassword())
        );
        assertEquals("UserCredentials: webId=http://example.org/alice/profile/card#me, credentials=inrupt-alice.json, username=***, password=***", userCredentials.toString());
    }

    @Test
    public void parseCredentialsLogin() throws Exception {
        String json = "{" +
                "\"webID\": \"ALICE_WEB_ID\", " +
                "\"username\": \"USERNAME\", " +
                "\"password\": \"PASSWORD\"" +
                "}";
        UserCredentials userCredentials = objectMapper.readValue(json, UserCredentials.class);
        assertAll("userCredentials",
                () -> assertEquals("ALICE_WEB_ID", userCredentials.getWebID()),
                () -> assertEquals("USERNAME", userCredentials.getUsername()),
                () -> assertEquals("PASSWORD", userCredentials.getPassword()),
                () -> assertNull(userCredentials.getCredentials()),
                () -> assertFalse(userCredentials.isUsingRefreshToken()),
                () -> assertTrue(userCredentials.isUsingUsernamePassword())
        );
        assertEquals("UserCredentials: webId=ALICE_WEB_ID, credentials=null, username=***, password=***", userCredentials.toString());
    }

    @Test
    public void parseCredentialsRefresh() throws Exception {
        String json = "{" +
                "\"webID\": \"ALICE_WEB_ID\", " +
                "\"refreshToken\": \"TOKEN\", " +
                "\"clientId\": \"CLIENT_ID\", " +
                "\"clientSecret\": \"CLIENT_SECRET\"" +
                "}";
        UserCredentials userCredentials = objectMapper.readValue(json, UserCredentials.class);
        assertAll("userCredentials",
                () -> assertEquals("ALICE_WEB_ID", userCredentials.getWebID()),
                () -> assertEquals("TOKEN", userCredentials.getRefreshToken()),
                () -> assertEquals("CLIENT_ID", userCredentials.getClientId()),
                () -> assertEquals("CLIENT_SECRET", userCredentials.getClientSecret()),
                () -> assertNull(userCredentials.getCredentials()),
                () -> assertTrue(userCredentials.isUsingRefreshToken()),
                () -> assertFalse(userCredentials.isUsingUsernamePassword())
        );
        assertEquals("UserCredentials: webId=ALICE_WEB_ID, credentials=null, refreshToken=***, clientId=***, clientSecret=***", userCredentials.toString());
    }

    @Test
    public void parseEmptyCredentials() throws Exception {
        String json = "{}";
        UserCredentials userCredentials = objectMapper.readValue(json, UserCredentials.class);
        assertAll("userCredentials",
                () -> assertNull(userCredentials.getWebID()),
                () -> assertNull(userCredentials.getRefreshToken()),
                () -> assertNull(userCredentials.getClientId()),
                () -> assertNull(userCredentials.getClientSecret()),
                () -> assertNull(userCredentials.getUsername()),
                () -> assertNull(userCredentials.getPassword()),
                () -> assertNull(userCredentials.getCredentials()),
                () -> assertFalse(userCredentials.isUsingRefreshToken()),
                () -> assertFalse(userCredentials.isUsingUsernamePassword())
        );
        assertEquals("UserCredentials: webId=null, credentials=null, username=null, password=null, refreshToken=null, clientId=null, clientSecret=null", userCredentials.toString());
    }

    @Test
    public void parseCredentialsWithExternalFile() throws Exception {
        String json = "{" +
                "\"webID\": \"ALICE_WEB_ID\", " +
                "\"credentials\": \"inrupt-alice.json\"" +
                "}";
        UserCredentials userCredentials = objectMapper.readValue(json, UserCredentials.class);
        assertAll("userCredentials",
                () -> assertEquals("ALICE_WEB_ID", userCredentials.getWebID()),
                () -> assertEquals("EXTERNAL_TOKEN", userCredentials.getRefreshToken()),
                () -> assertEquals("EXTERNAL_CLIENT_ID", userCredentials.getClientId()),
                () -> assertEquals("EXTERNAL_CLIENT_SECRET", userCredentials.getClientSecret()),
                () -> assertEquals("EXTERNAL_USERNAME", userCredentials.getUsername()),
                () -> assertEquals("EXTERNAL_PASSWORD", userCredentials.getPassword()),
                () -> assertEquals("inrupt-alice.json", userCredentials.getCredentials())
        );
    }

    @Test
    public void parseCredentialsWithMissingFile() {
        String json = "{" +
                "\"webID\": \"ALICE_WEB_ID\", " +
                "\"credentials\": \"inrupt-missing.json\"" +
                "}";
        assertThrows(JsonMappingException.class, () -> objectMapper.readValue(json, UserCredentials.class));
    }

    @Test
    public void usernamePasswordOptions() throws JsonProcessingException {
        String json = "{\"username\": \"USERNAME\"}";
        UserCredentials userCredentials = objectMapper.readValue(json, UserCredentials.class);
        assertFalse(userCredentials.isUsingUsernamePassword());
    }

    @Test
    public void refreshTokenOptions() throws JsonProcessingException {
        String json = "{\"refreshToken\": \"TOKEN\"}";
        UserCredentials userCredentials = objectMapper.readValue(json, UserCredentials.class);
        assertFalse(userCredentials.isUsingRefreshToken());
    }

    @Test
    public void refreshTokenOptions2() throws JsonProcessingException {
        String json = "{\"refreshToken\": \"TOKEN\", \"clientId\": \"CLIENT_ID\"}";
        UserCredentials userCredentials = objectMapper.readValue(json, UserCredentials.class);
        assertFalse(userCredentials.isUsingRefreshToken());
    }
}
