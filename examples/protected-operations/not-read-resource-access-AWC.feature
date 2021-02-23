Feature: Bob cannot read an RDF resource to which he is not granted read access

  Background:
    # call the setup function to create the resource/acl and get the access tokens for alice and bob
    * def testContext = callonce read('this:protected-operations-setup.feature@name=setupAccessTo') { bobAccessModes: 'acl:Append, acl:Write, acl:Control' }
    * def resourceUrl = target.serverRoot + testContext.resourcePath
    * url resourceUrl

    # prepare the teardown function
    * configure afterFeature = function() {Java.type('org.solid.testharness.http.SolidClient').deleteResourceRecursively(testContext.containerUrl, 'alice')}

  Scenario: Bob can get the resource OPTIONS
    Given configure headers = testContext.solidClientBob.getAuthHeaders('OPTIONS', resourceUrl)
    When method OPTIONS
    Then status 204

  Scenario: Bob cannot read the resource with GET
    Given configure headers = testContext.solidClientBob.getAuthHeaders('GET', resourceUrl)
    When method GET
    Then status 403

  Scenario: Bob cannot read the resource with HEAD
    Given configure headers = testContext.solidClientBob.getAuthHeaders('HEAD', resourceUrl)
    When method HEAD
    Then status 403
