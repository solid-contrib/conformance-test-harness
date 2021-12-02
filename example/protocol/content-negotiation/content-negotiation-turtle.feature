Feature: Requests support content negotiation for Turtle resource

  Background: Create a turtle resource
    * def testContainer = rootTestContainer.reserveContainer()
    * def exampleTurtle = karate.readAsString('../fixtures/example.ttl')
    * def resource = testContainer.createResource('.ttl', exampleTurtle, 'text/turtle');
    * def expected = parse(exampleTurtle, 'text/turtle')
    * configure headers clients.alice.getAuthHeaders('GET', resource.url)
    * url resource.url

  Scenario: Alice can GET the TTL example as JSON-LD
    Given header Accept = 'application/ld+json'
    When method GET
    Then status 200
    And match header Content-Type contains 'application/ld+json'
    And assert parse(response, 'application/ld+json', resource.url).contains(expected)

  Scenario: Alice can GET the TTL example as TTL
    Given header Accept = 'text/turtle'
    When method GET
    Then status 200
    And match header Content-Type contains 'text/turtle'
    And assert parse(response, 'text/turtle', resource.url).contains(expected)
