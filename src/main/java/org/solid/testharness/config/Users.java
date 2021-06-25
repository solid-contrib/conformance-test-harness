package org.solid.testharness.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "users")
public interface Users {
    @WithName("alice")
    UserCredentials alice();

    @WithName("bob")
    UserCredentials bob();
}
