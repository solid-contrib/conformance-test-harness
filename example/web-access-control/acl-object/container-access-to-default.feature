Feature: Bob can read a container and its children if he is granted both direct and inheritable read access

  Background: Create test container with direct and inheritable read-only access for Bob
    * def setup =
    """
      function() {
        const testContainer = rootTestContainer.createContainer();
        const access = testContainer.accessDatasetBuilder
                .setAgentAccess(testContainer.url, webIds.bob, ['read'])
                .setInheritableAgentAccess(testContainer.url, webIds.bob, ['read'])
                .build();
        testContainer.accessDataset = access;
        const intermediateContainer = testContainer.reserveContainer();
        const resource = intermediateContainer.createResource('.txt', 'hello', 'text/plain')
        return {
          containerUrl: testContainer.url,
          intermediateContainerUrl: intermediateContainer.url,
          resourceUrl: resource.url
        }
      }
    """
    * def test = callonce setup

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

