Feature: Bob can only read an RDF resource to which he is only granted read access

  Background:
    # call the setup function to create the resource/acl and get the access tokens for alice and bob
    * def testContext = callonce read('this:protected-operations-setup.feature@name=setupAccessTo') { bobAccessModes: 'acl:Read' }
    * configure headers = { Authorization: '#(testContext.bobAuthHeader)' }
    * url target.serverRoot + testContext.resourcePath

    # prepare the teardown function
    * configure afterFeature = function() {Java.type('org.solid.testharness.utils.SolidClient').deleteResourceRecursively(testContext.containerUrl, testContext.aliceAuthHeader)}

  Scenario: Bob can read the resource with GET
    When method GET
    Then status 200

  Scenario: Bob can read the resource with HEAD
    When method HEAD
    Then status 200

  Scenario: Bob can read the resource with OPTIONS
    When method OPTIONS
    Then status 204

  Scenario: Bob cannot PUT to the resource
    Given request '<> <http://www.w3.org/2000/01/rdf-schema#comment> "Bob replaced it." .'
    And header Content-Type = 'text/turtle'
    When method PUT
    Then status 403

  Scenario: Bob cannot PATCH the resource
    Given request 'INSERT DATA { <> a <http://example.org/Foo> . }'
    And header Content-Type = 'application/sparql-update'
    When method PATCH
    Then status 403

  Scenario: Bob cannot POST to the resource
    Given request '<> <http://www.w3.org/2000/01/rdf-schema#comment> "Bob replaced it." .'
    And header Content-Type = 'text/turtle'
    When method POST
    Then status 403

  Scenario: Bob cannot DELETE the resource
    When method DELETE
    Then status 403

#  Scenario: Bob cannot use an unknown method on the resource
#    When method 'DAHU'
#    Then status 400