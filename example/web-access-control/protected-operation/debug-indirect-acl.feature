Feature: Test ACL in applyMembers mode

  Scenario: Indirect ACR test
    # create container, add ACL, create resource, test access
    * def testContainer = rootTestContainer.createContainer()

    # get the initial ACR for that container
    Given url testContainer.aclUrl
    And header Accept = 'text/turtle'
    And headers clients.alice.getAuthHeaders('GET', testContainer.aclUrl)
    When method GET
    Then status 200
    * print "INITIAL ACR: " + response

    # grant Bob read access to member resources
    * def access = testContainer.accessDatasetBuilder.setInheritableAgentAccess(testContainer.url, webIds.bob, ['read']).build()
    * testContainer.accessDataset = access

    # get the ACR to confirm it changed
    Given url testContainer.aclUrl
    And header Accept = 'text/turtle'
    And headers clients.alice.getAuthHeaders('GET', testContainer.aclUrl)
    When method GET
    Then status 200
    * print "NEW CONTAINER ACR: " + response
    * match response contains webIds.bob

    # create a new resource in the container which should inherit access from the container
    * def testResource = testContainer.createResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');

    # get the resource ACR to confirm it has inherited read permission for Bob
    Given url testResource.aclUrl
    And header Accept = 'text/turtle'
    And headers clients.alice.getAuthHeaders('GET', testResource.aclUrl)
    When method GET
    Then status 200
    * print "RESOURCE ACR: " + response

    # can Bob read the resource
    Given url testResource.url
    And headers clients.bob.getAuthHeaders('GET', testResource.url)
    When method GET
    Then status 200
