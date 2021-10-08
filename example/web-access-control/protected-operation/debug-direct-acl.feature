Feature: Test ACL creation in apply mode

  Scenario: Direct ACR test
    * def testContainer = createTestContainer()
    * def testResource = testContainer.createChildResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');

    Given url testResource.getAclUrl()
    And header Accept = 'text/turtle'
    And headers clients.alice.getAuthHeaders('GET', testResource.getAclUrl())
    When method GET
    Then status 200
    * print "INITIAL ACR: " + response

    * def access = testResource.getAccessDatasetBuilder(webIds.alice).setAgentAccess(testResource.getUrl(), webIds.bob, ['read']).build()
    * print "SET ACR + " + access.asTurtle()
    * def resp = testResource.setAccessDataset(access)
    * print "RESP = " + resp

    Given url testResource.getAclUrl()
    And header Accept = 'text/turtle'
    And headers clients.alice.getAuthHeaders('GET', testResource.getAclUrl())
    When method GET
    Then status 200
    * print "NEW ACR: " + response

    * def x = pause(5000)
    Given url testResource.getAclUrl()
    And header Accept = 'text/turtle'
    And headers clients.alice.getAuthHeaders('GET', testResource.getAclUrl())
    When method GET
    Then status 200
    * print "FINAL ACR: " + response

    Given url testResource.getUrl()
    And headers clients.bob.getAuthHeaders('GET', testResource.getUrl())
    When method GET
    Then status 200
    * print response