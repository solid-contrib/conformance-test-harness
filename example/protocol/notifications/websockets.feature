Feature: Test websockets

  Background: Setup test container and get subscription endpoint
    * def testContainer = rootTestContainer.createContainer()

    # find notification gateway
#    Given url resolveUri(rootTestContainer.url, '/.well-known/solid')
#    And header Accept = 'application/ld+json'
#    When method GET
#    Then status 200

    # find websocket endpoint
#    Given url response.notificationGateway
    Given url 'https://notification.pod.inrupt.com'
    And header Accept = 'application/json'
    And header Content-Type = 'application/json'
    And request { "protocols": ["ws"] }
    When method POST
    Then def wsEndpoint = response.endpoint
    Then status 200

  Scenario: test
#    * def subscriptionEndpoint = resolveUri(wsEndpoint, '/subscription')
    Given url wsEndpoint
    And headers clients.alice.getAuthHeaders('POST', wsEndpoint)
    And header Content-Type = 'application/json'
    And header Accept = 'application/json'
    And request ({topic: testContainer.url})
    # type: 'WebSocketSubscription2021',
    When method POST
    Then status 200

    #, null, {subProtocol: 'solid-0.1'}) {subProtocol: 'solid-0.2'}
    * def handler = function(msg){ karate.log('msg:', msg); return false }
    * def handlerT = function(msg){ return true }
    * def handlerF = function(msg){ return false }
    * def containerSocket = karate.webSocket(response.endpoint, null, {subProtocol: response.subprotocol})

    * def resource = testContainer.createResource('.ttl', karate.readAsString('../fixtures/example.ttl'), 'text/turtle');
    * pause(2000)
    And def result = containerSocket.listen(5000)
    * print result
    * def expected =
    """
    {
      type: ["http://www.w3.org/ns/prov#Activity", "Update"],
      actor: [webIds.alice],
      object: {
        id: testContainer.url,
        type: [
          "http://www.w3.org/ns/ldp#BasicContainer",
          "http://www.w3.org/ns/ldp#Container",
          "http://www.w3.org/ns/ldp#RDFSource",
          "http://www.w3.org/ns/ldp#Resource"
        ]
      }
    }
    """
    * expected.actor = [webIds.alice]
    * expected.object.id = testContainer.url
    Then match JSON.parse(result) contains deep expected

    * pause(2000)
    And def result2 = containerSocket.listen(5000)
    * print result2
    Then match JSON.parse(result2) contains deep expected

    * def resource2 = testContainer.createResource('.txt', 'hello', 'text/plain')
    * pause(2000)
    And def result3 = containerSocket.listen(5000)
    * print result3
    Then match JSON.parse(result3) contains deep expected


#    * def res =
#    """
#    {
#  "id": "urn:uuid:07cb0e24-b3cb-4ca1-a4ea-72455737945f",
#  "type": [
#    "http://www.w3.org/ns/prov#Activity",
#    "Update"
#  ],
#  "actor": [
#    "https://pod.inrupt.com/solid-test-suite-alice/profile/card#me"
#  ],
#  "object": {
#    "id": "https://pod.inrupt.com/solid-test-suite-alice/",
#    "state": "b465236f27aae35a3c3ab19ffcb773b7a27bb3b78b78879c5f627a2479034bef",
#    "type": [
#      "http://www.w3.org/ns/ldp#BasicContainer",
#      "http://www.w3.org/ns/ldp#Container",
#      "http://www.w3.org/ns/ldp#RDFSource",
#      "http://www.w3.org/ns/ldp#Resource"
#    ]
#  },
#  "published": "2022-03-28T15:44:12.883808Z",
#  "@context": [
#    "https://www.w3.org/ns/activitystreams",
#    {
#      "state": {
#        "@id": "http://www.w3.org/2011/http-headers#etag"
#      }
#    }
#  ]
#}
#    """

#  "@context": ["https://www.w3.org/ns/solid/notification/v1"],
#  "type": "WebSocketSubscription2021",
#  "topic": "https://storage.example/resource",
#  "state": "opaque-state",
#  "expiration": "2021-12-23T12:37:15Z",
#  "rate": "PT10s"
#  }

  @ignore
  Scenario: hid

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

#  Scenario: text messages
#    * def socket = karate.webSocket('ws://echo.websocket.org')
#    When socket.send('hello world!')
#    And def result = socket.listen(5000)
#    Then match result == 'hello world!'
#
#    When socket.send('another test')
#    And def result = socket.listen(5000)
#    Then match result == 'another test'
#
#  Scenario: binary message
#    * def socket = karate.webSocketBinary('ws://echo.websocket.org')
#    And bytes data = read('../upload/test.pdf')
#    When socket.sendBytes(data)
#    And def result = socket.listen(5000)
#    # the result data-type is byte-array, but this comparison works
#    Then match result == read('../upload/test.pdf')
#
#  Scenario: sub protocol
#    Given def demoBaseUrl = 'wss://subscriptions.graph.cool/v1/cizfapt9y2jca01393hzx96w9'
#    And def options = { subProtocol: 'graphql-subscriptions', headers: { Authorization: 'Bearer foo' } }
#    And def socket = karate.webSocket(demoBaseUrl, null, options)
#    And def txt = '{"type": "connection_init", "payload": {}}'
#    When socket.send(txt)
#    And def result = socket.listen(5000)
#    Then match result == { type: 'connection_ack' }


#  Scenario: Test notification
#    * def handler = function(msg){ return msg.startsWith('hello') }
#    * def socket = karate.webSocket(demoBaseUrl + '/websocket', handler)
#    * socket.send('Billie')
#    * def result = socket.listen(5000)
#    * match result == 'hello Billie !'