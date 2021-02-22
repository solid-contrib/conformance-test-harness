@ignore
Feature: Set up clients with a sample container and resource

  Scenario:
    * def setupClients =
    """
      function(resourcePath) {
        const res = {
          aliceAuthHeader: getAuthHeader('alice'),
          bobAuthHeader: getAuthHeader('bob'),
          resourcePath,
        };
        res.solidClient = SolidClient.create(res.aliceAuthHeader);
        res.resource = new SolidResource(res.solidClient, target.serverRoot + res.resourcePath, karate.readAsString('../fixtures/example.ttl'), 'text/turtle');
        res.containerUrl = res.resource.getParentUrl()
        res.containerPath = res.resource.getParentPath()
        return res;
      }
    """
