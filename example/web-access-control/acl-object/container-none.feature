Feature: Bob cannot read a container or children if he is not given any access

  Background: Create test container with no access for Bob
    * def setup =
    """
      function() {
        const testContainer = createTestContainerImmediate();
        const access = testContainer.getAccessDatasetBuilder(webIds.alice).build();
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

  Scenario: Bob cannot read the container or its children
    Given url test.containerUrl
    And headers clients.bob.getAuthHeaders('GET', test.containerUrl)
    When method GET
    Then status 403

  Scenario: Bob cannot read the intermediate container
    Given url test.intermediateContainerUrl
    And headers clients.bob.getAuthHeaders('GET', test.intermediateContainerUrl)
    When method GET
    Then status 403

  Scenario: Bob cannot read the resource
    Given url test.resourceUrl
    And headers clients.bob.getAuthHeaders('GET', test.resourceUrl)
    When method GET
    Then status 403

