Feature: Bob cannot read an RDF resource to which he is not granted read access

  Background: Create test resource with all access except read for Bob
    * callonce read('this:setup.feature@name=setupAccessTo') { bobAccessModes: 'acl:Append, acl:Write, acl:Control' }
    * def resourceUrl = resource.getUrl()
    * url resourceUrl

  Scenario: Bob can get the resource OPTIONS
    Given configure headers = clients.bob.getAuthHeaders('OPTIONS', resourceUrl)
    When method OPTIONS
    Then status 204

  Scenario: Bob cannot read the resource with GET
    Given configure headers = clients.bob.getAuthHeaders('GET', resourceUrl)
    When method GET
    Then status 403

  Scenario: Bob cannot read the resource with HEAD
    Given configure headers = clients.bob.getAuthHeaders('HEAD', resourceUrl)
    When method HEAD
    Then status 403
