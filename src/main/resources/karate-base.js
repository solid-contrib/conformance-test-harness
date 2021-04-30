function fn() {
    const bootstrap = Java.type('org.solid.testharness.config.Bootstrap').getInstance();
    if (!bootstrap) {
        karate.log('Scenario was not bootstrapped')
        karate.abort()
        return {}
    }

    const testSubject = bootstrap.getTestSubject();
    const target = testSubject.targetServer;
    if (target == null) {
        karate.log('Check the environment properties - config is not defined');
    }

    karate.configure('connectTimeout', 5000);
    karate.configure('readTimeout', 5000);
    karate.configure('ssl', true);

    const additionalConfig = karate.call('classpath:setup.js', target)
    return {
        target,
        ...additionalConfig,
        clients: karate.toMap(testSubject.clients),
        webIds: target ? target.webIds : {}
    };
}
