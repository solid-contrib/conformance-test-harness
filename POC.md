# Solid Specification Test Conformance Suite - Test Harness

**NOTE:** This project is derived from the initial PoC used to review the potential architecture and as
such still has some rough edges. There will be a lot of changes in the near future but comments are always welcome. 

## Prerequisites
The example test cases have been run against ESS in ACL compatibility mode, CSS and NSS. They require 2 user accounts to
be made available via an IdP: alice and bob. The profiles for these users may need additional information adding to them:
* Trusted app:
```
:me acl:trustedApp [
  acl:mode acl:Read, acl:Write;
  acl:origin <https://tester>
];
```
* Solid Identity Provider: (where the URL is the IdP of this account) - NSS does not add this to new profiles by default
```
:me solid:oidcIssuer <https://inrupt.net/>;
```

If you are planning to use accounts that do not own PODs on the target server then you will also need to provide a container
on the target server for the tests that has been granted full access control for the test user.

There are 2 approaches to authentication, refresh tokens and session based login. If the target test server and the chosen IdP
are compatible (which they should be) then either mechanism can be used to get the access tokens required to run the tests. 

### Refresh tokens
This relies on getting a refresh token from an IdP (e.g. https://broker.pod-compat.inrupt.com/) and exchanging that for an
access token in order to run the tests. The refresh token can be created using a simple bootstrap process:
```shell
npx @inrupt/generate-oidc-token
```

The configuration that must be saved for each user is:
* WebID
* Client Id
* Client Secret
* Refresh Token

Unfortunately, this process requires a user to go the broker's web page, log in and authorize the application. Also, the
refresh tokens expire and would need to be recreated regularly. This is not suitable for a CI environment so alternatives 
are bing considered such as a Mock IdP. 
 
This mechanism will not work for NSS until support for refresh tokens is added: See https://github.com/solid/node-solid-server/issues/1533

### Session based login
Some IdPs make is easy to authenticate without a browser by supporting form based login and sessions. The test harness has
the capability to use this mechanism to login and get access tokens. The configuration that must be saved for each user is:
* WebID
* Username
* Password
  
The harness also needs to know the login path to use on the IdP and the origin that has been registered as the trusted app 
for the users.

This mechanism will work in CI environments and the passwords could be passed in as external secrets.


## Target server configuration

The config for the server(s) under test goes in `config.ttl`. An example of this is:
```turtle
@base <https://github.com/solid/conformance-test-harness/> .
@prefix test-harness: <https://github.com/solid/conformance-test-harness/> .
@prefix solid-test: <https://github.com/solid/conformance-test-harness/vocab#> .

@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix doap: <http://usefulinc.com/ns/doap#> .
@prefix earl: <http://www.w3.org/ns/earl#> .
@prefix solid: <http://www.w3.org/ns/solid/terms#> .
@prefix dcterms: <http://purl.org/dc/terms/> .

<> a earl:Software ;
   doap:name "Solid Specification Conformance Test Harness"@en ;
   doap:description "A test harness that will run suites of tests related to Solid specifications."@en ;
   doap:created: "2021-02-16"^^xsd:date ;
   doap:developer <https://inrupt.com/profile/card/#us>;
   doap:homepage <https://github.com/solid/conformance-test-harness> ;
   doap:release [
                  doap:revision "0.0.1-SNAPSHOT"
                ] .

<ess-compat>
  a earl:Software, earl:TestSubject ;
  doap:name "Enterprise Solid Server (Web Access Control version)";
  doap:release [
                 doap:name "ESS 1.0.9";
                 doap:revision "1.0.9";
                 doap:created "2021-03-05"^^xsd:date
               ];
  doap:developer <https://inrupt.com/profile/card/#us>;
  doap:homepage <https://inrupt.com/products/enterprise-solid-server>;
  doap:description "A production-grade Solid server produced and supported by Inrupt."@en;
  doap:programming-language "Java" ;
  solid:oidcIssuer <https://inrupt.net> ;
  solid:loginEndpoint <https://inrupt.net/login/password> ;
  solid-test:origin <https://tester> ;
  solid-test:aliceUser [
                         solid-test:webId <https://solid-test-suite-alice.inrupt.net/profile/card#me> ;
                         solid-test:credentials "inrupt-alice.json"
                       ] ;
  solid-test:bobUser [
                       solid-test:webId <https://solid-test-suite-bob.inrupt.net/profile/card#me> ;
                       solid-test:credentials "inrupt-bob.json"
                     ] ;
  solid-test:maxThreads 8 ;
  solid-test:features "authentication", "acl", "wac-allow" ;
  solid-test:serverRoot <https://pod-compat.inrupt.com> ;
  solid-test:podRoot <https://pod-compat.inrupt.com/solid-test-suite-alice/> ;
  solid-test:testContainer "/solid-test-suite-alice/shared-test/" .
```
First there is a description of this test harness, then sections to define each server to be tested including the user accounts, and the features that the server supports.

There is a sample of this file in the `config/config.ttl` folder and this will be used unless you override this location as shown below.

This system needs user credentials for authentication, but these should not be kept in the file itself. There is a reference to an
external JSON file which can be shared between multliple servers and has the following format:
```json5
{
  "webID":"https://pod-compat.inrupt.com/solid-test-suite-alice/profile/card#me",
  // EITHER
  "refreshToken": "",
  "clientId": "",
  "clientSecret": ""
  // OR
  "username": "",
  "password": ""
}
```

## Setting up the environment
There 5 important settings:
* `target` - the name of the target server, used to select the server config from the config file
* `configFile` - the location of the config file
* `credentialsDir` - the location of the shared credentials files if used
* `testSuiteDescription` - the location of the test suite description document that lists the test cases to be run
* `feature:mappings` - maps test cases IRIs to a local file system (there can be multiple mappings)

There are 2 ways to set these properties. Firstly you can provide `config/application.yaml` in the working directory containing:
```yaml
target: TARGET_SERVER
configFile: PATH_TO_CONFIG
credentialsDir: PATH_TO_CREDENTIALS
testSuiteDescription: PATH_TO_TESTSUITE_DOC
feature:
  mappings:
    - prefix: https://github.com/solid/conformance-test-harness/example
      path: example
```
This method works well when testing your tests in an IDE as it doesn't require anything adding to the command line.

Alternatively you can set these things on the command line:
```
-Dtarget=TARGET_SERVER
-DconfigFile=PATH_TO_CONFIG
-DcredentialsDir=PATH_TO_CREDENTIALS
-testSuiteDescription=PATH_TO_TESTSUITE_DOC
``` 

## Running the test suite
To run the test suite with the default target server as defined in `config/application.yaml`:

```shell
# this uses a profile to run the TestSuiteRunner instead of local unit tests
mvn test -Psolid
```
To run the test suite with a specific target server:
```shell
mvn test -Psolid -Dtarget=ess-compat
mvn test -Psolid -Dtarget=css
mvn test -Psolid -Dtarget=nss
```

Using an IDE you can also run a specific scenario by editing the TestScenarioRunner and then running it as you would any unit test:
```Java
Results results = testRunner.runTests(Collections.singletonList("classpath:content-negotiation/content-negotiation-turtle.feature"));
```

You can also go to the TestSuiteRunnner class and run the whole test suite in the same way.  

## Test Reports
|Report|Location|
|------|--------|
|Summary report|`target/karate-reports/karate-summary.html`|
|Timeline|`target/karate-reports/karate-timeline.html`|

## Example test cases
In the future, all test cases will be pulled from an external repository (whether they are ultimately written in KarateDSL or RDF).
There are currently some examples in the `examples` folder to show some templates for how tests
can be defined.
* The content negotiation tests create RDF resources of different formats, then confirm that they can be accessed as other formats.
  It uses a Java library to convert Turtle or JSON-LD to triples to allow responses to be compared to the original test sample. Support for RDFa 
  is not consistent across all servers so that test is missed for now.
* The protect operations tests create a resource or container and then each test sets up different ACLs for it. The tests confirm that
  the Bob user has the correct access to the resource or container.
* The WAC allow tests create a resource and then each test sets up different ACLs for that resource. The tests parse the WAC-Allow
  header and confirm that the Bob user and un-authenticated users see the correct permissions.

## Test patterns

Each group of tests is created in its own folder with a shared setup feature. There are 2 variants of this demonstrated.

Karate based setup feature:
* fetches any access tokens that are required
* creates Authorization headers with these tokens
* creates any test resources required for the test
* adds ACLs if needed
* provides a link to the container created for the test to be used in teardown

Javascript function setup (see wac-allow tests):
* the setup feature contains a function used to set up the clients and a test resource - this is shared across all features in the group
* a separate setup function in each test feature is called once for that feature to set up the ACLs - this puts the specifics of the test
  feature into the feature file making them easier to read

The test files themselves:
* run a background task for each scenario to call the necessary setup procedure
* hold the returned test context to provide access to the Authorization headers and the test container or resource paths
* prepare the teardown function that will delete the resources created for the tests
* provide a set of scenarios that make http requests against the test resource and validate the responses

## Technical notes
* The TestSuiteRunner shows how tests can be selected for running and how to hook onto the report generation phase
* There are Java utility classes to provide common parts of the test implementation e.g. authentication, creating resources and ACLs, performing RDF conversions
* There is a Javascript library in `src/main/resources/setup.feature` demonstrating how you can also create common functions in Javascript

## Plans

### Short term
* [x] Sort out project structure to retain benefits of IDE whilst keeping tests separate from code.
* [x] Review the project build and file locations, particularly tests and resources which are a little confusing since everything is test related.
* [x] Provide an alternative authentication mechanism to support more servers.
* [x] Improve the configuration mechanism (including process of acquiring refresh tokens).
* [ ] Decide whether to implement test cases in RDF instead of KarateDSL - this would require a translation layer to be added before
  tests are passed to the TestSuiteRunner. We would also want to see if we could still enable the IDE integration that allows individual
  tests to be run.
* [ ] Add the facility to gather test cases from an external repository.
* [ ] Add a Dockerfile, so the tests can be run in a container.
* [ ] Add unit tests for the harness components themselves [IN PROGRESS].
* ...

### Longer term
* [ ] Read test case descriptions from test suite description document [IN PROGRESS].
* [x] Report results in EARL.
* [ ] Create the overall conformance report (matrix of all servers tested) 
* ...
