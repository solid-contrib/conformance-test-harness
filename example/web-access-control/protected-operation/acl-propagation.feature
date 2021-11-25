Feature: Inheritable ACL controls child resources

  Background: Create test resource and prepare ACL
    * def testContainer = rootTestContainer.reserveContainer()
    * def resource = testContainer.createResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');
    * assert resource.exists()

  Scenario: Check Bob's access before and after setting access via the parent ACL
    # Bob cannot access the resource
    Given url resource.url
    And headers clients.bob.getAuthHeaders('GET', resource.url)
    When method GET
    Then status 403

    # Add ACL which should propagate so Bob has access
    * eval
    """
      access = testContainer.accessDatasetBuilder
          .setAgentAccess(testContainer.url, webIds.bob, ['read', 'write'])
          .setInheritableAgentAccess(testContainer.url, webIds.bob, ['read', 'write'])
          .build();
      testContainer.accessDataset = access;
    """

    # Bob can put a new resource
    * def resource2 = resource.container.reserveResource('.txt')
    Given url resource2.url
    And request 'New resource'
    And headers clients.bob.getAuthHeaders('PUT', resource2.url)
    And header Content-Type = 'text/plain'
    When method PUT
    # Only 201 because of https://datatracker.ietf.org/doc/html/rfc7231#section-4.3.4
    Then status 201

    # Bob can read the new resource
    Given url resource2.url
    And headers clients.bob.getAuthHeaders('GET', resource2.url)
    When method GET
    Then status 200

    # Bob can now read the original resource
    Given url resource.url
    And headers clients.bob.getAuthHeaders('GET', resource.url)
    When method GET
    Then status 200
