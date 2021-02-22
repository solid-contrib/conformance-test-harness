function fn() {
    const config = {};
    karate.log('BASE CONFIGURATION')
    // don't waste time waiting for a connection or if servers don't respond within 5 seconds
    const configFile = java.lang.System.getProperty('config')
    if (configFile !== null && configFile !== '') {
        const serverConfig = read(`file:${configFile}`);
        let env = karate.env; // get java system property 'karate.env'
        if (!env) {
            env = serverConfig.target;
        }
        config.target = serverConfig.servers[env]
        // bind external credentials into the config
        let credentialsPath = java.lang.System.getProperty('credentials')
        if (config.target.features.authentication && credentialsPath != null && credentialsPath != "") {
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
        karate.log('CONFIG: ', config);
    }
    karate.configure('connectTimeout', 5000);
    karate.configure('readTimeout', 5000);
    karate.configure('ssl', true);
    return config;
}