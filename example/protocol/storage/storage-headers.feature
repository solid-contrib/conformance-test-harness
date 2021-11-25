Feature: Finding the storage for a resource

  Background: Set test file
    * def testContainer = rootTestContainer.reserveContainer()
    * def resource = testContainer.createResource('.txt', 'hello', 'text/plain')

  Scenario: Test Storage finding
    # Confirm resource is not a Storage
    * assert !resource.storageType

    # Find storage as Alice
    * def storage = resource.findStorage()
    * assert storage != null
    Given url storage.url
    And headers clients.alice.getAuthHeaders('GET', storage.url)
    And header Accept = 'text/turtle'
    When method GET
    Then status 200
    And assert storage.storageType

    # Use a local function to check the link headers
    * def hasStorageType = (ls) => ls.findIndex(l => l.rel == 'type' && l.uri == 'http://www.w3.org/ns/pim/space#Storage') != -1
    * def links = parseLinkHeaders(responseHeaders)
    And assert hasStorageType(links)

    # Find storage as Bob fails since he doesn't have access
    * def resourceBob = SolidResource.create(clients.bob, resource.url, null, null)
    * def storageBob = resourceBob.findStorage()
    * assert storageBob == null

    # Grant Bob read access to the storage root
    * def acl = storage.accessDatasetBuilder.setAgentAccess(storage.url, webIds.bob, ['read']).build()
    * storage.setAccessDataset(acl)

    # Find storage as Bob now succeeds since he has access
    * def storageBob = resourceBob.findStorage()
    * assert storageBob != null
    Given url storageBob.url
    And headers clients.bob.getAuthHeaders('GET', storageBob.url)
    And header Accept = 'text/turtle'
    When method GET
    Then status 200
    And assert storageBob.storageType

    # Remove Bob's access
    Given url storage.url
    And headers clients.alice.getAuthHeaders('PATCH', storage.url)
    And header Content-Type = 'application/sparql-update'
    And request acl.asSparqlInsert().replace('INSERT DATA', 'DELETE DATA')
    When method PATCH
    Then status 204

    # Find storage as Bob fails again since he no longer has access
    * def storageBob = resourceBob.findStorage()
    * assert storageBob == null
