@wac
Feature: The WAC-Allow header shows user and public access modes with Bob write and public read, append as default on parent

  Background: Create test resource giving Bob default write access and public default read/append access
    * def setup =
    """
      function() {
        const testContainer = rootTestContainer.reserveContainer();
        const resource = testContainer.createResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');
        const access = testContainer.accessDatasetBuilder
              .setInheritableAgentAccess(testContainer.url, webIds.bob, ['write'])
              .setInheritablePublicAccess(testContainer.url, ['read', 'append'])
              .build();
        resource.container.accessDataset = access
        return resource;
      }
    """
    * def resource = callonce setup
    * url resource.url

  Scenario: There is no acl on the resource
    Given url resource.aclUrl
    And headers clients.alice.getAuthHeaders('HEAD', resource.aclUrl)
    And header Accept = 'text/turtle'
    When method HEAD
    Then status 404

  Scenario: There is an acl on the parent containing Bob's WebID
    Given url resource.container.aclUrl
    And headers clients.alice.getAuthHeaders('GET', resource.container.aclUrl)
    And header Accept = 'text/turtle'
    When method GET
    Then status 200
    And match header Content-Type contains 'text/turtle'
    And match response contains webIds.bob

  Scenario: Bob calls GET and the header shows RWA access for user, RA for public
    Given headers clients.bob.getAuthHeaders('GET', resource.url)
    When method GET
    Then status 200
    And match header WAC-Allow != null
    * def result = parseWacAllowHeader(responseHeaders)
    And match result.user contains only ['read', 'write', 'append']
    And match result.public contains only ['read', 'append']

  Scenario: Bob calls HEAD and the header shows RWA access for user, RA for public
    Given headers clients.bob.getAuthHeaders('HEAD', resource.url)
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
