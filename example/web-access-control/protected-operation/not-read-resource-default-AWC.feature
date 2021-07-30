Feature: Bob cannot read an RDF resource to which he is not granted default read access via the parent

  Background: Create test resource with all default access except read for Bob
    * def setup =
    """
      function() {
        const testContainer = createTestContainer();
        const resource = testContainer.createChildResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');
        if (resource.exists()) {
          const acl = aclPrefix
            + createOwnerAuthorization(webIds.alice, resource.getContainer().getUrl())
            + createBobDefaultAuthorization(webIds.bob, resource.getContainer().getUrl(), 'acl:Append, acl:Write, acl:Control')
          karate.log('ACL: ' + acl);
          resource.getContainer().setAccessDataset(acl);
        }
        return resource;
      }
    """
    * def resource = callonce setup
    * assert resource.exists()
    * def resourceUrl = resource.getUrl()
    * url resourceUrl

  Scenario: Bob can get the resource OPTIONS
    Given headers clients.bob.getAuthHeaders('OPTIONS', resourceUrl)
    When method OPTIONS
    Then status 204

  Scenario: Bob cannot read the resource with GET
    Given headers clients.bob.getAuthHeaders('GET', resourceUrl)
    When method GET
    Then status 403

  Scenario: Bob cannot read the resource with HEAD
    Given headers clients.bob.getAuthHeaders('HEAD', resourceUrl)
    When method HEAD
    Then status 403
