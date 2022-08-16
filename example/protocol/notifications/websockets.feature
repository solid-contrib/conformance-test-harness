Feature: Test websockets

  Background: Setup test container and get subscription endpoint
    # find notification gateway
    Given url resolveUri(rootTestContainer.url, '/.well-known/solid')
    And header Accept = 'text/turtle'
    When method GET
    Then status 200

    * def model = parse(response, 'text/turtle', rootTestContainer.url)
    * def notificationPredicate = iri(SOLID, 'notificationGateway')
    * assert model.contains(null, notificationPredicate, null)
    * def notificationGateway = model.objects(null, notificationPredicate)[0]

    # get websocket endpoint
    Given url notificationGateway
    And header Accept = 'application/ld+json'
    And header Content-Type = 'application/ld+json'
    And request {"@context": ["https://www.w3.org/ns/solid/notification/v1"], "type": ["WebSocketSubscription2021"], "protocols": ["ws"]}
    When method POST
    Then status 200
#    And match response.type == 'WebSocketSubscription2021'
    And match response.endpoint == '#notnull'
    * def wsEndpoint = response.endpoint

    * def testContainer = rootTestContainer.createContainer()

  Scenario: test
    Given url wsEndpoint
    And headers clients.alice.getAuthHeaders('POST', wsEndpoint)
    And header Content-Type = 'application/ld+json'
    And header Accept = 'application/ld+json'
    And request {@context: ['https://www.w3.org/ns/solid/notification/v1'], type: 'WebSocketSubscription2021', topic: '#(testContainer.url)'}
    When method POST
    Then status 200
    * def endpoint = response.endpoint

    * def containerSocket = karate.webSocket(endpoint, null, {subProtocol: 'solid-0.2'})
    * assert containerSocket != null
    * def resource = testContainer.createResource('.txt', 'Hello World!', 'text/plain');
    * listen 5000
    * def model = parse(listenResult, 'application/ld+json', testContainer.url)
    * assert model.contains(null, iri(RDF, 'type'), iri('https://www.w3.org/ns/activitystreams#Update'))
    * assert model.contains(null, iri('https://www.w3.org/ns/activitystreams#object'), iri(testContainer.url))
    * assert model.contains(null, iri('https://www.w3.org/ns/activitystreams#published'), null)
    # actor - not consistently used yet and may return container not webid
#    * assert model.contains(null, iri('https://www.w3.org/ns/activitystreams#actor'), iri(webIds.alice))
    * resource.delete()
    * listen 5000
    * def resourceModel = parse(listenResult, 'application/ld+json', testContainer.url)
    * print resourceModel.asTriples()


  @ignore
  Scenario: example for solid-0.1
    * def childContainer = testContainer.reserveContainer()
    Given url testContainer.url
    And headers clients.alice.getAuthHeaders('HEAD', testContainer.url)
    When method HEAD
    * def containerUpdateUrl = responseHeaders['Updates-Via'][0]
    * def containerSocket = karate.webSocket(containerUpdateUrl, null, {subProtocol: 'solid-0.1'})
    * def authHeaders = clients.alice.getAuthHeaders('GET', testContainer.url)
    * containerSocket.send(`auth ${authHeaders['Authorization']}`)
    * containerSocket.send(`dpop ${authHeaders['DPoP']}`)
    * containerSocket.send(`sub ${testContainer.url}`)

    * def childSocket = karate.webSocket(containerUpdateUrl, null, {subProtocol: 'solid-0.1'})
    * def authHeaders2 = clients.alice.getAuthHeaders('GET', childContainer.url)
    * childSocket.send(`auth ${authHeaders2['Authorization']}`)
    * childSocket.send(`dpop ${authHeaders2['DPoP']}`)
    * childSocket.send(`sub ${childContainer.url}`)

  #    * def childSocket = karate.webSocket(childContainer.url.replace(/https?:\/\//, 'ws://'))
    * childContainer.instantiate()

    ## after - disconnect clients

  @ignore
  Scenario: emits websockets-pubsub on the existing container
    * pause(1000)
    And def result = containerSocket.listen(5000)
    * print result
    Then match result contains `ack ${testContainer.url}`
    And def result = containerSocket.listen(5000)
    * print result
    Then match result contains `pub ${testContainer.url}`

#  Scenario: emits websockets-pubsub on the new container
#    * pause(1000)
    And def result2 = childSocket.listen(5000)
    * print result2
    Then match result2 contains `ack ${childContainer.url}`
    And def result2 = childSocket.listen(5000)
    * print result2
    Then match result2 contains `pub ${childContainer.url}`
