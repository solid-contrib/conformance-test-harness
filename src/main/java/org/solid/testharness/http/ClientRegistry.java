package org.solid.testharness.http;

import java.util.*;

public final class ClientRegistry {
    private static final Map<String, Client> registeredClientMap = Collections.synchronizedMap(new HashMap<>());

    public static final String DEFAULT = "default";
    public static final String SESSION_BASED = "session";

    static {
        register(DEFAULT, new Client.Builder().build());
        register(SESSION_BASED, new Client.Builder().withSessionSupport().build());
    }

    public static void register(final String label, final Client client) {
        registeredClientMap.put(label, client);
    }

    public static boolean hasClient(final String label) {
        return registeredClientMap.containsKey(label);
    }

    public static Client getClient(final String label) {
        if (label == null) {
            return registeredClientMap.get(DEFAULT);
        }
        return registeredClientMap.get(label);
    }

    private ClientRegistry() { }
}
