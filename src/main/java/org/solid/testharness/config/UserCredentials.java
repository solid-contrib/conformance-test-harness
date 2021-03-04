package org.solid.testharness.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.Reader;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserCredentials {
    private String webID;
    private String credentials;
    private String refreshToken;
    private String clientId;
    private String clientSecret;
    private String username;
    private String password;

    public String getWebID() {
        return webID;
    }

    public void setWebID(String webID) {
        this.webID = webID;
    }

    public String getCredentials() {
        return credentials;
    }

    public void setCredentials(String credentials) throws Exception {
        this.credentials = credentials;
        if (credentials != null) {
            String credentialsPath = System.getProperty("credentials");
            Reader reader = new ReaderHelper(credentialsPath).getReader(credentials);
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
}
