Feature: Test ACL in applyMembers mode retrospectively applied

  Scenario: Indirect retrospective ACR test
    # create resource, change container ACL, test access
    * def testContainer = createTestContainer()
    * def testResource = testContainer.createChildResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');

    # get the initial ACR for that resource
    Given url testResource.getAclUrl()
    And header Accept = 'text/turtle'
    And headers clients.alice.getAuthHeaders('GET', testResource.getAclUrl())
    When method GET
    Then status 200
    * print "INITIAL ACR: " + response

    # confirm Bob has no read access
    Given url testResource.getUrl()
    And headers clients.bob.getAuthHeaders('GET', testResource.getUrl())
    When method GET
    Then status 403

    # grant Bob read access to member resources
    * def access = testContainer.getAccessDatasetBuilder(webIds.alice).setInheritableAgentAccess(testContainer.getUrl(), webIds.bob, ['read']).build()
    * assert testContainer.setAccessDataset(access)

    # get the container ACR to confirm it changed
    Given url testContainer.getAclUrl()
    And header Accept = 'text/turtle'
    And headers clients.alice.getAuthHeaders('GET', testContainer.getAclUrl())
    When method GET
    Then status 200
    * print "NEW CONTAINER ACR: " + response
    * match response contains webIds.bob

    # get the resource ACR to confirm it changed and references the container ACR
    Given url testResource.getAclUrl()
    And header Accept = 'text/turtle'
    And headers clients.alice.getAuthHeaders('GET', testResource.getAclUrl())
    When method GET
    Then status 200
    * print "NEW RESOURCE ACR: " + response
    * match response contains testContainer.getAclUrl()

    # can Bob read the resource
    Given url testResource.getUrl()
    And headers clients.bob.getAuthHeaders('GET', testResource.getUrl())
    When method GET
    Then status 200
