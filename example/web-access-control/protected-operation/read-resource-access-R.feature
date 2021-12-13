Feature: Bob can only read an RDF resource to which he is only granted read access

  Background: Create test resource with read-only access for Bob
    * def setup =
    """
      function() {
        const testContainer = rootTestContainer.reserveContainer();
        const resource = testContainer.createResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');
        const access = resource.accessDatasetBuilder
          .setAgentAccess(resource.url, webIds.bob, ['read'])
          .build();
        resource.accessDataset = access;
        return resource;
      }
    """
    * def resource = callonce setup
    * url resource.url

  Scenario: Bob can read the resource with GET
    Given headers clients.bob.getAuthHeaders('GET', resource.url)
    When method GET
    Then status 200

  Scenario: Bob can read the resource with HEAD
    Given headers clients.bob.getAuthHeaders('HEAD', resource.url)
    When method HEAD
    Then status 200

  Scenario: Bob cannot PUT to the resource
    Given request '<> <http://www.w3.org/2000/01/rdf-schema#comment> "Bob replaced it." .'
    And headers clients.bob.getAuthHeaders('PUT', resource.url)
    And header Content-Type = 'text/turtle'
    When method PUT
    Then status 403

  Scenario: Bob cannot PATCH the resource
    Given request 'INSERT DATA { <> a <http://example.org/Foo> . }'
    And headers clients.bob.getAuthHeaders('PATCH', resource.url)
    And header Content-Type = 'application/sparql-update'
    When method PATCH
    Then status 403

  Scenario: Bob cannot POST to the resource
    Given request '<> <http://www.w3.org/2000/01/rdf-schema#comment> "Bob replaced it." .'
    And headers clients.bob.getAuthHeaders('POST', resource.url)
    And header Content-Type = 'text/turtle'
    When method POST
    Then status 403

  Scenario: Bob cannot DELETE the resource
    Given headers clients.bob.getAuthHeaders('DELETE', resource.url)
    When method DELETE
    Then status 403

  Scenario: Bob cannot use an unknown method on the resource
    * def response = clients.alice.sendAuthorized('DAHU', resource.url, null, null)
    Then assert response.status == 405
