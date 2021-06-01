package org.solid.testharness.config;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserCredentialsTest {
    @Test
    public void loginCredentials() {
        final UserCredentials userCredentials = new UserCredentials();
        userCredentials.username = Optional.of("USERNAME");
        userCredentials.password = Optional.of("PASSWORD");

        assertFalse(userCredentials.isUsingRefreshToken());
        assertTrue(userCredentials.isUsingUsernamePassword());
        assertEquals("UserCredentials: username=***, password=***", userCredentials.toString());
    }

    @Test
    public void partialLoginCredentials() {
        final UserCredentials userCredentials = new UserCredentials();
        userCredentials.username = Optional.of("USERNAME");
        userCredentials.password = Optional.empty();
        assertFalse(userCredentials.isUsingUsernamePassword());
        userCredentials.username = Optional.empty();
        userCredentials.password = Optional.of("PASSWORD");
        assertFalse(userCredentials.isUsingUsernamePassword());
    }

    @Test
    public void refreshCredentials() {
        final UserCredentials userCredentials = new UserCredentials();
        userCredentials.refreshToken = Optional.of("TOKEN");
        userCredentials.clientId = Optional.of("CLIENT_ID");
        userCredentials.clientSecret = Optional.of("CLIENT_SECRET");
        assertTrue(userCredentials.isUsingRefreshToken());
        assertFalse(userCredentials.isUsingUsernamePassword());
        assertEquals("UserCredentials: refreshToken=***, clientId=***, clientSecret=***",
                userCredentials.toString());
    }

    @Test
    public void partialRefreshCredentials() {
        final UserCredentials userCredentials = new UserCredentials();
        userCredentials.refreshToken = Optional.of("TOKEN");
        userCredentials.clientId = Optional.of("CLIENT_ID");
        userCredentials.clientSecret = Optional.empty();
        assertFalse(userCredentials.isUsingRefreshToken());
        userCredentials.refreshToken = Optional.of("TOKEN");
        userCredentials.clientId = Optional.empty();
        userCredentials.clientSecret = Optional.of("CLIENT_SECRET");
        assertFalse(userCredentials.isUsingRefreshToken());
        userCredentials.refreshToken = Optional.empty();
        userCredentials.clientId = Optional.of("CLIENT_ID");
        userCredentials.clientSecret = Optional.of("CLIENT_SECRET");
        assertFalse(userCredentials.isUsingRefreshToken());
    }

    @Test
    public void emptyCredentials() {
        final UserCredentials userCredentials = new UserCredentials();
        assertFalse(userCredentials.isUsingRefreshToken());
        assertFalse(userCredentials.isUsingUsernamePassword());
        assertEquals("UserCredentials: username=null, password=null, " +
                "refreshToken=null, clientId=null, clientSecret=null", userCredentials.toString());
    }
}
