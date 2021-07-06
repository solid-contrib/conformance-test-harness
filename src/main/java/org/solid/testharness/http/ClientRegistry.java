/*
 * MIT License
 *
 * Copyright (c) 2021 Solid
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

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class ClientRegistry {
    private Map<String, Client> registeredClientMap;

    public static final String DEFAULT = "default";
    public static final String SESSION_BASED = "session";

    @Inject
    Config config;

    @PostConstruct
    void postConstruct() {
        registeredClientMap = Collections.synchronizedMap(new HashMap<>());
        if (config.overridingTrust()) {
            register(DEFAULT, new Client.Builder().withLocalhostSupport().build());
            register(SESSION_BASED, new Client.Builder().withLocalhostSupport().withSessionSupport().build());
        } else {
            register(DEFAULT, new Client.Builder().build());
            register(SESSION_BASED, new Client.Builder().withSessionSupport().build());
        }
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
        if (label == null) {
            return registeredClientMap.get(DEFAULT);
        }
        return registeredClientMap.get(label);
    }
}
