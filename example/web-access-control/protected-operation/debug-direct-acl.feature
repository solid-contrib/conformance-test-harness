Feature: Test ACL creation in apply mode

  Scenario: Direct ACR test
    * def testContainer = createTestContainer()
    * def testResource = testContainer.createChildResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');

    * print "INITIAL ACR: " + testResource.getAccessDataset().asTurtle()

    * def access = testResource.getAccessDatasetBuilder(webIds.alice).setAgentAccess(testResource.getUrl(), webIds.bob, ['read']).build()
    * print "SET ACR + " + access.asTurtle()
    * def resp = testResource.setAccessDataset(access)
    * print "RESP = " + resp

    * print "NEW ACR: " + testResource.getAccessDataset().asTurtle()

    * def x = pause(5000)
    * print "FINAL ACR: " + testResource.getAccessDataset().asTurtle()

    Given url testResource.getUrl()
    And headers clients.bob.getAuthHeaders('GET', testResource.getUrl())
    When method GET
    Then status 200
    * print response