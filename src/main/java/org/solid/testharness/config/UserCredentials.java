package org.solid.testharness.config;

import io.quarkus.arc.config.ConfigProperties;

import java.util.Optional;

@ConfigProperties(prefix = "alice")
public final class UserCredentials {
    public Optional<String> refreshToken;
    public Optional<String> clientId;
    public Optional<String> clientSecret;
    public Optional<String> username;
    public Optional<String> password;

    public boolean isUsingUsernamePassword() {
        return username.isPresent() && password.isPresent();
    }

    public boolean isUsingRefreshToken() {
        return refreshToken.isPresent() && clientId.isPresent() && clientSecret.isPresent();
    }

    @Override
    public String toString() {
        if (isUsingUsernamePassword()) {
            return String.format("UserCredentials: username=%s, password=%s",
                    mask(username), mask(password)
            );
        } else if (isUsingRefreshToken()) {
            return String.format("UserCredentials: refreshToken=%s, clientId=%s, clientSecret=%s",
                    mask(refreshToken), mask(clientId), mask(clientSecret)
            );
        } else {
            return String.format("UserCredentials: username=%s, password=%s, " +
                            "refreshToken=%s, clientId=%s, clientSecret=%s",
                    mask(username), mask(password),
                    mask(refreshToken), mask(clientId), mask(clientSecret)
            );
        }
    }

    private String mask(final Optional<String> value) {
        return value.isPresent() ? "***" : "null";
    }
}
