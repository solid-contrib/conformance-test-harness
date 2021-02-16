Feature: The WAC-Allow header shows user and public access modes with Bob write and public read, append as default on parent

  Background:
    * call read('classpath:utils.feature')
    * call read('this:setup.feature')
    * def setup =
    """
      function(config) {
        const resourcePath = getRandomResourcePath('.ttl');
        console.log('RESOURCE ', resourcePath);
        const context = setupClients(resourcePath);
        const acl = aclPrefix
          + createOwnerAuthorization(target.users.alice.webID, context.containerPath)
          + createBobDefaultAuthorization(target.users.bob.webID, context.containerPath, config.bobModes)
          + createPublicDefaultAuthorization(context.containerPath, config.publicModes)
        context.resource.setParentAcl(acl)
        return context;
      }
    """
    * def testContext = callonce setup { bobModes: 'acl:Write', publicModes: 'acl:Read, acl:Append' }
    # prepare the teardown function
    * configure afterFeature = function() {HttpUtils.deleteResourceRecursively(testContext.containerUrl, testContext.aliceAuthHeader)}
    * url target.serverRoot + testContext.resourcePath

  Scenario: There is no acl on the resource
    Given url testContext.resource.getAclUrl()
    And header Authorization = testContext.aliceAuthHeader
    And header Accept = 'text/turtle'
    When method HEAD
    Then status 404

  Scenario: There is an acl on the parent containing #bobDefault
    Given url testContext.resource.getParentAclUrl()
    And header Authorization = testContext.aliceAuthHeader
    And header Accept = 'text/turtle'
    When method GET
    Then status 200
    And match response contains 'bobDefault'

  Scenario: Bob calls GET and the header shows RWA access for user, RA for public
    Given header Authorization = testContext.bobAuthHeader
    When method GET
    Then status 200
    And match header WAC-Allow != null
    * def result = HttpUtils.parseWacAllowHeader(responseHeaders)
    And match result.user contains only ['read', 'write', 'append']
    And match result.public contains only ['read', 'append']

  Scenario: Bob calls HEAD and the header shows RWA access for user, RA for public
    Given header Authorization = testContext.bobAuthHeader
    When method HEAD
    Then status 200
    And match header WAC-Allow != null
    * def result = HttpUtils.parseWacAllowHeader(responseHeaders)
    And match result.user contains only ['read', 'write', 'append']
    And match result.public contains only ['read', 'append']

  Scenario: Public calls GET and the header shows RA access for user and public
    When method GET
    Then status 200
    And match header WAC-Allow != null
    * def result = HttpUtils.parseWacAllowHeader(responseHeaders)
    And match result.user contains only ['read', 'append']
    And match result.public contains only ['read', 'append']

  Scenario: Public calls HEAD and the header shows RA access for user and public
    When method HEAD
    Then status 200
    And match header WAC-Allow != null
    * def result = HttpUtils.parseWacAllowHeader(responseHeaders)
    And match result.user contains only ['read', 'append']
    And match result.public contains only ['read', 'append']
