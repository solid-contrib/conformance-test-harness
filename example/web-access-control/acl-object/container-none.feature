Feature: Bob cannot read a container or children if he is not given any access

  Background: Create test container with no access for Bob
    * def setup =
    """
      function() {
        const testContainer = rootTestContainer.createContainer();
        const access = testContainer.getAccessDatasetBuilder(webIds.alice).build();
        if (testContainer.setAccessDataset(access)) {
          const intermediateContainer = testContainer.reserveContainer();
          const resource = intermediateContainer.createResource('.txt', 'hello', 'text/plain')
          return {
            containerUrl: testContainer.url,
            intermediateContainerUrl: intermediateContainer.url,
            resourceUrl: resource.url
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

