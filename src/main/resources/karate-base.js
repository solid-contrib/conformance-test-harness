function fn() {
    const config = {};
    // the settings are taken in the following order of preference: system property, local-config, config
    let env = karate.env; // get java system property 'karate.env'
    let credentialsPath = java.lang.System.getProperty('credentials')
    let configFile = java.lang.System.getProperty('config')

    const cwd = java.lang.System.getProperty('user.dir')
    const localConfig = read(`file:${cwd}/local-config.json`)
    if (localConfig) {
        if (!env) env = localConfig.env
        if (!credentialsPath) credentialsPath = localConfig.credentials
        if (!configFile) configFile = localConfig.config && localConfig.config.startsWith('/') ? localConfig.config : `${cwd}/${localConfig.config}`
    }
    karate.log("Settings env =", env, "| credentials =", credentialsPath, "| config =", configFile)

    if (configFile !== null && configFile !== '') {
        const serverConfig = read(`file:${configFile}`);
        if (!env) {
            env = serverConfig.target;
        }
        config.target = serverConfig.servers[env]
        if (localConfig && localConfig.includeAllServers) config.servers = serverConfig.servers

        // bind external credentials into the config
        if (config.target.features && config.target.features.authentication && credentialsPath != null && credentialsPath != "") {
            if (!credentialsPath.endsWith('/')) {
                credentialsPath += '/';
            }
            Object.keys(config.target.users).forEach((key) => {
                if ('credentials' in config.target.users[key]) {
                    const data = read(`file:${credentialsPath}${config.target.users[key].credentials}`);
                    Object.assign(config.target.users[key], data);
                }
            })
        }
        // karate.log('CONFIG: ', config);
    } else {
        karate.log('Check the environment properties - config is not defined');
    }
    // don't waste time waiting for a connection or if servers don't respond within 5 seconds
    karate.configure('connectTimeout', 5000);
    karate.configure('readTimeout', 5000);
    karate.configure('ssl', true);
    return config;
}