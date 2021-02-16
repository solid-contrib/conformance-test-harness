@ignore
Feature: Set up a new container with sample file for Alice

  Background:
    * callonce read('classpath:utils.feature')
    * def containerPath = getRandomContainerPath()

  @name=setupTurtle
  Scenario:
    * def resourcePath = containerPath + 'example.ttl'
    * def exampleTurtle = karate.readAsString('../fixtures/example.ttl')
    * def aliceAuthHeader = getAuthHeader('alice')
    * def httpUtils = new HttpUtils(aliceAuthHeader)
    * def resource = new SolidResource(httpUtils, target.serverRoot + resourcePath, exampleTurtle, 'text/turtle')
    * assert resource != null
    * def containerUrl = resource.getParentUrl()
    * def resourceUrl = target.serverRoot + resourcePath
    * def sample = RDFUtils.turtleToTripleArray(exampleTurtle, resourceUrl)

  @name=setupJson
  Scenario:
    * def resourcePath = containerPath + 'example.json'
    * def exampleJson = karate.readAsString('../fixtures/example.json')
    * def aliceAuthHeader = getAuthHeader('alice')
    * def httpUtils = new HttpUtils(aliceAuthHeader)
    * def resource = new SolidResource(httpUtils, target.serverRoot + resourcePath, exampleJson, 'application/ld+json')
    * assert resource != null
    * def containerUrl = resource.getParentUrl()
    * def resourceUrl = target.serverRoot + resourcePath
    * def sample = RDFUtils.jsonLdToTripleArray(exampleJson, resourceUrl)

