@ignore
Feature: Set up a sample turtle file for Alice with defined access set for Bob

  @name=setupAccessTo # input: { bobAccessModes }
  Scenario: Create resource with access for Bob
    * def testContainer = createTestContainer()
    * def resource = testContainer.createChildResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');
    * assert resource.exists()
    * def acl =
    """
      aclPrefix
       + createOwnerAuthorization(webIds.alice, resource.getPath())
       + createBobAccessToAuthorization(webIds.bob, resource.getPath(), bobAccessModes)
    """
    * assert resource.setAcl(acl)

  @name=setupDefault # input: { bobAccessModes }
  Scenario: Create resource with default access for Bob
    * def testContainer = createTestContainer()
    * def resource = testContainer.createChildResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');
    * assert resource.exists()
    * def acl =
    """
      aclPrefix
       + createOwnerAuthorization(webIds.alice, resource.getContainer().getPath())
       + createBobDefaultAuthorization(webIds.bob, resource.getContainer().getPath(), bobAccessModes)
    """
    * assert resource.getContainer().setAcl(acl)
