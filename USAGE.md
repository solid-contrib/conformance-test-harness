# Usage: conformance-test-harness

The [Conformance Test Harness](https://github.com/solid/conformance-test-harness) (CTH) is used to run conformance tests against the [Solid specifications](https://solidproject.org/TR/).

# Prerequisites

The tests require 2 user accounts to be set up, referred to as `alice` and `bob`.
These users can be registered on any compatible Solid Identity Provider (IdP) or on the server under test.

The server under test must also host a Pod with a container for running tests. The `alice` user must 
have full control over that container, either because `alice` owns the Pod or because `alice` has been
granted full control access.   

# Configuration
The CTH relies on up to 4 sources of configuration, depending on how the data is likely to be used
and how dynamic it is. They are as follows (in order of least dynamic to most).

## 1. Test Subject Description
This is a Turtle file which describes the test subject and it's capabilities, primarily using
[EARL](http://www.w3.org/ns/earl#) and [DOAP](http://usefulinc.com/ns/doap#) vocabularies. It takes the following form:
```
<css>
    a earl:Software, earl:TestSubject ;
    doap:name "Community Solid Server" ;
    doap:release <css#test-subject-release> ;
    doap:developer <https://github.com/solid> ;
    doap:homepage <https://github.com/solid/community-server> ;
    doap:description "An open and modular implementation of the Solid specifications."@en ;
    doap:programming-language "TypeScript" ;
    solid-test:skip "acp" ;
    rdfs:comment "Comment on the test subject"@en.
    
<css#test-subject-release>
    doap:revision "0.9.0" ;
    doap:created "2021-05-04T00:00:00.000Z"^^xsd:dateTime .
```
**Note**: The test subject identifier is simply an IRI. It is not meant to be resolvable and can be anything as long as
you specify it on the command line when running tests.

There are some test subject specific configuration properties in this file:
```
  solid-test:skip "acp"  # skip tests with these tags 
  solid-test:features "acp-legacy"  # enable the legacy mode for ACP to conform to an early version of the specification
```
An example of this file is provided in the test repository (https://github.com/solid/specification-tests),
containing descriptions of the following Solid implementations:
* `<css>` - [Community Solid Server](https://github.com/solid/community-server) (CSS).
* `<ess>` - [Enterprise Solid Server](https://inrupt.com/products/enterprise-solid-server) (ESS) in Access Control Policies (ACP) compatibility mode.
* `<ess-wac>` - [Enterprise Solid Server](https://inrupt.com/products/enterprise-solid-server) (ESS) in  Web Access Controls (WAC) compatibility mode.
* `<nss>` - [Node Solid Server](https://github.com/solid/node-solid-server) (NSS).
* `<trinpod>` - [TrinPod](https://trinpod.us).

Other implementations will follow as servers are publicly deployed.

You can either use this file and choose which server you are targeting when you run the CTH, or
provide your own version of the file.

## 2. CTH Configuration
This file can be used in place of command line settings if desired but is only required if you want to override default
settings or map URLs or source files to a local file system. It can also control the level of logging but this is better
controlled via environment variables.

The file can have various formats though the example provided is YAML. It must be in a specific location,
`config/application.yaml`, in your current working directory. The default version of this file, used in the docker image
of the CTH, is maintained at https://github.com/solid/specification-tests/blob/main/application.yaml.
```yaml
# The first 3 can be ignored if using the command line settings: subjects, source and target 
subjects: test-subjects.ttl
sources:
  # Protocol spec & manifest
  # Editor's draft (fully annotated)
  - https://solidproject.org/ED/protocol
  - https://github.com/solid/specification-tests/blob/main/protocol/solid-protocol-test-manifest.ttl
  # Additional comments on requirements (linked to requirement IRI and using rdfs:comment predicate)
  - https://github.com/solid/specification-tests/blob/main/protocol/requirement-comments.ttl

  # WAC spec & manifest
  # Editor's draft (fully annotated)
  - https://solid.github.io/web-access-control-spec/
  - https://github.com/solid/specification-tests/blob/main/web-access-control/web-access-control-test-manifest.ttl

  # Published draft (not annotated)
  # This is an example of how you could run tests for a specific version of the specification 
  #  - https://solidproject.org/TR/2021/wac-20210711
  #  - https://github.com/solid/specification-tests/web-access-control/web-access-control-test-manifest-20210711.ttl

# The target is just an IRI or local name relative to the test-subjects file and is not expected to resolve to anything
target: https://github.com/solid/conformance-test-harness/ess

# To map URLs from the manifest to local files:
mappings:
  - prefix: https://github.com/solid/specification-tests/blob/main
    path: ./data

# Other configuration to override defaults
agent: agent-string		# default = Solid-Conformance-Test-Suite
connectTimeout: 1000	# default = 5000
readTimeout: 1000		# default = 5000
maxThreads: 4           # default = 8, number of threads for running tests in parallel  
origin: https://test    # default = https://tester, origin used for OIDC registration
```

## 3. Environment Variables

The CTH attempts to use a discovery process to determine the container in which to run tests.
However, this depends on at least knowing the WebId of a user who can have full access to the test container.
It is also possible to provide the location directly, overriding whatever is found in a WebId profile. This
configuration (and more) is provided using environment variables stored in a `.env` file.

The definition of this configuration is split into:
* Core configuration
* Authentication details
* Logging
* Other

### Core configuration
The tests require two WebIds for users known as alice and bob. Alice has full access to the test container and can grant
Bob certain access as required for tests.
```shell
USERS_ALICE_WEBID=
USERS_BOB_WEBID=
```
A default identity provider can be specified in the config if it is shared between all users, but it is not required if
the IDP value is set for each user:
```shell
SOLID_IDENTITY_PROVIDER=	# e.g., https://broker.pod.inrupt.com
```
The CTH will attempt to use a pod found in the WebId profile via the `pim:storage` predicate. You can use the following
config if you want to use a particular container in that location by providing a relative path. Alternatively, if there 
is no storage location available, or you want to use a test container in a different location (to which Alice must be
granted full control), then you can provide an absolute URL:
```shell
TEST_CONTAINER=         # e.g., test/ or https://pod/test/
```
For backwards compatibility you can provide the resource server root and the test container path separately:
```shell
RESOURCE_SERVER_ROOT=	# e.g., https://pod.inrupt.com or https://pod-user.inrupt.net
TEST_CONTAINER=         # e.g., pod-user/test or test
```

### Authentication
There are 4 options for obtaining authentication access tokens when running tests:
1. Client credentials.
2. Refresh tokens.
3. Session-based login.
4. Register users locally (and login during the authorization code request).

#### 1. Client Credentials
The simplest authentication mechanism is based on the Solid Identity Provider offering the
client credentials authorization flow and requires the users pre-register for these credentials.

This mechanism works well in CI environments where the credentials can be passed in as secrets.

For each user, the following configuration information is required:
* Client Id.
* Client Secret.
* OIDC Issuer (if not SOLID_IDENTITY_PROVIDER).

The required environment variables are:
```shell
# Authentication Configuration - Client Credentials
USERS_ALICE_CLIENTID=
USERS_ALICE_CLIENTSECRET=
USERS_ALICE_IDP=
USERS_BOB_CLIENTID=
USERS_BOB_CLIENTSECRET=
USERS_BOB_IDP=
```

**Note**: The access tokens provided cause a 500 error on NSS so session based login is the only
option for that server.

#### 2. Refresh Tokens
This relies on getting a refresh token from an IdP (e.g., https://broker.pod.inrupt.com/) and
exchanging that for an access token in order to run the tests. 

This process requires a user to go the broker's web page, log in, and authorize the application. Also, the
refresh tokens expire and would need to be recreated regularly which makes it unsuitable for a CI environment.

The refresh token can be created using a
simple bootstrap process:
```shell
npx @inrupt/generate-oidc-token
```

For each user, the following configuration information is required:
* Client Id.
* Client Secret.
* Refresh Token.
* OIDC Issuer (if not SOLID_IDENTITY_PROVIDER).

The required environment variables are:
```shell
# Authentication Configuration - Refresh Tokens
USERS_ALICE_REFRESHTOKEN=
USERS_ALICE_CLIENTID=
USERS_ALICE_CLIENTSECRET=
USERS_ALICE_IDP=
USERS_BOB_REFRESHTOKEN=
USERS_BOB_CLIENTID=
USERS_BOB_CLIENTSECRET=
USERS_BOB_IDP=
```

**Note**: This mechanism will not work for NSS until support for refresh tokens is added:
See https://github.com/solid/node-solid-server/issues/1533.

#### 3. Session-based Login 
Some IdPs make it easy to authenticate without a browser by supporting form based login and sessions. The CTH
has the capability to use this mechanism to login and get access tokens but the users must have some additional data 
added to their profiles.  

**Notes**: These updates assume the Profile document contains prefixes:
```
@prefix solid: <http://www.w3.org/ns/solid/terms#>.
@prefix acl: <http://www.w3.org/ns/auth/acl#>.
```
* Set the Origin as a Trusted App:
    ```
    :me acl:trustedApp [
        acl:mode acl:Read, acl:Write;
        acl:origin <https://tester>  # or whatever origin is defined in the test harness configuration
    ];
    ```
* Solid Identity Provider: (where the URL is the IdP of this account):
    ```
    :me solid:oidcIssuer <https://inrupt.net/>;
    ```
    **Note**: By default, NSS does not add this to new profiles.

For each user, the following configuration information is required:
* Username.
* Password.
* OIDC Issuer (if not SOLID_IDENTITY_PROVIDER).

A URL for the login form is also required. 

The required environment variables are:
```shell
# Authentication Configuration - Session-based Login
LOGIN_ENDPOINT=		        # e.g., https://inrupt.net/login/password
USERS_ALICE_USERNAME=
USERS_ALICE_PASSWORD=
USERS_ALICE_IDP=
USERS_BOB_USERNAME=
USERS_BOB_PASSWORD=
USERS_BOB_IDP=
ORIGIN=                     # optional as it defaults to https://tester
```

This mechanism will work in CI environments where the credentials can be passed in as secrets.

#### 4. Register Users Locally
If an implementation supports user registration, including creating a Pod, then the harness can use this to create the
users before running the tests. This can be very useful in a CI environment where the server is created and destroyed
during the process.

**Note**: There is no standard for this process, so the current implementation only works with CSS. Other
implementations will be added as required.

For each user, the following configuration information is required:
* Username.
* Password.
* OIDC Issuer (if not SOLID_IDENTITY_PROVIDER).

A URL for the user registration form is also required.

The required environment variables are:
```shell
# Authentication Configuration - Register Users Locally
USER_REGISTRATION_ENDPOINT=	# e.g., https://localhost:3000/idp/register
USERS_ALICE_USERNAME=
USERS_ALICE_PASSWORD=
USERS_ALICE_IDP=
USERS_BOB_USERNAME=
USERS_BOB_PASSWORD=
USERS_BOB_IDP=
ORIGIN=                     # optional as it defaults to https://tester
 ```

This mechanism will work in CI environments where the credentials can be passed in as secrets.

### Logging
By default, the CTH only provides minimal logging. If you want to see the HTTP request/response exchanges in
the logs, you can set `DEBUG` level for the categories shown below:
* `com.intuit.karate` - HTTP interactions within test cases. **Note**: If this is not set to `DEBUG`, the log entries are
  also excluded from the reports.
* `org.solid.testharness.http.Client` - HTTP interactions during container and resource set up.
* `org.solid.testharness.http.AuthManager` - HTTP interactions during the authentication flow before testing starts.

In the environment file, this looks like this:
```
# Logging Levels
quarkus.log.category."com.intuit.karate".level=DEBUG
quarkus.log.category."org.solid.testharness.http.Client".level=DEBUG
quarkus.log.category."org.solid.testharness.http.AuthManager".level=DEBUG
```
**Note**: Tokens in responses or authorization headers as masked as a security measure.

There is a special logging category, called `ResultLogger`, which outputs a summary of the results in JSON format at
`INFO` level. This is described in the [Reports](#reports) section of this document.

### Other configuration
#### Parallel testing
By default, the CTH will run tests in parallel, defaulting to 8 threads. You can either override this in the
YAML config file as mentioned previously, or you can do it with environment variables. For example:
```
# Parallel Testing
MAXTHREADS=2
```

## 4. Command Line Options

The command line options are:
```
usage: run
    --coverage        produce a coverage report only
 -f,--filter <arg>    feature filter(s)
    --ignore-failures return success even if there are failures 
 -h,--help            print this message
 -o,--output <arg>    output directory
    --skip-reports    skip report generation
    --skip-teardown   skip teardown (when server itself is being stopped)
 -s,--source <arg>    URL or path to test source(s)
    --status <arg>    status(es) of tests to run
    --subjects <arg>  URL or path to test subject config (Turtle)
 -t,--target <arg>    target server
```
If `--coverage` is not specified then the default action is to run the tests and produce the results reports.

# Execution
The simplest way to run the CTH is via the [Docker](https://www.docker.com/) image published to
https://hub.docker.com/r/solidproject/conformance-test-harness.

For ease of use, the Docker image includes the latest release of the tests, manifest files, and test subject
configuration files from https://github.com/solid/specification-tests.

The Docker image works with the following internal structure:
* `/app/harness` - contains the executable jar file.
* `/app/config` - contains the default application.yaml file from https://github.com/solid/specification-tests.
* `/data` - contains the contents of the https://github.com/solid/specification-tests test repository.
* `/reports` - the directory into which reports are written.

To use this image, you just need to provide:

1. An environment file for the server you are testing; and,
2. The test subject using the `target` option on the command line.

When you run the server in a container, there are some important things to remember:
* You cannot use localhost, so you need to name your instance and use that name to access it from the test container.
  The CTH is set up to allow the name `server` so the URL will be https://server.
* When using a domain name, you may hit the problem that DPoP verification requires https unless you are using
  localhost hence using `https` in the above URL.
* As the image uses `https`, you need to provide a SSL certificate to the server which can be generated
  as a self-signed certificate (as seen in the CSS example mentioned below).
* The CTH is set up to detect when you are using https://server, and will allow self-signed certificates. If you see an
  issue with DPoP verification rejecting self-signed certificates, the server being tested will need to be set up to
  have the same capability. For a Node based server, this is done by adding `NODE_TLS_REJECT_UNAUTHORIZED=0`
  to the environment before running the server.

Some additional notes on using the Docker image:
* To run a specific version of the Docker image, you need to append a tag to the image name, e.g.
  `solidproject/conformance-test-harness:1.1.0` 
* If you do not specify a version then Docker will use the latest image
* If you want to see the reports, the `/reports` directory must be mapped to a local volume. Alternatively you could use 
this image in your own image which could send the reports to an external location such as Amazon Web Services (AWS) S3.
* If you use the `--skip-reports` option you do not need to map the `/reports` directory.
* If you want to run different tests or supply a different config file, you can mount local directories in place of other
internal ones. For example:
    ```
    docker run -i --rm \
    -v "$(pwd)"/tests:/data \
    -v "$(pwd)"/config:/app/config \
    -v "$(pwd)"/reports/local:/reports \
    --env-file=ess.env solidproject/conformance-test-harness \ 
    --output=/reports \
    --target=...
    ```
* If you want to run a specific release of the tests then you will need to clone the repository, checkout the specific
  version and then treat them as local tests, as shown above.
    ```shell
    git clone https://github.com/solid/specification-tests /data
    cd /data
    git checkout v1.0.0
    ```  

## Minimal example
If you want to set up your own script, for example, in a CI workflow you could create a `.env` file containing a logging
instruction and server credentials, such as this:
```shell
quarkus.log.category."com.intuit.karate".level=DEBUG

SOLID_IDENTITY_PROVIDER=https://server/idp/
USERS_ALICE_WEBID=https://server/alice/profile/card#me
USERS_ALICE_USERNAME=alice@example.org
USERS_ALICE_PASSWORD=?
USERS_BOB_WEBID=https://server/bob/profile/card#me
USERS_BOB_USERNAME=bob@example.org
USERS_BOB_PASSWORD=?
USER_REGISTRATION_ENDPOINT=https://server/idp/register/

RESOURCE_SERVER_ROOT=https://server
TEST_CONTAINER=/alice/
```

Then you would need a script such as:
```shell
    #!/bin/bash

    # This uses the test harness docker image with the default tests pulled from a repository.
    # Environment variables are defined in an `env` file in the directory from which you run this script.

    mkdir reports/ess
    docker pull solidproject/conformance-test-harness
    docker run -i --rm \
    -v "$(pwd)"/reports/ess:/reports \
    --env-file=ess.env solidproject/conformance-test-harness \
    --output=/reports --target=https://github.com/solid/conformance-test-harness/ess
```

## Using the provided script 
There is a script to help you run tests here: https://github.com/solid/specification-tests/blob/main/run.sh. This can be
used to run combinations of the following:
* Run the tests against a publicly available Solid server or one in a local container.
* Run the tests embedded in the image or your own version of the tests (e.g. if you have cloned the specification-tests
  repository to write tests).
* Use the image's CTH build or a local one if you are developing CTH.

Simply download a copy of the script into a directory and create a suitable `server.env` file as aboce, replacing
`server` with the name of the server you are testing, matching the localname of the server from the `test-subjects.ttl`
file.

1. To run the tests embedded in the image against a publicly available Solid server or CSS in a local container:
  ```shell
  ./run.sh server  # where server is replace with the target name matching your `env` file and `test-subjects.ttl`
  ```
2. Run your own version of the tests (e.g. if you have cloned the specification-tests repository to write tests).
  ```shell
  ./run.sh -d . server  # -d option allows you to specify the location of tests (current directory in this case)
  ```
3. Run the tests with a local build of the CTH.
  ```shell
  ./run.sh -l server  # use a local build of CTH assuming the image name is `testharness`
  ```

You can add additional options for CTH after the commands above, for example, use `--filter` to select specific tests.

# Reports

## Log output
Note this is not necessarily in the order below and only non-zero values will be included.
```json5
{
  "mustFeatures": { // combination of MUST and MUST-NOT
    "passed": 0,
    "failed": 0,
    "cantTell": 0,
    "untested": 0,
    "inapplicable": 0
  },
  "mustScenarios": { // combination of MUST and MUST-NOT
    /* as above */
  }, 
  "features": {
    "MUST": {
      "passed": 0,
      "failed": 0,
      "cantTell": 0,
      "untested": 0,
      "inapplicable": 0
    },
    "MUST-NOT": { /* as above */ },
    "SHOULD": { /* as above */ },
    "SHOULD-NOT": { /* as above */ },
    "MAY": { /* as above */ },
  },
  "scenarios": {
    /* same structure as "features" */
  },
  "elapsedTime":1000.0,
  "totalTime":1000.0,
  "resultDate":"2021-06-17T09:12:31.000Z",
}
```
This results in a log entry such as:
```
2021-06-17 11:43:04,742 INFO  [ResultLogger] (main) {"resultDate":"2021-06-17T11:12:31.000Z","elapsedTime":7552.0,"mustFeatures": {"passed":0,"failed":0}, ...}
```

The `mustFeatures` group are important since they represent the results of the tests related to mandatory requirements
and give an indication of the server's overall conformance.

### Interpreting Result Counts
The Scenario counts represent the results of all individual tests that were run. The possible outcomes for scenarios are:
* `passed` - the scenario test passed
* `failed` - the scenario test failed
* `cantTell` - the scenario was aborted without a pass or fail being a clear outcome, normally because a condition was
  not met making the rest of the test redundant
* `untested` - the scenario has the `@ignore' tag applied, so it is never run
* `inapplicable` - the scenario has a skip tag applied, so it will not run if it depends on a feature not provided by
  the server, or the CTH does not have the capability to test this feature

The other counts are for features where a feature may contain more than one test scenario. The outcomes for features are
determined by the following algorithm:
* If it is filtered out of the test run, it is marked as `untested`
* Else if it is tagged with `@ignore`, it is marked as `untested`
* Else if it has a tag which causes it to be skipped, it is marked as `inapplicable`
* Else if any scenarios failed, it is marked as `failed`
* Else if any scenarios passed, it is marked as `passed` (some scenarios may be `inapplicable`, `cantTell` or `untested`)
* Else of the remaining `inapplicable`, `cantTell` or `untested` pick the one with the highest count, in that order of
  preference (or `untested` if there were all counts are zero)
 
## Output report documents
|Report|Location|
|------|--------|
|Coverage (HTML+RDFa)|`coverage.html`|
|Results (HTML+RDFa)|`report.html`|
|Results (Turtle)|`report.ttl`|

The format of the coverage and results reports are very similar. An example of the coverage report is here:
https://solid.github.io/specification-tests/coverage. The structure of the two reports is shown below. 

**Coverage report**
* **Specifications under test** - list of specification references.
* **Test suite** - description of the specification tests software.
* **Test coverage by specification requirement** - for each specification, a list of the requirements with a count of
  the implemented test cases. 


**Results report**
* **Specifications under test** - list of specification references.
* **Assertor** - description of the test harness software.
* **Test suite** - description of the specification tests software.
* **Test subject** - description of the software of the server under test.
* **Results summary** - information about when the tests were run and the overall results.
* **Outcomes Key** - a table of the symbols used to represent test outcomes.
* **Results by specification requirement** - for each specification, a list of the requirements with a count of how many
  tests passed versus how many are implemented in each case. The results  are shown as a table which also details which 
  scenarios passed or failed within the test case.
* **Results by test case** - for each implemented test case, a list of the scenarios tested for that test case and the
  detailed log of the steps and results.

Both reports have buttons allowing all sections to be expanded/collapsed, or for the results report, just the failing
sections to be expanded.
