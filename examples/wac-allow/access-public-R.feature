Feature: The WAC-Allow header shows user and public access modes with public read set

  Background:
    * call read('classpath:utils.feature')
    * call read('this:setup.feature')
    * def setup =
    """
      function(config) {
        const resourcePath = getRandomResourcePath('.ttl');
        const context = setupClients(resourcePath);
        const acl = aclPrefix
          + createOwnerAuthorization(target.users.alice.webID, context.resourcePath)
          + createPublicAccessToAuthorization(context.resourcePath, config.publicModes);
        context.resource.setAcl(acl);
        return context;
      }
    """
    * def testContext = callonce setup { publicModes: 'acl:Read' }
    # prepare the teardown function
    * configure afterFeature = function() {HttpUtils.deleteResourceRecursively(testContext.containerUrl, testContext.aliceAuthHeader)}
    * url target.serverRoot + testContext.resourcePath

  Scenario: There is an acl on the resource containing #publicAccessTo
    Given url testContext.resource.getAclUrl()
    And header Authorization = testContext.aliceAuthHeader
    And header Accept = 'text/turtle'
    When method GET
    Then status 200
    And match response contains 'publicAccessTo'

  Scenario: There is no acl on the parent
    Given url testContext.resource.getParentAclUrl()
    And header Authorization = testContext.aliceAuthHeader
    And header Accept = 'text/turtle'
    When method HEAD
    Then status 404

  Scenario: Bob calls GET and the header shows R access for user, R for public
    Given header Authorization = testContext.bobAuthHeader
    When method GET
    Then status 200
    And match header WAC-Allow != null
    * def result = HttpUtils.parseWacAllowHeader(responseHeaders)
    And match result.user contains only ['read']
    And match result.public contains only ['read']

  Scenario: Bob calls HEAD and the header shows R access for user, R for public
    Given header Authorization = testContext.bobAuthHeader
    When method HEAD
    Then status 200
    And match header WAC-Allow != null
    * def result = HttpUtils.parseWacAllowHeader(responseHeaders)
    And match result.user contains only ['read']
    And match result.public contains only ['read']

  Scenario: Public calls GET and the header shows R access for user and public
    When method GET
    Then status 200
    And match header WAC-Allow != null
    * def result = HttpUtils.parseWacAllowHeader(responseHeaders)
    And match result.user contains only ['read']
    And match result.public contains only ['read']

  Scenario: Public calls HEAD and the header shows R access for user and public
    When method HEAD
    Then status 200
    And match header WAC-Allow != null
    * def result = HttpUtils.parseWacAllowHeader(responseHeaders)
    And match result.user contains only ['read']
    And match result.public contains only ['read']
