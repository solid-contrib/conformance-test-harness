Feature: Creating a resource using PUT and PATCH must create intermediate containers

  Background: Set up clients and paths
    * def testContainer = createTestContainer()
    * def intermediateContainer = testContainer.generateChildContainer()
    * def resource = intermediateContainer.generateChildResource('.txt')

  Scenario: PUT creates a grandchild resource and intermediate containers
    * def resourceUrl = resource.getUrl()
    Given url resourceUrl
    And configure headers = clients.alice.getAuthHeaders('PUT', resourceUrl)
    And request "Hello"
    When method PUT
    Then assert responseStatus >= 200 && responseStatus < 300

    * def parentUrl = intermediateContainer.getUrl()
    Given url parentUrl
    And configure headers = clients.alice.getAuthHeaders('GET', parentUrl)
    When method GET
    Then status 200
    And match intermediateContainer.parseMembers(response) contains resource.getUrl()

    * def grandParentUrl = testContainer.getUrl()
    Given url grandParentUrl
    And configure headers = clients.alice.getAuthHeaders('GET', grandParentUrl)
    When method GET
    Then status 200
    And match testContainer.parseMembers(response) contains intermediateContainer.getUrl()

  Scenario: PATCH creates a grandchild resource and intermediate containers
    * def resourceUrl = resource.getUrl()
    Given url resourceUrl
    And configure headers = clients.alice.getAuthHeaders('PATCH', resourceUrl)
    And header Content-Type = "application/sparql-update"
    And request 'INSERT DATA { <#hello> <#linked> <#world> . }'
    When method PATCH
    Then assert responseStatus >= 200 && responseStatus < 300

    * def parentUrl = intermediateContainer.getUrl()
    Given url parentUrl
    And configure headers = clients.alice.getAuthHeaders('GET', parentUrl)
    When method GET
    Then status 200
    And match intermediateContainer.parseMembers(response) contains resource.getUrl()

    * def grandParentUrl = testContainer.getUrl()
    Given url grandParentUrl
    And configure headers = clients.alice.getAuthHeaders('GET', grandParentUrl)
    When method GET
    Then status 200
    And match testContainer.parseMembers(response) contains intermediateContainer.getUrl()
