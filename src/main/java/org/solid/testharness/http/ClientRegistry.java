/*
 * MIT License
 *
 * Copyright (c) 2019 - 2022 W3C Solid Community Group
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.solid.testharness.http;

import org.solid.testharness.config.Config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class ClientRegistry {
    private Map<String, Client> registeredClientMap;

    public static final String DEFAULT = "default";
    public static final String ALICE_WEBID = "alice-webid";

    @Inject
    Config config;

    @PostConstruct
    void postConstruct() {
        registeredClientMap = Collections.synchronizedMap(new HashMap<>());
        register(DEFAULT, new Client.Builder().build());
        final URI webId = URI.create(config.getWebIds().get(HttpConstants.ALICE));
        final Client client = new Client.Builder().followRedirects()
                .withOptionalLocalhostSupport(webId, config.isSelfSignedCertsAllowed()).build();
        register(ClientRegistry.ALICE_WEBID, client);
    }

    public void register(final String label, final Client client) {
        registeredClientMap.put(label, client);
    }

    public void unregister(final String label) {
        registeredClientMap.remove(label);
    }

    public boolean hasClient(final String label) {
        return registeredClientMap.containsKey(label);
    }

    public Client getClient(final String label) {
        return registeredClientMap.get(Objects.requireNonNullElse(label, DEFAULT));
    }
}
