package org.solid.testharness.http;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class ClientRegistry {
    private Map<String, Client> registeredClientMap;

    public static final String DEFAULT = "default";
    public static final String SESSION_BASED = "session";

    @PostConstruct
    void postConstruct() {
        registeredClientMap = Collections.synchronizedMap(new HashMap<>());
        register(DEFAULT, new Client.Builder().build());
        register(SESSION_BASED, new Client.Builder().withSessionSupport().build());
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
