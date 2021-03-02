Feature: The WAC-Allow header shows user and public access modes with Bob write and public read, append

  Background: Create test resource giving Bob write access and public read/append access
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
            + createBobAccessToAuthorization(target.users.bob.webID, resource.getPath(), 'acl:Write')
            + createPublicAccessToAuthorization(resource.getPath(), 'acl:Read, acl:Append')
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

  Scenario: There is an acl on the resource containing #bobAccessTo
    Given url testContext.resource.getAclUrl()
    And configure headers = testContext.solidClientAlice.getAuthHeaders('GET', testContext.resource.getAclUrl())
    And header Accept = 'text/turtle'
    When method GET
    Then status 200
    And match header Content-Type contains 'text/turtle'
    And match response contains 'bobAccessTo'

  Scenario: There is no acl on the parent
    Given url testContext.resource.getContainer().getAclUrl()
    And configure headers = testContext.solidClientAlice.getAuthHeaders('HEAD', testContext.resource.getContainer().getAclUrl())
    And header Accept = 'text/turtle'
    When method HEAD
    Then status 404

  Scenario: Bob calls GET and the header shows RWA access for user, RA for public
    Given configure headers = testContext.solidClientBob.getAuthHeaders('GET', resourceUrl)
    When method GET
    Then status 200
    And match header WAC-Allow != null
    * def result = parseWacAllowHeader(responseHeaders)
    And match result.user contains only ['read', 'write', 'append']
    And match result.public contains only ['read', 'append']

  Scenario: Bob calls HEAD and the header shows RWA access for user, RA for public
    Given configure headers = testContext.solidClientBob.getAuthHeaders('HEAD', resourceUrl)
    When method HEAD
    Then status 200
    And match header WAC-Allow != null
    * def result = parseWacAllowHeader(responseHeaders)
    And match result.user contains only ['read', 'write', 'append']
    And match result.public contains only ['read', 'append']

  Scenario: Public calls GET and the header shows RA access for user and public
    When method GET
    Then status 200
    And match header WAC-Allow != null
    * def result = parseWacAllowHeader(responseHeaders)
    And match result.user contains only ['read', 'append']
    And match result.public contains only ['read', 'append']

  Scenario: Public calls HEAD and the header shows RA access for user and public
    When method HEAD
    Then status 200
    And match header WAC-Allow != null
    * def result = parseWacAllowHeader(responseHeaders)
    And match result.user contains only ['read', 'append']
    And match result.public contains only ['read', 'append']
