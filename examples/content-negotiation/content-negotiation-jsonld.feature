@ignore
Feature: Requests support content negotiation for JSON-LD resource

  Background:
    * def RDFUtils = Java.type('org.solid.testharness.utils.RDFUtils')
    * def testContext = callonce read('this:content-negotiation-setup.feature@name=setupJson')
    * configure headers = { Authorization: '#(testContext.aliceAuthHeader)' }
    * url target.serverRoot + testContext.resourcePath

    # prepare the teardown function
    * configure afterFeature = function() {Java.type('org.solid.testharness.utils.SolidClient').deleteResourceRecursively(testContext.containerUrl, testContext.aliceAuthHeader)}

  Scenario: Alice can read the JSON-LD example as JSON-LD
    Given header Accept = 'application/ld+json'
    When method GET
    Then status 200
    * print response
    And match header Content-Type contains 'application/ld+json'
    And match RDFUtils.jsonLdToTripleArray(JSON.stringify(response), testContext.resourceUrl) contains testContext.sample

  Scenario: Alice can read the JSON-LD example as TTL
    Given header Accept = 'text/turtle'
    When method GET
    Then status 200
    * print response
    And match header Content-Type contains 'text/turtle'
    And match RDFUtils.turtleToTripleArray(response, testContext.resourceUrl) contains testContext.sample
