Feature: Requests support content negotiation for JSON-LD resource

  Background: Create a JSON-LD resource
    * def testContainer = rootTestContainer.reserveContainer()
    * def exampleJson = karate.readAsString('../fixtures/example.json')
    * def resource = testContainer.createResource('.json', exampleJson, 'application/ld+json');
    * assert resource.exists()
    * def expected = RDFUtils.jsonLdToTripleArray(exampleJson, resource.getUrl())
    * headers clients.alice.getAuthHeaders('GET', resource.getUrl())
    * url resource.url

  Scenario: Alice can read the JSON-LD example as JSON-LD
    Given header Accept = 'application/ld+json'
    When method GET
    Then status 200
    And match header Content-Type contains 'application/ld+json'
    And match RDFUtils.jsonLdToTripleArray(JSON.stringify(response), resource.getUrl()) contains expected

  Scenario: Alice can read the JSON-LD example as TTL
    Given header Accept = 'text/turtle'
    When method GET
    Then status 200
    And match header Content-Type contains 'text/turtle'
    And match RDFUtils.turtleToTripleArray(response, resource.getUrl()) contains expected
