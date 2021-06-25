
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
- [Usage](./USAGE.md)
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

Javascript function setup (see wac-allow tests):
* the setup feature contains a function used to set up the clients and a test resource - this is shared across all
  features in the group
* a separate setup function in each test feature is called once for that feature to set up the ACLs - this puts the 
  specifics of the test feature into the feature file making them easier to read

The test files themselves:
* run a background task for each scenario to call the necessary setup procedure
* hold the returned test context to provide access to the Authorization headers and the test container or resource paths
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
There are 4 important settings:
* `target` - the IRI of the target server, used to select the server config from the config file
* `subjects` - the location of the file describing test subjects
* `sources` - the locations of annotated specification documents that list the test cases to be run
* `mappings` - maps test cases IRIs to a local file system (there can be multiple mappings). Mappings should be
  ordered so the most specific is first. This allows individual files to be mapped separately from their containing
  directories.

There are 2 ways to set these properties. Firstly you can provide `config/application.yaml` in the working directory
containing:
```yaml
target: TARGET_SERVER_IRI
subjects: PATH_TO_SUBJECTS_DOC
sources:
	- PATH_TO_SPECIFATION_DOC
	- PATH_TO_SPECIFATION_DOC
mappings:
- prefix: https://github.com/solid/conformance-test-harness/example
  path: example
```
This method works well when running your tests in an IDE as it doesn't require anything adding to the command line.
Alternatively you can set these as command line options as described later. There is an additional option for use
during development - you can select a target using:
```
-Dtarget=TARGET_SERVER_IRI
``` 

### Build and test
To run the unit tests on the harness itself:
```shell
./mvnw test
```
The test coverage report is available here:`target/site/jacoco/index.html`

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

