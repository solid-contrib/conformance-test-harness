Feature: Bob cannot read an RDF resource to which he is not granted read access

  Background:
    # call the setup function to create the resource/acl and get the access tokens for alice and bob
    * def testContext = callonce read('this:protected-operations-setup.feature@name=setupAccessTo') { bobAccessModes: 'acl:Append, acl:Write, acl:Control' }
    * configure headers = { Authorization: '#(testContext.bobAuthHeader)' }
    * url target.serverRoot + testContext.resourcePath

    # prepare the teardown function
    * configure afterFeature = function() {Java.type('org.solid.testharness.utils.SolidClient').deleteResourceRecursively(testContext.containerUrl, testContext.aliceAuthHeader)}

  Scenario: Bob can get the resource OPTIONS
    When method OPTIONS
    Then status 204

  Scenario: Bob cannot read the resource with GET
    When method GET
    Then status 403

  Scenario: Bob cannot read the resource with HEAD
    When method HEAD
    Then status 403
