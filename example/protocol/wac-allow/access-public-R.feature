Feature: The WAC-Allow header shows user and public access modes with public read set

  Background: Create test resource giving public read access
    * def setup =
    """
      function() {
        const testContainer = rootTestContainer.reserveContainer();
        const resource = testContainer.createResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');
        if (resource.exists()) {
          const access = resource.getAccessDatasetBuilder(webIds.alice)
                .setPublicAccess(resource.url, ['read'])
                .build();
          resource.setAccessDataset(access);
        }
        return resource;
      }
    """
    * def resource = callonce setup
    * assert resource.exists()
    * def resourceUrl = resource.url
    * url resourceUrl

  Scenario: There is an acl on the resource granting public access
    Given url resource.aclUrl
    And headers clients.alice.getAuthHeaders('GET', resource.getAclUrl())
    And header Accept = 'text/turtle'
    When method GET
    Then status 200
    And match header Content-Type contains 'text/turtle'
    # This was a quick check but relies on the representation of the ACL not changing
    # It should really ASK {?s acl:accessTo <target>; acl:agentClass foaf:Agent ; acl:mode acl:Read .}
    And match response contains 'foaf:Agent'

  Scenario: There is no acl on the parent
    Given url resource.container.aclUrl
    And headers clients.alice.getAuthHeaders('HEAD', resource.container.getAclUrl())
    And header Accept = 'text/turtle'
    When method HEAD
    Then status 404

  Scenario: Bob calls GET and the header shows R access for user, R for public
    Given headers clients.bob.getAuthHeaders('GET', resourceUrl)
    When method GET
    Then status 200
    And match header WAC-Allow != null
    * def result = parseWacAllowHeader(responseHeaders)
    And match result.user contains only ['read']
    And match result.public contains only ['read']

  Scenario: Bob calls HEAD and the header shows R access for user, R for public
    Given headers clients.bob.getAuthHeaders('HEAD', resourceUrl)
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
