# Solid Specification Test Conformance Suite - Test Harness

**NOTE:** This project is derived from the initial PoC used to review the potential architecture and as
such has some very rough edges. There will be a lot of changes in the near future but comments are always welcome. 

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

The config for the server(s) under test goes in `config.json`. The format of this is:
```json
{
  "target": "default_server",
  "servers": {
    "default_server": {
      "features": {
        "authentication": true,
        "acl": true,
        "wac-allow": true
      },
      "solidIdentityProvider": "",
      "loginPath": "",
      "origin": "",
      "serverRoot": "",
      "rootContainer": "",
      "testContainer": "",
      "setupRootAcl": true,
      "maxThreads": 8,
      "users": {
      }
    }
  }
}
```
The initial `target` value is name of the server to test by default from the list below. The
server sections define each server to be tested including the user accounts, and the features that the server supports.

There is a sample of this file in the `config` folder and this will be used unless you override this location as shown below.

Within `config.json` are the user authentication settings. You can either add the information directly into this:
```json5
"users":{
  "alice":{
    "webID":"https://pod-compat.inrupt.com/solid-test-suite-alice/profile/card#me",
    // EITHER
    "refreshToken": "",
    "clientId": "",
    "clientSecret": ""
    // OR
    "username": "",
    "password": ""
  }
}
```
Or you can keep the credentials in a separate file which allows them to be shared between servers when they are using the same IdP:
```json
"users":{
  "alice":{
    "webID":"https://pod-compat.inrupt.com/solid-test-suite-alice/profile/card#me",
    "credentials":"filename.json"
  }
}
```

## Setting up the environment
There 3 important settings:
* `env` - the name of the target server, used to select the config from the config file
* `config` - the location of the config file
* `credentials` - the location of the shared credentials files if used

There are 2 ways to set these properties. Firstly you can provide `local-config.json` in the working directory containing:
```json
{
  "env": "TARGET_SERVER",
  "config": "PATH_TO_CONFIG",
  "credentials": "PATH_TO_CREDENTIALS"
}
```
This method works well when testing your tests in an IDE as it doesn't require any environment variables to be set.

Alternatively you can set these things on the command line:
```
-Dkarate.env=TARGET_SERVER
-Dconfig=PATH_TO_CONFIG
-Dcredentials=PATH_TO_CREDENTIALS
``` 

You may be relieved to know that this is not how authentication of test accounts will stay - the process will
be made much simpler but this worked to get the test harness started.

## Running the test suite
To run the test suite with the default target server as defined in `config.json`:

```shell
./gradlew testsuite
```
To run the test suite with a specific target server:
```shell
./gradlew testsuite -Dkarate.env=ess-compat
./gradlew testsuite -Dkarate.env=css
./gradlew testsuite -Dkarate.env=nss
```

Using an IDE you can also go directly to specific scenarios and run them on their own, viewing the output in the IDE console.
This is incredibly helpful when developing tests.

You can also go to the TestRunnner class and run the whole test suite in the same way.  

## Test Reports
|Report|Location|
|------|--------|
|Summary report|`build/karate-reports/karate-summary.html`|
|Timeline|`build/karate-reports/karate-timeline.html`|

## Example test cases
In the future, all test cases will be pulled from an external repository (whether they are ultimately written in KarateDSL or RDF).
There are currently some examples in the `src/test/resources/features` folder to show some templates for how tests
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
* There is a Javascript library in `src/test/java/utils.feature` demonstrating how you can also create common functions in Javascript

## Plans

### Short term
* [x] Sort out project structure to retain benefits of IDE whilst keeping tests separate from code.
* [x] Review the Gradle setup and file locations, particularly tests and resources which are a little confusing since everything is test related.
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
* [ ] Read test case descriptions from test suite description document.
* [ ] Report results in EARL.
* [ ] Create the overall conformance report (matrix of all servers tested) 
* ...
