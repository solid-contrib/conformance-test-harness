function fn() {
    // the settings are taken in the following order of preference: system property, local-config, config
    // Run from IDE: unless env set in IDE, it is not present so load from local-config
    // Run from Gradle: it passes through system properties, use defaults (inc from local-config) if not set

    let env = karate.env; // get java system property 'karate.env'
    let credentialsPath = karate.properties['credentials']
    let configFile = karate.properties['config']

    const cwd = karate.properties['user.dir']
    const localConfig = read(`file:${cwd}/local-config.json`)
    if (localConfig) {
        if (!env) env = localConfig.env
        if (!credentialsPath) credentialsPath = localConfig.credentials
        if (!configFile) configFile = localConfig.config && localConfig.config.startsWith('/') ? localConfig.config : `${cwd}/${localConfig.config}`
    }
    if (java.lang.System.getProperty('testmode') !== 'suite-runner') {
        karate.log("Settings env =", env, "| credentials =", credentialsPath, "| config =", configFile)
    }

    let target = {};
    let servers = {};

    if (configFile !== null && configFile !== '') {
        const serverConfig = read(`file:${configFile}`);
        if (!env) {
            env = serverConfig.target;
        }
        target = serverConfig.servers[env]
        if (localConfig && localConfig.includeAllServers) servers = serverConfig.servers

        // bind external credentials into the config
        if (target.features && target.features.authentication && credentialsPath != null && credentialsPath !== "") {
            if (!credentialsPath.endsWith('/')) {
                credentialsPath += '/';
            }
            Object.keys(target.users).forEach((key) => {
                if ('credentials' in target.users[key]) {
                    const data = read(`file:${credentialsPath}${target.users[key].credentials}`);
                    Object.assign(target.users[key], data);
                    delete target.users[key].credentials;
                }
            })
        }
    } else {
        karate.log('Check the environment properties - config is not defined');
    }
    karate.configure('connectTimeout', 5000);
    karate.configure('readTimeout', 5000);
    karate.configure('ssl', true);

    const additionalConfig = karate.call('classpath:setup.js', target)
    return {
        target,
        servers,
        ...additionalConfig.utils,
        clients: karate.toMap(additionalConfig.clients)
    };
}
