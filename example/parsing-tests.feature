Feature: Test RDF library support

  Background: Create a turtle resource
    * def turtleData = parse(karate.readAsString('protocol/fixtures/example.ttl'), 'text/turtle')
    * def typeData = parse(karate.readAsString('protocol/fixtures/types.ttl'), 'text/turtle')

    * def NS = 'http://example.org/'
    * def SUBJECT = iri(NS, '#test')
    * def STORE = 'http://store.example.com/'

  Scenario: Model contains passes
    * assert typeData.contains(typeData)

  Scenario: Model contains fails
    * assert typeData.contains(turtleData)

  Scenario: Model contains statement passes
    * assert typeData.contains(SUBJECT, iri(NS, '#iri'), iri(NS, "#world"))
    * assert typeData.contains(SUBJECT, iri(NS, '#string'), literal('text'))
    * assert typeData.contains(SUBJECT, iri(NS, '#stringen'), literal('texten', 'en'))
    * assert typeData.contains(SUBJECT, iri(NS, '#decimal'), literal('1.2', iri(XSD, 'decimal')))
    * assert typeData.contains(SUBJECT, iri(NS, '#short'), literal('1', iri(XSD, 'short')))
    * assert typeData.contains(SUBJECT, iri(NS, '#integer'), literal('1234567890', iri(XSD, 'integer')))
    * assert typeData.contains(SUBJECT, iri(NS, '#int'), literal('1234', iri(XSD, 'int')))
    * assert typeData.contains(SUBJECT, iri(NS, '#long'), literal('1234567890', iri(XSD, 'long')))
    * assert typeData.contains(SUBJECT, iri(NS, '#float'), literal('1234567890.0', iri(XSD, 'float')))
    * assert typeData.contains(SUBJECT, iri(NS, '#double'), literal('1234567890.0', iri(XSD, 'double')))
    * assert typeData.contains(SUBJECT, iri(NS, '#date'), literal('2021-11-01', iri(XSD, 'date')))
    * assert typeData.contains(SUBJECT, iri(NS, '#dateTime'), literal('2021-11-01T15:14:13.000Z', iri(XSD, 'dateTime')))
    * assert typeData.contains(SUBJECT, iri(NS, '#boolean'), literal('true', iri(XSD, 'boolean')))

    * assert typeData.contains(SUBJECT, iri(NS, '#decimal'), literal(new java.math.BigDecimal('1.2')))
    * assert typeData.contains(SUBJECT, iri(NS, '#integer'), literal(new java.math.BigInteger('1234567890')))
    * assert typeData.contains(SUBJECT, iri(NS, '#int'), literal(1234))
    * assert typeData.contains(SUBJECT, iri(NS, '#boolean'), literal(true))

  Scenario: Model contains statement with IRI fails
    * assert typeData.contains(SUBJECT, iri(NS, '#iri'), iri(NS, 'hello'))

  Scenario: Model contains statement with text fails
    * assert typeData.contains(SUBJECT, iri(NS, '#stringen'), literal('text'))

  Scenario: Model contains statement with text@en fails
    * assert typeData.contains(SUBJECT, iri(NS, '#stringen'), literal('text', 'fr'))

  Scenario: Test subjects match
    * match typeData.subjects(iri(NS, '#string'), literal('text')) contains NS + '#test'
    * match typeData.subjects(null, null) contains NS + '#test'

  Scenario: Test predicates match
    * match typeData.predicates(SUBJECT, literal('text')) contains NS + '#string'
    * match typeData.predicates(null, null) contains NS + '#string'

  Scenario: Test objects match
    * match typeData.objects(SUBJECT, iri(NS, '#string')) contains 'text'
    * match typeData.objects(null, null) contains 'text'

  Scenario: Json model contains statements
    * def jsonData = parse(karate.readAsString('protocol/fixtures/example.json'), 'application/ld+json')
    * assert jsonData.contains(iri(STORE), iri(STORE, 'name'), literal('Links Bike Shop'))
    * assert jsonData.contains(iri(STORE), iri(RDF, 'type'), iri(STORE, 'Store'))
    * match jsonData.objects(iri(STORE), iri(STORE, 'name')) contains 'Links Bike Shop'
