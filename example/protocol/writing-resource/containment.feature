Feature: Creating a resource using PUT and PATCH must create intermediate containers

  Background: Set up clients and paths
    * def testContainer = rootTestContainer.reserveContainer()
    * def intermediateContainer = testContainer.reserveContainer()
    * def resource = intermediateContainer.reserveResource('.txt')

  Scenario: PUT creates a grandchild resource and intermediate containers
    * def resourceUrl = resource.url
    Given url resourceUrl
    And headers clients.alice.getAuthHeaders('PUT', resourceUrl)
    And header Content-Type = 'text/plain'
    And request 'Hello'
    When method PUT
    Then assert responseStatus >= 200 && responseStatus < 300

    * def parentUrl = intermediateContainer.url
    Given url parentUrl
    And headers clients.alice.getAuthHeaders('GET', parentUrl)
    And header Accept = 'text/turtle'
    When method GET
    Then status 200
    And match RDFUtils.parseContainerContents(response, parentUrl) contains resource.url

    * def grandParentUrl = testContainer.url
    Given url grandParentUrl
    And headers clients.alice.getAuthHeaders('GET', grandParentUrl)
    And header Accept = 'text/turtle'
    When method GET
    Then status 200
    * print 'GRANDPARENT CONTAINMENT TRIPLES' + response
    And match RDFUtils.parseContainerContents(response, grandParentUrl) contains intermediateContainer.url

  Scenario: PATCH creates a grandchild resource and intermediate containers
    * def resourceUrl = resource.url
    Given url resourceUrl
    And headers clients.alice.getAuthHeaders('PATCH', resourceUrl)
    And header Content-Type = "application/sparql-update"
    And request 'INSERT DATA { <#hello> <#linked> <#world> . }'
    When method PATCH
    Then assert responseStatus >= 200 && responseStatus < 300

    * def parentUrl = intermediateContainer.url
    Given url parentUrl
    And headers clients.alice.getAuthHeaders('GET', parentUrl)
    And header Accept = 'text/turtle'
    When method GET
    Then status 200
    And match RDFUtils.parseContainerContents(response, parentUrl) contains resource.url

    * def grandParentUrl = testContainer.url
    Given url grandParentUrl
    And headers clients.alice.getAuthHeaders('GET', grandParentUrl)
    And header Accept = 'text/turtle'
    When method GET
    Then status 200
    * print 'GRANDPARENT CONTAINMENT TRIPLES' + response
    And match RDFUtils.parseContainerContents(response, grandParentUrl) contains intermediateContainer.url
