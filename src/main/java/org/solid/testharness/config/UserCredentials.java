package org.solid.testharness.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.common.vocab.SOLID_TEST;

import javax.enterprise.inject.spi.CDI;
import java.io.Reader;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserCredentials {
    private static final Logger logger = LoggerFactory.getLogger("org.solid.testharness.config.UserCredentials");

    private String webID;
    private String credentials;
    private String refreshToken;
    private String clientId;
    private String clientSecret;
    private String username;
    private String password;

    public UserCredentials() {}

    public UserCredentials(Model model, Resource resource) {
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

    public void setWebID(String webID) {
        this.webID = webID;
    }

    public String getCredentials() {
        return credentials;
    }

    public void setCredentials(String credentials) {
        TestHarnessConfig testHarnessConfig = CDI.current().select(TestHarnessConfig.class).get();
        this.credentials = credentials;
        if (credentials != null) {
            try {
//                TODO The ReaderHelper was for testing but look at alternatives
                Reader reader = new ReaderHelper(testHarnessConfig.getCredentialsDirectory()).getReader(credentials);
                ObjectMapper objectMapper = new ObjectMapper();
                UserCredentials externalCredentials = objectMapper.readValue(reader, UserCredentials.class);
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
                logger.debug("Failed to load user credentials", e);
                throw new RuntimeException("Failed to load user credentials", e);
            }
        }
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isUsingUsernamePassword() {
        return username != null && password != null;
    }

    public boolean isUsingRefreshToken() {
        return refreshToken != null && clientId != null && clientSecret != null;
    }
}
