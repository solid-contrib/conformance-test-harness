function fn() {
    const bootstrap = Java.type('org.solid.testharness.config.Bootstrap').getInstance();
    const harnessConfig = bootstrap.getTestHarnessConfig();

    // if (localConfig && localConfig.includeAllServers) servers = serverConfig.servers

    const target = harnessConfig.targetServer;
    if (target == null) {
        karate.log('Check the environment properties - config is not defined');
    }

    karate.configure('connectTimeout', 5000);
    karate.configure('readTimeout', 5000);
    karate.configure('ssl', true);

    const additionalConfig = karate.call('classpath:setup.js', target)
    return {
        target,
        // servers,
        ...additionalConfig,
        clients: karate.toMap(harnessConfig.clients),
        webIds: target.webIds
    };
}
