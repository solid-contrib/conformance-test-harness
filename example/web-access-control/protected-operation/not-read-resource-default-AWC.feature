Feature: Bob cannot read an RDF resource to which he is not granted default read access via the parent

  Background: Create test resource with all default access except read for Bob
    * def setup =
    """
      function() {
        const testContainer = createTestContainerImmediate();
        const access = testContainer.getAccessDatasetBuilder(webIds.alice)
          .setAgentAccess(testContainer.getUrl(), webIds.bob, ['write'])
          .setInheritableAgentAccess(testContainer.getUrl(), webIds.bob, ['append', 'write', 'control'])
          .build();
        karate.log('ACL:\n' + access.asTurtle());
        testContainer.setAccessDataset(access);
        return testContainer.createChildResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');
      }
    """
    * def resource = callonce setup
    * assert resource.exists()
    * def resourceUrl = resource.getUrl()
    * url resourceUrl

  Scenario: Bob cannot read the resource with GET
    Given headers clients.bob.getAuthHeaders('GET', resourceUrl)
    When method GET
    Then status 403

  Scenario: Bob cannot read the resource with HEAD
    Given headers clients.bob.getAuthHeaders('HEAD', resourceUrl)
    When method HEAD
    Then status 403

  Scenario: Bob can PUT to the resource but gets nothing back since he cannot read
    Given request '<> <http://www.w3.org/2000/01/rdf-schema#comment> "Bob replaced it." .'
    And headers clients.bob.getAuthHeaders('PUT', resourceUrl)
    And header Content-Type = 'text/turtle'
    When method PUT
    Then status 204