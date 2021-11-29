Feature: Bob cannot read an RDF resource to which he is not granted read access

  Background: Create test resource with all access except read for Bob
    * def setup =
    """
      function() {
        const testContainer = rootTestContainer.reserveContainer();
        const resource = testContainer.createResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');
        const access = resource.accessDatasetBuilder
          .setAgentAccess(resource.url, webIds.bob, ['append', 'write', 'control'])
          .build();
        resource.accessDataset = access;
        return resource;
      }
    """
    * def resource = callonce setup
    * url resource.url

  Scenario: Bob cannot read the resource with GET
    Given headers clients.bob.getAuthHeaders('GET', resource.url)
    When method GET
    Then status 403

  Scenario: Bob cannot read the resource with HEAD
    Given headers clients.bob.getAuthHeaders('HEAD', resource.url)
    When method HEAD
    Then status 403
