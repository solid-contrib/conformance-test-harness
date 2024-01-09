# Solid Specification Conformance Test Harness

## Release 1.1.16
### Minor changes
* Update dependencies.
* Separate stages of server preparation for better diagnostics of problems.

## Release 1.1.15
### Minor changes
* Update dependencies, in particular Karate, Quarkus and Graal to keep in sync and use latest to remove vulnerabilities.

## Release 1.1.14
### Minor changes
* Add `ALLOW_SELF_SIGNED_CERTS` to enable self-signed certificates on the server under test.

## Release 1.1.13
### Minor changes
* Add vocabs to project and generate code from them within the build.
* Update use of specification vocab to account for `requirementLevel` and `requirementSubject` changes.

## Release 1.1.12
### Minor changes
* Fix JSON logging output for result counts.
* Detect expired access tokens and regenerate when needed.
* Removed support for legacy mode of ACP.
* Allow operator to list scenarios which are known to fail so that they do not contribute to the overall exit code.

## Release 1.1.11
### Minor changes
* Improve logging of initial config and provide better information for errors.
* Add support for JSON logging - enable by setting `quarkus.log.console.json=true`.
* Prevent report output for setup scenarios.
* Upgrade dependencies to use latest Graal & Quarkus and remove vulnerabilities.

## Release 1.1.10
### Minor changes
* Switch to UBI for base image.

## Release 1.1.9
### Minor changes
* Update dependencies and enable OWASP dependency checks.

## Release 1.1.8
### Minor changes
* Update libraries for better Graal support and Karate websocket improvements.

## Release 1.1.7
### Minor changes
* Retry HTTP requests after timeouts to make tests more resilient. 

## Release 1.1.6
### Minor changes
* Update libraries and move repository
 
## Release 1.1.5
### Improvements
* When comments are linked to a test subject or a specification requirement, show them in the report.
* Config changes detailed in USAGE.md:
  * Remove SETUP_ROOT_ACL option.
  * Allow individual IDP for users which will override SOLID_IDENTITY_PROVIDER.
  * Make RESOURCE_SERVER_ROOT optional - if omitted, the first available storage from the WebID profile is used. It is
    maintained for backwards compatibility. If a different test pod is required this can be given as an absolute URL in
    TEST_CONTAINER.
  * TEST_CONTAINER is more flexible:
    * An absolute URL to the storage location.
    * A relative path applied to the storage location (either user's default storage or RESOURCE_SERVER_ROOT). 
    * Can be omitted if the test should use the root container of the user's default storage location.
* Update Karate library to benefit from improved logging of the body in HTTP requests & responses. 

## Release 1.1.4
### Minor changes
* When a background step is reused in multiple scenarios, ensure the log output is available in each scenario. 
* Allow tests to exit with the `earl:cantTell` outcome when a precondition in not met for the test to continue.
* Improve assertions at the scenario level to apply the correct outcomes.
* Improve assertions at the feature level to derive outcomes from the scenarios that make up the feature.
* Break scenario counts into each requirement level and show in results summary table.

## Release 1.1.3
### Fix
* Issues with login authentication mechanism and PKCE support.

## Release 1.1.2

### Minor changes
* Get ACL link on container creation to avoid unnecessary HEAD request.
* Fix issue attempting to get ACL link when resource creation failed.
* Improve JSON log format: structured scores and ISO8601 timestamp

## Release 1.1.1

### Minor changes
* Update Karate, Quarkus and Graal packages.

## Release 1.1.0

### Fix
* Fix reporting error when creating assertions on skipped ScenarioOutline tests.
* In ACP matcher support, replace acp:group with a simple list of agents.
* Add result counts to the report and command line output for each combination of requirement level and outcome.
* Add result counts to the command line output for the combined passes and fails of mandatory requirements.
* Add toggle for client requirements and initially hide them.
* Various report improvements (styling and annotation related).

## Release 1.0.14

### Features
* Use tags on features or scenarios to specify when they depend on optional features. Testers can then choose to skip
  tests with these tags. Skipped features or scenarios are marked with suitable assertions in the results report.
* The docker image has moved to https://hub.docker.com/r/solidproject/conformance-test-harness.

## Release 1.0.13

### Features
* Add `send` methods to the `SolidClient` API to allow testers to send requests with full control over the headers and
  the ability to use methods not defined by the HTTP specification.
* Push comments (for features, scenarios and steps) found in the test files through to the results reports.
* Use tags in feature files to allow tests to be skipped based on the server definition

### Minor changes
* Shorten the generated names of test resources.
* Prevent unnecessary data from annotated specifications being included in the Turtle report. 

## Release 1.0.12

### Features
* Add RDF parsing and querying library to the Karate environment.

### Minor changes
* Remove deprecated functions.

## Release 1.0.11

### Features
* Extract Karate-facing API from internals of CTH.
* Improve exception handling for reporting better errors in tests and simplifying tests.

### Minor changes
* Improve error handling & reporting when running tests locally.

## Release 1.0.10
### Features
* New command line options:
  * `--skip-reports` stops the results reports being generated. The result counts are still logged in JSON format.
  * `--ignore-failures` can be used in a CI workflow to ensure the harness always returns a success exit code even if
    tests fail.
  * `--status` list of review statuses (local names e.g. approved, unreviewed) of tests you want to run.
* Added a section to reports with details about the test suite e.g. release date and version information.

### Minor changes
* Fix: Fetch missing titles from tests that are not run to include in the result report.
* Update: Add assertions for tests that are not run or are inapplicable to the test subject.
* Update: Made all dates in report use `xsd:dateTime` although some only display as a date.

## Release 1.0.9
### Features
* Improve report navigation and interpretation with collapsing sections and counts of tests passed.

### Minor changes
* Fix: Use the config file in the specification-tests repo to ensure consistency.

## Release 1.0.6
### Features
* Add function to allow a test to locate the `pim:Storage` of a resource by traversing up the hierarchy. 

### Minor changes
* Fix: Coverage only report failed with missing subject.
* Fix: Fetch test case titles from Feature files for coverage report.
* Update: Simplify command line options. Tests are run unless you specify `--coverage` in which case you get that report
  instead.

## Release 1.0.5

### Minor changes
* Add support for acp:memberAccessControl.

## Release 1.0.4
### Features
* Move ACP support towards the draft standard (further changes to follow).

### Minor changes
* Use client_secret_basic authentication to work with client credentials from the Inrupt Application Catalog.
* Change authorization form support to work with changes in CSS 2.0.0 (https://github.com/solid/community-server/issues/992).

## Release 1.0.2

### Minor changes
* Add a version label to the image file to make it easier to identify.
* Fix URLs in reports to point to the specs, manifests and test cases in GitHub (including line-specific links).
* Allow either doap or spec namespaces on specification documents.

## Release 1.0.0
This is the first major release of the Conformance Test Harness. Most people will only need to use the containerized
version of this package which comes pre-loaded with a test of tests from the
[Specification Tests Repository](https://github.com/solid-contrib/specification-tests). The container image is available on
[DockerHub](https://hub.docker.com/r/solidconformancetestbeta/conformance-test-harness) along with detailed instructions
on how to use it.

### Features
* Various methods of authenticating with test servers.
* Universal access control layer allowing most authorization tests to be run against servers implementing either WAC
  or ACP.
* Test results are reported in Turtle format and in HTML annotated with RDFa to make them human and machine readable. 
  Each test is linked back to the relevant specification requirement and the results include a log of the
  request/response to help tracing any issues.

## Release 0.0.2

### Features
* Improvements to the release process.
* Command line application ready for use - see USAGE.md for details.

## Release 0.0.1

This is an initial pre-release of the Solid Conformance Test Harness. The purpose of the release was to prove out the 
packaging and release process.
