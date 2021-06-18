function fn() {
    const bootstrap = Java.type('org.solid.testharness.config.Bootstrap').getInstance();
    if (!bootstrap) {
        karate.log('Scenario was not bootstrapped')
        karate.abort()
        return {}
    }

    const config = bootstrap.getConfig();
    const testSubject = bootstrap.getTestSubject();
    const target = testSubject.targetServer;
    if (target == null) {
        karate.log('Check the environment properties - config is not defined');
    }

    const LogMasker = Java.type('org.solid.testharness.utils.MaskingLogModifier');
    karate.configure('logModifier', LogMasker.INSTANCE);
    karate.configure('connectTimeout', config.connectTimeout);
    karate.configure('readTimeout', config.readTimeout);
    karate.configure('ssl', true);

    const additionalConfig = karate.call('classpath:setup.js')
    return {
        target,
        rootTestContainer: testSubject.getRootTestContainer(),
        ...additionalConfig,
        clients: karate.toMap(testSubject.clients),
        webIds: config.webIds
    };
}
