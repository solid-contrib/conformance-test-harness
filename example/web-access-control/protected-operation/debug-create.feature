Feature: Create with immediate read

  Scenario: Create test
    * def testContainer = rootTestContainer.reserveContainer()
    * def testResource = testContainer.createResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');

    Given url testResource.url
    And headers clients.alice.getAuthHeaders('GET', testResource.url)
    When method GET
    Then status 200
