Feature: Bob can read a container and its children if he is granted both direct and inheritable read access

  Background: Create test container with direct and inheritable read-only access for Bob
    * def setup =
    """
      function() {
        const testContainer = createTestContainerImmediate();
        const access = testContainer.getAccessDatasetBuilder(webIds.alice)
                .setAgentAccess(testContainer.getUrl(), webIds.bob, ['read'])
                .setInheritableAgentAccess(testContainer.getUrl(), webIds.bob, ['read'])
                .build();
        karate.log('ACL:\n' + access.asTurtle());
        if (testContainer.setAccessDataset(access)) {
          const intermediateContainer = testContainer.generateChildContainer();
          const resource = intermediateContainer.createChildResource('.txt', 'hello', 'text/plain')
          return {
            containerUrl: testContainer.getUrl(),
            intermediateContainerUrl: intermediateContainer.getUrl(),
            resourceUrl: resource.getUrl()
          }
        }
        return null;
      }
    """
    * def test = callonce setup
    * assert test != null

  Scenario: Bob can read the container and its children
    Given url test.containerUrl
    And headers clients.bob.getAuthHeaders('GET', test.containerUrl)
    When method GET
    Then status 200

  Scenario: Bob can read the intermediate container
    Given url test.intermediateContainerUrl
    And headers clients.bob.getAuthHeaders('GET', test.intermediateContainerUrl)
    When method GET
    Then status 200

  Scenario: Bob can read the resource
    Given url test.resourceUrl
    And headers clients.bob.getAuthHeaders('GET', test.resourceUrl)
    When method GET
    Then status 200

