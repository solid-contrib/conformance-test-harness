package org.solid.testharness.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.SOLID_TEST;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.enterprise.inject.spi.CDI;
import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class UserCredentials {
    private static final Logger logger = LoggerFactory.getLogger(UserCredentials.class);

    private String webID;
    private String credentials;
    private String refreshToken;
    private String clientId;
    private String clientSecret;
    private String username;
    private String password;

    public UserCredentials(final Model model, final Resource resource) {
        for (Statement statement: model.filter(resource, null, null)) {
            if (statement.getPredicate().equals(SOLID_TEST.webId)) {
                setWebID(statement.getObject().stringValue());
            }
            if (statement.getPredicate().equals(SOLID_TEST.credentials)) {
                setCredentials(statement.getObject().stringValue());
            }
        }
    }

    public String getWebID() {
        return webID;
    }

    public void setWebID(final String webID) {
        this.webID = webID;
    }

    public String getCredentials() {
        return credentials;
    }

    public void setCredentials(final String credentials) {
        final Config config = CDI.current().select(Config.class).get();
        this.credentials = credentials;
        final Path credentialsPath = new File(config.getCredentialsDirectory(), credentials).toPath();
        try (final Reader reader = Files.newBufferedReader(credentialsPath)) {
            final ObjectMapper objectMapper = new ObjectMapper();
            final UserCredentials externalCredentials = objectMapper.readValue(reader, UserCredentials.class);
            if (externalCredentials.getRefreshToken() != null) {
                setRefreshToken(externalCredentials.getRefreshToken());
            }
            if (externalCredentials.getClientId() != null) {
                setClientId(externalCredentials.getClientId());
            }
            if (externalCredentials.getClientSecret() != null) {
                setClientSecret(externalCredentials.getClientSecret());
            }
            if (externalCredentials.getUsername() != null) {
                setUsername(externalCredentials.getUsername());
            }
            if (externalCredentials.getPassword() != null) {
                setPassword(externalCredentials.getPassword());
            }
        } catch (Exception e) {
            logger.debug("Failed to load user credentials: {}", e.toString());
            throw (TestHarnessInitializationException) new TestHarnessInitializationException(
                    "Failed to load user credentials: %s", e.toString()
            ).initCause(e);
        }
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(final String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(final String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public boolean isUsingUsernamePassword() {
        return username != null && password != null;
    }

    public boolean isUsingRefreshToken() {
        return refreshToken != null && clientId != null && clientSecret != null;
    }

    @Override
    public String toString() {
        if (isUsingUsernamePassword()) {
            return String.format("UserCredentials: webId=%s, credentials=%s, username=%s, password=%s",
                    webID, credentials, mask(username), mask(password)
            );
        } else if (isUsingRefreshToken()) {
            return String.format("UserCredentials: webId=%s, credentials=%s, " +
                            "refreshToken=%s, clientId=%s, clientSecret=%s",
                    webID, credentials, mask(refreshToken), mask(clientId), mask(clientSecret)
            );
        } else {
            return String.format("UserCredentials: webId=%s, credentials=%s, username=%s, password=%s, " +
                            "refreshToken=%s, clientId=%s, clientSecret=%s",
                    webID, credentials, mask(username), mask(password),
                    mask(refreshToken), mask(clientId), mask(clientSecret)
            );
        }
    }

    private String mask(final String value) {
        return value != null ? "***" : "null";
    }

    public UserCredentials() { }
}
