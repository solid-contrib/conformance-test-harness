Feature: Requests support content negotiation for Turtle resource

  Background:
    # call the setup function to create the resource/acl and get the access tokens for alice and bob
    * def RDFUtils = Java.type('org.solid.testharness.utils.RDFUtils')
    * def testContext = callonce read('this:content-negotiation-setup.feature@name=setupTurtle')
    * configure headers = { Authorization: '#(testContext.aliceAuthHeader)' }
    * url target.serverRoot + testContext.resourcePath

    # prepare the teardown function
    * configure afterFeature = function() {Java.type('org.solid.testharness.utils.HttpUtils').deleteResourceRecursively(testContext.containerUrl, testContext.aliceAuthHeader)}

  Scenario: Alice can read the TTL example as JSON-LD
    Given header Accept = 'application/ld+json'
    When method GET
    Then status 200
    And match header Content-Type contains 'application/ld+json'
    * match RDFUtils.jsonLdToTripleArray(JSON.stringify(response), testContext.resourceUrl) contains testContext.sample

  Scenario: Alice can read the TTL example as TTL
    Given header Accept = 'text/turtle'
    When method GET
    Then status 200
    And match header Content-Type contains 'text/turtle'
    And match RDFUtils.turtleToTripleArray(response, testContext.resourceUrl) contains testContext.sample
