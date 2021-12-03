Feature: Requests support content negotiation for JSON-LD resource

  Background: Create a JSON-LD resource
    * def testContainer = rootTestContainer.reserveContainer()
    * def exampleJson = karate.readAsString('../fixtures/example.json')
    * def resource = testContainer.createResource('.json', exampleJson, 'application/ld+json');
    * def expected = parse(exampleJson, 'application/ld+json')
    * configure headers = clients.alice.getAuthHeaders('GET', resource.url)
    * url resource.url

  Scenario: Alice can GET the JSON-LD example as JSON-LD
    Given header Accept = 'application/ld+json'
    When method GET
    Then status 200
    And match header Content-Type contains 'application/ld+json'
    And assert parse(response, 'application/ld+json', resource.url).contains(expected)

  Scenario: Alice can GET the JSON-LD example as TTL
    Given header Accept = 'text/turtle'
    When method GET
    Then status 200
    And match header Content-Type contains 'text/turtle'
    And assert parse(response, 'text/turtle', resource.url).contains(expected)
