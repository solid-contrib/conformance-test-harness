Feature: The WAC-Allow header shows user and public access modes with public read set

  Background: Create test resource giving public read access
    * def setup =
    """
      function() {
        const solidClientAlice = authenticate('alice');
        const solidClientBob = authenticate('bob');
        const testContainer = createTestContainer(solidClientAlice);
        const resource = testContainer.createChildResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');
        if (resource.exists()) {
          const acl = aclPrefix
            + createOwnerAuthorization(target.users.alice.webID, resource.getPath())
            + createPublicAccessToAuthorization(resource.getPath(), 'acl:Read');
          resource.setAcl(acl);
        }
        return {solidClientAlice, solidClientBob, resource};
      }
    """
    * def testContext = callonce setup
    * assert testContext.resource.exists()
    * def resourceUrl = testContext.resource.getUrl()
    * url resourceUrl

    * configure afterFeature = function() {testContext.resource.getContainer().delete()}

  Scenario: There is an acl on the resource containing #publicAccessTo
    Given url testContext.resource.getAclUrl()
    And configure headers = testContext.solidClientAlice.getAuthHeaders('GET', testContext.resource.getAclUrl())
    And header Accept = 'text/turtle'
    When method GET
    Then status 200
    And match header Content-Type contains 'text/turtle'
    And match response contains 'publicAccessTo'

  Scenario: There is no acl on the parent
    Given url testContext.resource.getContainer().getAclUrl()
    And configure headers = testContext.solidClientAlice.getAuthHeaders('HEAD', testContext.resource.getContainer().getAclUrl())
    And header Accept = 'text/turtle'
    When method HEAD
    Then status 404

  Scenario: Bob calls GET and the header shows R access for user, R for public
    Given configure headers = testContext.solidClientBob.getAuthHeaders('GET', resourceUrl)
    When method GET
    Then status 200
    And match header WAC-Allow != null
    * def result = parseWacAllowHeader(responseHeaders)
    And match result.user contains only ['read']
    And match result.public contains only ['read']

  Scenario: Bob calls HEAD and the header shows R access for user, R for public
    Given configure headers = testContext.solidClientBob.getAuthHeaders('HEAD', resourceUrl)
    When method HEAD
    Then status 200
    And match header WAC-Allow != null
    * def result = parseWacAllowHeader(responseHeaders)
    And match result.user contains only ['read']
    And match result.public contains only ['read']

  Scenario: Public calls GET and the header shows R access for user and public
    When method GET
    Then status 200
    And match header WAC-Allow != null
    * def result = parseWacAllowHeader(responseHeaders)
    And match result.user contains only ['read']
    And match result.public contains only ['read']

  Scenario: Public calls HEAD and the header shows R access for user and public
    When method HEAD
    Then status 200
    And match header WAC-Allow != null
    * def result = parseWacAllowHeader(responseHeaders)
    And match result.user contains only ['read']
    And match result.public contains only ['read']
