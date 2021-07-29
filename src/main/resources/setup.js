function() {
    const RDFUtils = Java.type('org.solid.testharness.utils.RDFUtils')
    const SolidResource = Java.type('org.solid.testharness.utils.SolidResource')
    const SolidContainer = Java.type('org.solid.testharness.utils.SolidContainer')
    const utils = {
        RDFUtils,
        SolidResource,
        SolidContainer,
        parseWacAllowHeader: (headers) => Java.type('org.solid.testharness.http.HttpUtils').parseWacAllowHeader(headers),
        createTestContainer: () => rootTestContainer.generateChildContainer(),
        createTestContainerImmediate: () => rootTestContainer.generateChildContainer().instantiate(),
        createAuthorization: (config) => {
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
                    // TODO: WHERE ARE GROUP DEFINITIONS
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
                    acl += config.accessToTargets.map((targetUri) => `\n  acl:accessTo <${targetUri}>;`)
                } else {
                    acl += `\n  acl:accessTo <${config.accessToTargets}>;`
                }
            }
            if ('defaultTargets' in config) {
                if (Array.isArray(config.defaultTargets)) {
                    acl += config.defaultTargets.map((targetUri) => `\n  acl:default <${targetUri}>;`)
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
        createOwnerAuthorization: (ownerAgent, targetUri) => createAuthorization({
            authUri: '#owner',
            agents: ownerAgent,
            accessToTargets: targetUri,
            defaultTargets: targetUri,
            modes: 'acl:Read, acl:Write, acl:Control'
        }),
        createBobAccessToAuthorization: (webID, resourceUri, modes) => createAuthorization({
            authUri: '#bobAccessTo',
            agents: webID,
            accessToTargets: resourceUri,
            modes: modes
        }),
        createBobDefaultAuthorization: (webID, resourceUri, modes) => createAuthorization({
            authUri: '#bobDefault',
            agents: webID,
            defaultTargets: resourceUri,
            modes: modes
        }),
        createPublicAccessToAuthorization: (resourceUri, modes) => createAuthorization({
            authUri: '#publicAccessTo',
            publicAccess: true,
            accessToTargets: resourceUri,
            modes: modes
        }),
        createPublicDefaultAuthorization: (resourceUri, modes) => createAuthorization({
            authUri: '#publicDefault',
            publicAccess: true,
            defaultTargets: resourceUri,
            modes: modes
        }),
        pause: (pause) => java.lang.Thread.sleep(pause)
    }
    return utils
}
