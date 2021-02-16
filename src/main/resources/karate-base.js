function fn() {
    // don't waste time waiting for a connection or if servers don't respond within 5 seconds
    let env = karate.env; // get java system property 'karate.env'
    let credentialsPath = java.lang.System.getProperty('credentials')
    if (!credentialsPath.endsWith('/')) {
        credentialsPath += '/';
    }
    const configFile = java.lang.System.getProperty('config')
    const serverConfig = read(`file:${configFile}`);
    if (!env) {
        env = serverConfig.target;
    }
    const config = {
        target: serverConfig.servers[env]
    };
    // bind external credentials into the config
    if (config.target.features.authentication && credentialsPath != null && credentialsPath != "") {
        console.log('Reading credential files from ', credentialsPath)
        Object.keys(config.target.users).forEach((key) => {
            if ('credentials' in config.target.users[key]) {
                const data = read(`file:${credentialsPath}${config.target.users[key].credentials}`);
                Object.assign(config.target.users[key], data);
            }
        })
    }
    karate.configure('connectTimeout', 5000);
    karate.configure('readTimeout', 5000);
    karate.configure('ssl', true);
    return config;
}