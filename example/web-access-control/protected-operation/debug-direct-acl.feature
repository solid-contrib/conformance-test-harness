Feature: Test ACL creation in apply mode

  Scenario: Direct ACR test
    # create a test resource
    * def testContainer = rootTestContainer.reserveContainer()
    * def testResource = testContainer.createResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');

    # get the initial ACR for that resource
    Given url testResource.aclUrl
    And header Accept = 'text/turtle'
    And headers clients.alice.getAuthHeaders('GET', testResource.aclUrl)
    When method GET
    Then status 200
    * print "INITIAL ACR: " + response

    # grant Bob read access
    * def access = testResource.accessDatasetBuilder.setAgentAccess(testResource.url, webIds.bob, ['read']).build()
    * testResource.accessDataset = access

    # get the ACR to confirm it changed
    Given url testResource.aclUrl
    And header Accept = 'text/turtle'
    And headers clients.alice.getAuthHeaders('GET', testResource.aclUrl)
    When method GET
    Then status 200
    * print "NEW ACR: " + response
    * match response contains webIds.bob

    # can Bob read the resource
    Given url testResource.url
    And headers clients.bob.getAuthHeaders('GET', testResource.url)
    When method GET
    Then status 200
