Feature: Inheritable ACL controls child resources

  Scenario: Create test resource and prepare ACL
    * def setup =
    """
      function() {
        const testContainer = createTestContainer();
        const resource = testContainer.createChildResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');
        return resource;
      }
    """
    * def addAcl =
    """
      function(container) {
        const access = container.getAccessDatasetBuilder(webIds.alice)
          .setAgentAccess(container.getUrl(), webIds.bob, ['read', 'write'])
          .setInheritableAgentAccess(container.getUrl(), webIds.bob, ['read', 'write'])
          .build();
        container.setAccessDataset(access);
      }
    """
    * def resource = callonce setup
    * assert resource.exists()
    * def resourceUrl = resource.getUrl()

    # Bob cannot access the resource
    Given url resourceUrl
    And headers clients.bob.getAuthHeaders('GET', resourceUrl)
    When method GET
    Then status 403

    # Add ACL which should propagate so Bob has access
    * addAcl(resource.getContainer())

    # Bob can put a new resource
    * def resource2 = resource.getContainer().generateChildResource('.txt')
    Given url resource2.getUrl()
    And request 'New resource'
    And headers clients.bob.getAuthHeaders('PUT', resource2.getUrl())
    And header Content-Type = 'text/plain'
    When method PUT
    Then match [200, 201, 204, 205] contains responseStatus

    # Bob can read the new resource
    Given url resource2.getUrl()
    And headers clients.bob.getAuthHeaders('GET', resource2.getUrl())
    When method GET
    Then status 200

    # Bob can now read the original resource
    Given url resourceUrl
    And headers clients.bob.getAuthHeaders('GET', resourceUrl)
    When method GET
    Then status 200
