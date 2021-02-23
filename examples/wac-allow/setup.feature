@ignore
Feature: Set up clients with a sample container and resource

  Scenario:
    * def setupClients =
    """
      function(resourcePath) {
        const res = {
          solidClientAlice: authenticate('alice'),
          solidClientBob: authenticate('bob'),
          resourcePath,
        };
        res.resource = new SolidResource(res.solidClientAlice, target.serverRoot + res.resourcePath, karate.readAsString('../fixtures/example.ttl'), 'text/turtle');
        res.containerUrl = res.resource.getParentUrl()
        res.containerPath = res.resource.getParentPath()
        return res;
      }
    """
