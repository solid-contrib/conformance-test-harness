
# conformance-test-harness
The test harness used to run Solid conformance tests and generate reports.

<!-- MarkdownTOC -->

- [Repo structure](#repo-structure)
- [Architecture](#architecture)
	- [Test harness](#test-harness)
	- [Test executor](#test-executor)
	- [Test suite description document](#test-suite-description-document)
	- [Test cases](#test-cases)
	- [Conformance report](#conformance-report)
- [Component Architecture](#component-architecture)
- [Test suite description document](#test-suite-description-document-1)
- [Prerequisites](#prerequisites)
	- [Refresh tokens](#refresh-tokens)
	- [Session based login](#session-base-login)
- [Usage](#usage)
	- [Target server configuration](#tarfget-server-configuration)
    - [Setting up the environment](#setting-up-the-environment)
	- [Execution](#execution)
	- [Docker](#docker)
- [Test reports](#test-reports)
- [Writing tests](#writing-tests)
    - [Example test cases](#example-test-cases)
    - [Test patterns](#test-patterns)
	- [Tips](#test-tips)
- [Processes](#processes)

<!-- /MarkdownTOC -->


## Repo structure

* architecture : Architectural diagrams
* config : config files used to run the example test cases
* example : PoC test cases demonstrating use of the harness
* src/main : the source of the conformance test harness
* src/test : unit tests for the harness itself

## Architecture

![Solid conformance test suite architecture](architecture/architecture.png)

### Test harness

The test harness controls the overall execution of a test suite. It is responsible for loading the test suite, locating
the tests, creating and controlling test executors and generating test suite conformance reports.

The harness will provide different interfaces to the test suite such as a REST API and a command line interface, and
will be platform agnostic. 

### Test executor
The test executor is responsible for executing tests against a Solid server. The harness can create multiple test
executors where each is responsible for executing a set of tests. Test executors do not need to run on the same host as
the test harness. 

### Test suite description document
This is an RDF document containing metadata about the test suite. The metadata includes:

  * Prerequisites (e.g. dependencies)
  * Requirements level from the specification (e.g., MUST, SHOULD, MAY, etc.)
  * References to the appropriate section in the specification
  * References to the actual test cases

The test suite at any point in time may not cover all of the tests required for the specifications.
The test harness will use the linkage between the test suite document, the RDF in the specifications and the
available test cases to determine how much of the specifications are covered by the test suite. This will be available
as a test coverage report.

### Test cases

The test cases will be contained in a repository and may be grouped in various ways to enable logical sets of tests to
be grouped cohesively. 

### Conformance report

Conformance reports will be generated using EARL thereby making them available for consumption by many different tools.
Output will be available in at least Turtle and HTML+RDFa formats.


## Component Architecture

The following is an illustration of the provisional component architecture for the test harness and executor but is 
liable to change as the project progresses.

![Harness component architecture](architecture/harness-components.png)

## Test suite description document

![Test suite description document](architecture/test-suite-description.png)

## Prerequisites

The example test cases have been run against ESS in ACL compatibility mode, CSS and NSS. They require 2 user accounts
to be made available via an IdP: alice and bob. The profiles for these users may need additional information adding to
them:
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

If you are planning to use accounts that do not own PODs on the target server then you will also need to provide a
container on the target server for the tests that has been granted full access control for the test user.

There are 3 approaches to authentication: refresh tokens, session based login, and client credentials. If the target 
test server and the chosen IdP are compatible (which they should be) then any of the mechanisms can be used to get the
access tokens required to run the tests.

### Refresh tokens
This relies on getting a refresh token from an IdP (e.g. https://broker.pod-compat.inrupt.com/) and exchanging that for
an access token in order to run the tests. The refresh token can be created using a simple bootstrap process:
```shell
npx @inrupt/generate-oidc-token
```

The configuration that must be saved for each user is:
* Client Id
* Client Secret
* Refresh Token

Unfortunately, this process requires a user to go the broker's web page, log in and authorize the application. Also, the
refresh tokens expire and would need to be recreated regularly. This is not suitable for a CI environment so
alternatives are being considered such as a Mock IdP.

This mechanism will not work for NSS until support for refresh tokens is added: 
See https://github.com/solid/node-solid-server/issues/1533

### Session based login
Some IdPs make is easy to authenticate without a browser by supporting form based login and sessions. The test harness 
has the capability to use this mechanism to login and get access tokens. The configuration that must be saved for each
user is:
* Username
* Password

The harness also needs to know the login path to use on the IdP and the origin that has been registered as the trusted
app for the users.

This mechanism will work in CI environments where the credentials can be passed in as secrets.

### Client credentials 
**NOTE: Not yet implemented**

This relies on an IdP that supports this grant mechanism and which has had users pre-registered.

The configuration that must be saved for each user is:
* Client Id (a WebID)
* Client Secret

## Usage

### Target server configuration

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
  solid-test:origin <https://tester> ;
  solid-test:maxThreads 8 ;
  solid-test:features "authentication", "acl", "wac-allow" .
```
This describes the server to be tested and may be used to test multiple instances of the same Solid server 
implementation. There is a sample of this file here: `config/config.ttl`. This will be used unless you override this
location as shown further down this page.

This information does not include the configuration details needed to authenticate with server. Since this
will be specific to each server instance, it is loaded from environment variables, or a file in the working directory
called `.env`. It contains the following sections:

#### Server
```
SERVER_ROOT=	# e.g. https://pod-compat.inrupt.com or https://pod-user.inrupt.net
TEST_CONTAINER= # e.g. pod-user/test or test
```
These are used to construct the root location for test files e.g. `https://pod-compat.inrupt.com/pod-user/test/`
or `https://pod-user.inrupt.net/test`

#### Authentication mechanism
```
SOLID_IDENTITY_PROVIDER=	# e.g. https://inrupt.net or https://broker.pod-compat.inrupt.com
LOGIN_ENDPOINT=				# e.g. https://inrupt.net/login/password [this is only needed if using session login]
```

### User credentials
The values and their usage are:
```
ALICE_WEBID=			# required
ALICE_USERNAME=			# session-based login
ALICE_PASSWORD=			# session-based login
ALICE_REFRESH_TOKEN=	# refresh_token login
ALICE_CLIENT_ID=		# refresh_token login
ALICE_CLIENT_SECRET=	# refresh_token or client_credentials login

BOB_WEBID=				# required
BOB_USERNAME=			# session-based login
BOB_PASSWORD=			# session-based login
BOB_REFRESH_TOKEN=		# refresh_token login
BOB_CLIENT_ID=			# refresh_token login
BOB_CLIENT_SECRET=		# refresh_token or client_credentials login
```

### Execution
The test harness is packaged into a single, executable jar which is available as an asset in the release within github:
https://github.com/solid/conformance-test-harness/releases. The only dependency is on Java 11. You can run it and see
its usage as follows:
```shell
java -jar solid-conformance-test-harness-runner.jar --help
```

The command line options are:
```
usage: run
 -c,--config <arg>   URL or path to test subject config (Turtle)
    --coverage       produce a coverage report
 -h,--help           print this message
 -o,--output <arg>   output directory
 -s,--suite <arg>    URL or path to test suite description
 -t,--target <arg>   target server
```
If you want to control the logging output or set up mappings between URIs and local directories for test features you
can also create `config/application.yaml` in your current working directory based on the definition shown above. Note
that the jar file does not have to be in this directory. The command line options override any equivalent options set
in any application properties file. An example of a minimal properties file which could be used to map test features
would be:
```yaml
feature:
  mappings:
    - prefix: https://github.com/solid/conformance-test-harness/example
      path: example
```
The application wrapper is still under development so there will be changes to the above options and properties.  

### Docker
We are in the process of making a docker image available for the test harness. Currently, there are 2 docker files:
* Dockerfile - this is just the harness
* Dockerfile.examples - this includes the example test cases from this project

Until the images are published, they must be built from this project as follows:
```shell
./mvnw -DskipTests package
docker build -f src/main/docker/Dockerfile -t solid-conformance .
docker build -f src/main/docker/Dockerfile.examples -t solid-conformance-examples .
```
There are many ways to configure and run tests with these docker images but there are 2 examples in this project. Both
require the environment variables described above to be placed in a file called `env` in the directory from which you
are going to run the tests.
1. example.sh - This uses the test harness docker image, fetches the example tests from the project repository and sets
   up a mapping file. The results are output to the `conformance-tests` directory into which the tests were downloaded.
2. example2.sh - This uses the testharness with embedded examples docker image. The results are output to the current 
   working directory.

## Test Reports
|Report|Location|
|------|--------|
|Coverage (HTML+RDFa)|`coverage.html`|
|Results (HTML+RDFa)|`report.html`|
|Results (Turtle)|`report.ttl`|
|Summary report|`target/karate-reports/karate-summary.html`|
|Timeline|`target/karate-reports/karate-timeline.html`|
|Unit test coverage|`target/site/jacoco/index.html` (when run form maven)|

## Writing tests

This section will contain guidelines for writing test cases using the KarateDSL alongside the features that the test
harness provides.

### Example test cases
In the future, all test cases will be pulled from an external repository (whether they are ultimately written in
KarateDSL or RDF). There are currently some examples in the `example` folder to show some templates for how tests
can be defined.
* The content negotiation tests create RDF resources of different formats, then confirm that they can be accessed as
  other formats. It uses a Java library to convert Turtle or JSON-LD to triples to allow responses to be compared to the
  original test sample. Support for RDFa is not consistent across all servers so that test is missed for now.
* The protect operations tests create a resource or container and then each test sets up different ACLs for it. The
  tests confirm that the Bob user has the correct access to the resource or container.
* The WAC allow tests create a resource and then each test sets up different ACLs for that resource. The tests parse the
  WAC-Allow header and confirm that the Bob user and un-authenticated users see the correct permissions.

### Test patterns

Each group of tests is created in its own folder with a shared setup feature. There are 2 variants of this demonstrated.

Karate based setup feature:
* fetches any access tokens that are required
* creates Authorization headers with these tokens
* creates any test resources required for the test
* adds ACLs if needed
* provides a link to the container created for the test to be used in teardown

Javascript function setup (see wac-allow tests):
* the setup feature contains a function used to set up the clients and a test resource - this is shared across all
  features in the group
* a separate setup function in each test feature is called once for that feature to set up the ACLs - this puts the 
  specifics of the test feature into the feature file making them easier to read

The test files themselves:
* run a background task for each scenario to call the necessary setup procedure
* hold the returned test context to provide access to the Authorization headers and the test container or resource paths
* prepare the teardown function that will delete the resources created for the tests
* provide a set of scenarios that make http requests against the test resource and validate the responses

### Tips

The full KarateDSL documentation is at: https://intuit.github.io/karate/

#### Request headers

There are 3 commands to add headers to an HTTP request:
* `configure headers` - https://intuit.github.io/karate/#configure-headers
	* Typically used in the `Background` section to set up headers for the whole feature file
	* Takes a JSON object containing key/value pairs to be added to the headers
	* Takes a function which returns a JSON object containing key/value pairs to be added to the headers
	* Often used to set up Authorization headers for set of tests
	* If later steps need to use different headers you must either update `configure headers` or set it to null and use
	  one of the commands below to set a new header
	* Example: `* configure headers = { Authorization: 'some_token', tx_id: '1234' }` 
* `header` - https://intuit.github.io/karate/#header
	* Takes a function or expression that returns a header value
	* Example: `* header Accept = 'application/json'`
* `headers` - https://intuit.github.io/karate/#headers
    * Takes a JSON object containing key/value pairs to be added to the headers
	* Note this is a shortcut command, not an expression as in the previous cases
	* Example: `* headers { Authorization: 'some_token', tx_id: '1234' }`

#### Response status
When you know there is only one valid status there is a shortcut but in other cases you need to allow for multiple
status codes or ranges. The best options are:
* `* status 200`
* `* match [200, 201, 202] contains responseStatus`
* `* match karate.range(200, 299) contains responseStatus` - note, this results in poor error messages as it lists all
  100 mismatched values
* `* assert responseStatus >= 200 && responseStatus < 300`

## Processes
### Checkout
```shell
git clone git@github.com:solid/conformance-test-harness.git
```

### Setting up the environment
There 5 important settings:
* `target` - the IRI of the target server, used to select the server config from the config file
* `configFile` - the location of the config file
* `testSuiteDescription` - the location of the test suite description document that lists the test cases to be run
* `feature:mappings` - maps test cases IRIs to a local file system (there can be multiple mappings). Mappings should be
  ordered so the most specific is first. This allows individual files to be mapped separately from their containing
  directories.

There are 2 ways to set these properties. Firstly you can provide `config/application.yaml` in the working directory

containing:
```yaml
target: TARGET_SERVER_IRI
configFile: PATH_TO_CONFIG
testSuiteDescription: PATH_TO_TESTSUITE_DOC
feature:
  mappings:
    - prefix: https://github.com/solid/conformance-test-harness/example
      path: example
```
This method works well when running your tests in an IDE as it doesn't require anything adding to the command line.

Alternatively you can set these things on the command line:
```
-Dtarget=TARGET_SERVER_IRI
-DconfigFile=PATH_TO_CONFIG
-DtestSuiteDescription=PATH_TO_TESTSUITE_DOC
``` 

### Build and test
To run the unit tests on the harness itself:
```shell
./mvnw test
```
To run the test suite with the default target server as defined in `config/application.yaml`:
```shell
# this uses a profile to run the TestSuiteRunner instead of local unit tests
./mvnw test -Psolid
```
To run the test suite with a specific target server:
```shell
./mvnw test -Psolid -Dtarget=https://github.com/solid/conformance-test-harness/ess-compat
./mvnw test -Psolid -Dtarget=https://github.com/solid/conformance-test-harness/css
./mvnw test -Psolid -Dtarget=https://github.com/solid/conformance-test-harness/nss
```

Using an IDE you can also run a specific scenario by editing the TestScenarioRunner and then running it as you would any
unit test:
```java
String featurePath = "classpath:writing-resource/containment.feature";
Results results = testRunner.runTest(featurePath);
```

You can also go to the TestSuiteRunnner class and run the whole test suite in the same way.

**Note:** You must configure the IDE to include the following command line option to make Quarkus use the production
profile when running tests:
```
-Dquarkus.test.profile=prod
```

### Package
The test harness can be packaged into a single jar:
```shell
./mvnw package
```
To quickly build this package without running the unit tests:
```shell
./mvnw -DskipTests package
```
This creates `target/solid-conformance-test-harness-runner.jar` which can be deployed to its own directory and run as:
```shell
java -jar solid-conformance-test-harness-runner.jar
```

### Release
Update CHANGELOG.md to highlight new features before starting the release.
```shell
./mvnw release:prepare
```
The first time you run this it will ask various questions to help setup `release.properties` which will be used for
future releases. This process automatically modifies `pom.xml` to prepare a release version, commits the change and tags
the repository, then sets up the project ready for the ongoing development of the next version. 

The final stage of deploying the package has not been set up yet but will use:
```shell
./mvnw release:perform
```

You can test this process, and undo the results with:
```shell
./mvnw release:prepare -DdryRun=true
./mvnw release:clean
```

Once the release has been completed you should go to the release tag in github and edit it. You can then upload
`target/solid-conformance-test-harness-runner.jar` as an asset of the release.

