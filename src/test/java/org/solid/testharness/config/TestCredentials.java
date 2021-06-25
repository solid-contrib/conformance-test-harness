package org.solid.testharness.config;

import java.util.Optional;

public class TestCredentials implements UserCredentials {
    public String webId;
    public Optional<String> refreshToken = Optional.empty();
    public Optional<String> clientId = Optional.empty();
    public Optional<String> clientSecret = Optional.empty();
    public Optional<String> username = Optional.empty();
    public Optional<String> password = Optional.empty();

    @Override
    public String webId() {
        return webId;
    }

    @Override
    public Optional<String> refreshToken() {
        return refreshToken;
    }

    @Override
    public Optional<String> clientId() {
        return clientId;
    }

    @Override
    public Optional<String> clientSecret() {
        return clientSecret;
    }

    @Override
    public Optional<String> username() {
        return username;
    }

    @Override
    public Optional<String> password() {
        return password;
    }
}
