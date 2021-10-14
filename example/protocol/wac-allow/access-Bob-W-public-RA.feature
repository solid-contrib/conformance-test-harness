Feature: The WAC-Allow header shows user and public access modes with Bob write and public read, append

  Background: Create test resource giving Bob write access and public read/append access
    * def setup =
    """
      function() {
        const testContainer = createTestContainer();
        const resource = testContainer.createChildResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');
        if (resource.exists()) {
          const access = resource.getAccessDatasetBuilder(webIds.alice)
                .setAgentAccess(resource.getUrl(), webIds.bob, ['write'])
                .setPublicAccess(resource.getUrl(), ['read', 'append'])
                .build();
          resource.setAccessDataset(access);
        }
        return resource;
      }
    """
    * def resource = callonce setup
    * assert resource.exists()
    * def resourceUrl = resource.getUrl()
    * url resourceUrl

  Scenario: There is an acl on the resource containing Bob's WebID
    Given url resource.getAclUrl()
    And headers clients.alice.getAuthHeaders('GET', resource.getAclUrl())
    And header Accept = 'text/turtle'
    When method GET
    Then status 200
    And match header Content-Type contains 'text/turtle'
    And match response contains webIds.bob

  # Note this test is not applicable to a server using ACP as the WAC-Allow header is not supported currently.
  # In ACP mode, the following step would fail as there will be an ACL on the parent - this step needs to change
  # to ask the test harness to confirm there is no inherited access.
  Scenario: There is no acl on the parent
    Given url resource.getContainer().getAclUrl()
    And headers clients.alice.getAuthHeaders('HEAD', resource.getContainer().getAclUrl())
    And header Accept = 'text/turtle'
    When method HEAD
    Then status 404

  Scenario: Bob calls GET and the header shows RWA access for user, RA for public
    Given headers clients.bob.getAuthHeaders('GET', resourceUrl)
    When method GET
    Then status 200
    And match header WAC-Allow != null
    * def result = parseWacAllowHeader(responseHeaders)
    And match result.user contains only ['read', 'write', 'append']
    And match result.public contains only ['read', 'append']

  Scenario: Bob calls HEAD and the header shows RWA access for user, RA for public
    Given headers clients.bob.getAuthHeaders('HEAD', resourceUrl)
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
