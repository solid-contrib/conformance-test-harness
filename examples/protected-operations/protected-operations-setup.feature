@ignore
Feature: Set up a sample turtle file for Alice with defined access set for Bob

  Background:
    * callonce read('classpath:utils.feature')
    * def resourcePath = getRandomResourcePath('.ttl')

  # Get access tokens then create a resource and ACL
  @name=setupAccessTo # input: { bobAccessModes }
  Scenario:
    * def solidClient = authenticate('alice')
    * def solidClientBob = authenticate('bob')
    * def exampleTurtle = karate.readAsString('../fixtures/example.ttl')
    * def resource = new SolidResource(solidClient, target.serverRoot + resourcePath, exampleTurtle, 'text/turtle')
    * assert resource != null
    * def containerUrl = resource.getParentUrl()
    * def acl =
    """
      aclPrefix
       + createOwnerAuthorization(target.users.alice.webID, resourcePath)
       + createBobAccessToAuthorization(target.users.bob.webID, resourcePath, bobAccessModes)
    """
    * match resource.setAcl(acl) == true

  @name=setupDefault # input: { bobAccessModes }
  Scenario:
    * def solidClient = authenticate('alice')
    * def solidClientBob = authenticate('bob')
    * def exampleTurtle = karate.readAsString('../fixtures/example.ttl')
    * def resource = new SolidResource(solidClient, target.serverRoot + resourcePath, exampleTurtle, 'text/turtle')
    * assert resource != null
    * def containerUrl = resource.getParentUrl()
    * def containerPath = resource.getParentPath()
    * def acl =
    """
      aclPrefix
       + createOwnerAuthorization(target.users.alice.webID, containerPath)
       + createBobDefaultAuthorization(target.users.bob.webID, containerPath, bobAccessModes)
    """
    * match resource.setParentAcl(acl) == true
