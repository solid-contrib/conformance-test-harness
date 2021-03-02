function(target) {
    const RDFUtils = Java.type('org.solid.testharness.utils.RDFUtils')
    const HttpUtils = Java.type('org.solid.testharness.http.HttpUtils')
    const SolidResource = Java.type('org.solid.testharness.utils.SolidResource')
    const SolidContainer = Java.type('org.solid.testharness.utils.SolidContainer')
    const utils = {
        RDFUtils,
        SolidResource,
        SolidContainer,
        statusSuccess: function(){ const status = karate.get('responseStatus'); return status >= 200 && status < 300 },
        statusRedirect: function(){ const status = karate.get('responseStatus'); return status >= 300 && status < 400 },
        statusFail: function(){ const status = karate.get('responseStatus'); return status >= 400 && status < 500 },
        parseWacAllowHeader: function(headers) { return HttpUtils.parseWacAllowHeader(headers) },

        createTestContainer: function(client) {
            return SolidContainer.create(client, target.serverRoot + target.testContainer).generateChildContainer()
        },
        createOwnerAuthorization: function(ownerAgent, target) {
            return `
    <#owner> a acl:Authorization;
      acl:agent <${ownerAgent}>;
      acl:accessTo <${target}>;
      acl:default <${target}>;
      acl:mode acl:Read, acl:Write, acl:Control.`},
        createAuthorization: function(config) {
            // config: { authUri, agents, groups, publicAccess, authenticatedAccess, accessToTargets, defaultTargets, modes }
            let acl = `\n<${config.authUri}> a acl:Authorization;`
            if ('agents' in config) {
                if (Array.isArray(config.agents)) {
                    acl += config.agents.map((agent) => `\n  acl:agent <${agent}>;`)
                } else {
                    acl += `\n  acl:agent <${config.agents}>;`
                }
            }
            if ('groups' in config) {
                if (Array.isArray(config.groups)) {
                    acl += config.groups.map((group) => `\n  acl:agentGroup <${group}>;`)
                } else {
                    acl += `\n  acl:agentGroup <${config.groups}>;`
                }
            }
            if (config.publicAccess) {
                acl += '\n  acl:agentClass foaf:Agent;'
            }
            if (config.authenticatedAccess) {
                acl += '\n  acl:agentClass acl:AuthenticatedAgent;'
            }
            if ('accessToTargets' in config) {
                if (Array.isArray(config.accessToTargets)) {
                    acl += config.accessToTargets.map((target) => `\n  acl:accessTo <${target}>;`)
                } else {
                    acl += `\n  acl:accessTo <${config.accessToTargets}>;`
                }
            }
            if ('defaultTargets' in config) {
                if (Array.isArray(config.defaultTargets)) {
                    acl += config.defaultTargets.map((target) => `\n  acl:default <${target}>;`)
                } else {
                    acl += `\n  acl:default <${config.defaultTargets}>;`
                }
            }
            if (Array.isArray(config.modes)) {
                acl += `\n  acl:mode ${config.modes.join(', ')}.`
            } else if (config.modes) {
                acl += `\n  acl:mode ${config.modes}.`
            }
            return acl
        },
        aclPrefix: '@prefix acl: <http://www.w3.org/ns/auth/acl#>.\n@prefix foaf: <http://xmlns.com/foaf/0.1/>.',
        createBobAccessToAuthorization: function(webID, resourcePath, modes) {
            return createAuthorization({authUri: '#bobAccessTo', agents: webID, accessToTargets: resourcePath, modes: modes})
        },
        createBobDefaultAuthorization: function(webID, resourcePath, modes) {
            return createAuthorization({authUri: '#bobDefault', agents: webID, defaultTargets: resourcePath, modes: modes})
        },
        createPublicAccessToAuthorization: function(resourcePath, modes) {
            return createAuthorization({authUri: '#publicAccessTo', publicAccess: true, accessToTargets: resourcePath, modes: modes})
        },
        createPublicDefaultAuthorization: function(resourcePath, modes) {
            return createAuthorization({authUri: '#publicDefault', publicAccess: true, defaultTargets: resourcePath, modes: modes})
        },
        authenticate: function(user) {
            // karate.log('Get User ', user)
            return clients[user];
        },
        pause: function (pause) { java.lang.Thread.sleep(pause) }
    }

    karate.log('SETUP CLIENTS AND UTILS');
    const AuthManager = Java.type('org.solid.testharness.http.AuthManager')
    const clients = {};
    Object.keys(target.users).forEach(user => {
        clients[user] = AuthManager.authenticate(user, target);
    })
    return { utils, clients }
}
