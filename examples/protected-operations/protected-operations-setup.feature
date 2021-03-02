@ignore
Feature: Set up a sample turtle file for Alice with defined access set for Bob

  @name=setupAccessTo # input: { bobAccessModes }
  Scenario: Create resource with access for Bob
    * def solidClientAlice = authenticate('alice')
    * def solidClientBob = authenticate('bob')
    * def testContainer = createTestContainer(solidClientAlice)
    * def resource = testContainer.createChildResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');
    * assert resource.exists()
    * def acl =
    """
      aclPrefix
       + createOwnerAuthorization(target.users.alice.webID, resource.getPath())
       + createBobAccessToAuthorization(target.users.bob.webID, resource.getPath(), bobAccessModes)
    """
    * assert resource.setAcl(acl)

  @name=setupDefault # input: { bobAccessModes }
  Scenario: Create resource with default access for Bob
    * def solidClientAlice = authenticate('alice')
    * def solidClientBob = authenticate('bob')
    * def testContainer = createTestContainer(solidClientAlice)
    * def resource = testContainer.createChildResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');
    * assert resource.exists()
    * def acl =
    """
      aclPrefix
       + createOwnerAuthorization(target.users.alice.webID, resource.getContainer().getPath())
       + createBobDefaultAuthorization(target.users.bob.webID, resource.getContainer().getPath(), bobAccessModes)
    """
    * assert resource.getContainer().setAcl(acl)
