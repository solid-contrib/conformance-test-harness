Feature: Create with immediate read

  Scenario: Create test
    * def testContainer = createTestContainer()
    * def testResource = testContainer.createChildResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');

    Given url testResource.getUrl()
    And headers clients.alice.getAuthHeaders('GET', testResource.getUrl())
    When method GET
    Then status 200
    * print response
