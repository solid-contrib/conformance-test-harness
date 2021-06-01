Feature: Requests support content negotiation for Turtle resource

  Background: Create a turtle resource
    * def testContainer = createTestContainer()
    * def exampleTurtle = karate.readAsString('../fixtures/example.ttl')
    * def resource = testContainer.createChildResource('.ttl', exampleTurtle, 'text/turtle');
    * assert resource.exists()
    * def expected = RDFUtils.turtleToTripleArray(exampleTurtle, resource.getUrl())
    * configure headers = clients.alice.getAuthHeaders('GET', resource.getUrl())
    * url resource.getUrl()

  Scenario: Alice can read the TTL example as JSON-LD
    Given header Accept = 'application/ld+json'
    When method GET
    Then status 200
    And match header Content-Type contains 'application/ld+json'
    And match RDFUtils.jsonLdToTripleArray(JSON.stringify(response), resource.getUrl()) contains expected

  Scenario: Alice can read the TTL example as TTL
    Given header Accept = 'text/turtle'
    When method GET
    Then status 200
    And match header Content-Type contains 'text/turtle'
    And match RDFUtils.turtleToTripleArray(response, resource.getUrl()) contains expected
