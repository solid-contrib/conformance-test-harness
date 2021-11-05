# Solid Specification Conformance Test Harness

## Release 1.0.7
### Features
* Improve report navigation and interpretation with collapsing sections and counts of tests passed.

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
[Specification Tests Repository](https://github.com/solid/specification-tests). The container image is available on
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
