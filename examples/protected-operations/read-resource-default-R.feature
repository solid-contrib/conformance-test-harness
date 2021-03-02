Feature: Bob can only read an RDF resource to which he is only granted default read access via the parent

  Background: Create test resource with default read-only access for Bob
    * def testContext = callonce read('this:protected-operations-setup.feature@name=setupDefault') { bobAccessModes: 'acl:Read' }
    * def resourceUrl = testContext.resource.getUrl()
    * url resourceUrl

    * configure afterFeature = function() {testContext.resource.getContainer().delete()}

  Scenario: Bob can read the resource with GET
    Given configure headers = testContext.solidClientBob.getAuthHeaders('GET', resourceUrl)
    When method GET
    Then status 200

  Scenario: Bob can read the resource with HEAD
    Given configure headers = testContext.solidClientBob.getAuthHeaders('HEAD', resourceUrl)
    When method HEAD
    Then status 200

  Scenario: Bob can read the resource with OPTIONS
    Given configure headers = testContext.solidClientBob.getAuthHeaders('OPTIONS', resourceUrl)
    When method OPTIONS
    Then status 204

  Scenario: Bob cannot PUT to the resource
    Given request '<> <http://www.w3.org/2000/01/rdf-schema#comment> "Bob replaced it." .'
    And configure headers = testContext.solidClientBob.getAuthHeaders('PUT', resourceUrl)
    And header Content-Type = 'text/turtle'
    When method PUT
    Then status 403

  Scenario: Bob cannot PATCH the resource
    Given request 'INSERT DATA { <> a <http://example.org/Foo> . }'
    And configure headers = testContext.solidClientBob.getAuthHeaders('PATCH', resourceUrl)
    And header Content-Type = 'application/sparql-update'
    When method PATCH
    Then status 403

  Scenario: Bob cannot POST to the resource
    Given request '<> <http://www.w3.org/2000/01/rdf-schema#comment> "Bob replaced it." .'
    And configure headers = testContext.solidClientBob.getAuthHeaders('POST', resourceUrl)
    And header Content-Type = 'text/turtle'
    When method POST
    Then status 403

  Scenario: Bob cannot DELETE the resource
    Given configure headers = testContext.solidClientBob.getAuthHeaders('DELETE', resourceUrl)
    When method DELETE
    Then status 403
