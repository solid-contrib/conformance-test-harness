# Some of the tests below are omitted for servers not implementing WAC
Feature: Bob cannot read an RDF resource to which he is not granted default read access via the parent

  Background: Create test resource with all default access except read for Bob
    * def setup =
    """
      function() {
        const testContainer = rootTestContainer.createContainer();
        const access = testContainer.accessDatasetBuilder
          .setAgentAccess(testContainer.url, webIds.bob, ['write'])
          .setInheritableAgentAccess(testContainer.url, webIds.bob, ['append', 'write', 'control'])
          .build();
        testContainer.accessDataset = access;
        return testContainer.createResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');
      }
    """
    * def resource = callonce setup
    * url resource.url

  Scenario: Bob cannot read the resource with GET
    Given headers clients.bob.getAuthHeaders('GET', resource.url)
    When method GET
    Then cantTellIf([405, 415, 403].includes(responseStatus))
    Then status 403

  Scenario: Bob cannot read the resource with HEAD
    Given headers clients.bob.getAuthHeaders('HEAD', resource.url)
    When method HEAD
    Then status 403

  @wac
  # Not applicable if server does not implement WAC
  Scenario: Bob can PUT to the resource but gets nothing back since he cannot read
    Given request '<> <http://www.w3.org/2000/01/rdf-schema#comment> "Bob replaced it." .'
    And headers clients.bob.getAuthHeaders('PUT', resource.url)
    And header Content-Type = 'text/turtle'
    When method PUT
    Then status 204