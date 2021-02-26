Feature: Creating a resource using PUT and PATCH must create intermediate containers

  Background:
    * call read('classpath:utils.feature')
    * def solidClient = authenticate('alice')
    * def containerPath = getRandomContainerPath()
    * def intermediateFolder = getRandomChildContainerPath()
    * def resourceName = getRandomResourceName('.txt')
    * def containerUrl = target.serverRoot + containerPath

    # prepare the teardown function
    * configure afterFeature = function() {solidClient.deleteResourceRecursively(containerUrl)}

  Scenario: PUT creates a grandchild resource and intermediate containers
    * def resourceUrl = target.serverRoot + containerPath + intermediateFolder + resourceName
    Given url resourceUrl
    And configure headers = solidClient.getAuthHeaders('PUT', resourceUrl)
    And request "Hello"
    When method PUT
    Then assert responseStatus >= 200 && responseStatus < 300

    * def parentUrl = target.serverRoot + containerPath + intermediateFolder
    Given url parentUrl
    And configure headers = solidClient.getAuthHeaders('GET', parentUrl)
    When method GET
    Then status 200
    * def parentMembers = RDFUtils.parseContainer(response, parentUrl)
    And match parentMembers contains resourceUrl

    * def grandParentUrl = target.serverRoot + containerPath
    Given url grandParentUrl
    And configure headers = solidClient.getAuthHeaders('GET', grandParentUrl)
    When method GET
    Then status 200
    * def grandParentMembers = RDFUtils.parseContainer(response, grandParentUrl)
    And match grandParentMembers contains parentUrl

  Scenario: PATCH creates a grandchild resource and intermediate containers
    Given url resourceUrl
    And configure headers = solidClient.getAuthHeaders('PATCH', resourceUrl)
    And header Content-Type = "application/sparql-update"
    And request 'INSERT DATA { <#hello> <#linked> <#world> . }'
    When method PATCH
    Then assert responseStatus >= 200 && responseStatus < 300

    * def parentUrl = target.serverRoot + containerPath + intermediateFolder
    Given url parentUrl
    And configure headers = solidClient.getAuthHeaders('GET', parentUrl)
    When method GET
    Then status 200
    * def parentMembers = RDFUtils.parseContainer(response, parentUrl)
    And match parentMembers contains resourceUrl

    * def grandParentUrl = target.serverRoot + containerPath
    Given url grandParentUrl
    And configure headers = solidClient.getAuthHeaders('GET', grandParentUrl)
    When method GET
    Then status 200
    * def grandParentMembers = RDFUtils.parseContainer(response, grandParentUrl)
    And match grandParentMembers contains parentUrl
