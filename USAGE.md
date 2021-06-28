# Usage: conformance-test-harness

The test harness used to run conformance tests against the Solid specifications.

# Prerequisites

The tests require 2 user accounts to be set up, referred to as `alice` and `bob`.
These users can be registered on any compatible Solid Identity Provider or on the server under test.

The server under test must also host a POD with a container for running tests. The `alice` user must 
have full control over the container, either because `alice` owns the POD or because `alice` has been
granted full control access.   

# Configuration
The test harness relies on up to 4 sources of configuration, depending on how the data is likely to be used
and how dynamic it is. They are as follows (in order of least dynamic to most).

## Test subject description
This is a Turtle file which describes the test subject and it's capabilities, primarily using
[EARL](http://www.w3.org/ns/earl#) and [DOAP](http://usefulinc.com/ns/doap#) vocabularies. It takes the following form:
```
<https://github.com/solid/conformance-test-harness/css>
    a earl:Software, earl:TestSubject ;
    doap:name "Community Solid Server" ;
    doap:release [
                     doap:name "CSS 0.9.0" ;
                     doap:revision "0.9.0" ;
                     doap:created "2021-05-04"^^xsd:date
                 ] ;
    doap:developer <https://github.com/solid> ;
    doap:homepage <https://github.com/solid/community-server> ;
    doap:description "An open and modular implementation of the Solid specifications."@en ;
    doap:programming-language "TypeScript" ;
    solid-test:origin <https://tester> ;
    solid-test:maxThreads 8 ;
    solid-test:features "authentication", "acl", "wac-allow" ;
    solid-test:setupRootAcl true .
```
**NOTE**: The test subject identifier is simply an IRI - it is not meant to be resolvable and can be anything as long as you
specify it on the command line when running tests.

There are some test subject specific configuration properties in this file:
```
  solid-test:origin <https://tester>   # registered origin for session-based login
  solid-test:maxThreads 8              # number of threads for running tests in parallel  
  solid-test:features "authentication", "acl", "wac-allow"  # server capabilities
  solid-test:setupRootAcl true         # this tells the server to setup an ACL on the root (used by CSS)
```
An example of this file is provided in the test repository (https://github.com/solid/specification-tests),
containing descriptions of the following Solid implementations:
* CSS - [Community Solid Server](https://github.com/solid/community-server)
* ESS (2 versions) - [Enterprise Solid Server](https://inrupt.com/products/enterprise-solid-server)
  * Access Control Policies (ACP)
  * Compatibility mode for Web Access Controls (ACL)
* NSS - [Node Solid Server] (https://github.com/solid/node-solid-server)
* Other implementations will follow

You could either use this file and choose which server you are targeting when you run the test harness or you could 
provide your own version of the file.

## Test harness configuration
This file can have various formats though the example provided is YAML. It can be used in place of 
command line settings if desired but is only required if you want to override default settings or
map URLs or source files to a local file system. It can also control the level of logging but this
is better controlled via environment variables.

The file must be in a specific location: `config/application.yaml` in your current working directory.
```yaml
# The first 3 can be ignored if using the command line settings: subjects, source and target 
subjects: test-subjects.ttl
sources:
  - example/protocol/solid-protocol-test-manifest.ttl
  - example/web-access-control/web-access-control-test-manifest.ttl
  - example/protocol/solid-protocol-spec.ttl
  - example/web-access-control/web-access-control-spec.ttl
target: https://github.com/solid/conformance-test-harness/ess-compat

# To map URLs from the manifest to local files:
mappings:
  - prefix: https://github.com/solid/conformance-test-harness/example
    path: ./example

# Other configuration to override defaults
agent: agent-string		# default = Solid-Conformance-Test-Suite
connectTimeout: 1000	# default = 5000
readTimeout: 1000		# default = 5000
```

## Environment variables

### Logging
By default, the test harness only provides minimal logging. If you want to see the HTTP request/response exchanges in
the logs you can set `DEBUG` level for the categories shown below:
* `com.intuit.karate` - HTTP interactions within test cases. **Note**: If this is not set to `DEBUG` the log entries are
  also excluded from the reports.
* `org.solid.testharness.http.Client` - HTTP interactions during container and resource set up.
* `org.solid.testharness.http.AuthManager` - HTTP interactions during the authentication flow before testing starts.

In the environment file this looks like this:
```
quarkus.log.category."com.intuit.karate".level=DEBUG
quarkus.log.category."org.solid.testharness.http.Client".level=DEBUG
quarkus.log.category."org.solid.testharness.http.AuthManager".level=DEBUG
```
**Note**: Tokens in responses or authorization headers as masked as a security measure.

There is a special logging category called `ResultLogger` which outputs a summary of the results in JSON format at 
`INFO` level (not necessarily in the older below):
```json
{
  "scenariosPassed":0,
  "scenariosFailed":0,
  "featuresPassed":0,
  "featuresSkipped":0,
  "featuresFailed":0,
  "elapsedTime":1000.0,
  "totalTime":1000.0,
  "resultDate":"2021-06-17 09:12:31 am"
}
```
This results in a log entry such as:
```
2021-06-17 11:43:04,742 INFO  [ResultLogger] (main) {"resultDate":"2021-06-17 11:43:04 am","featuresFailed":0,"elapsedTime":7552.0,"scenariosPassed":4,"featuresSkipped":0,"totalTime":14401.0,"scenariosFailed":0,"featuresPassed":2}
```

### Server
The test harness needs to know the root URL of the server being tested and the path to the container in which test files
will be created.
```
RESOURCE_SERVER_ROOT=	# e.g. https://pod-compat.inrupt.com or https://pod-user.inrupt.net
TEST_CONTAINER= # e.g. pod-user/test or test
```
These are used to construct the root location for any files created and destroyed by the tests
e.g. `https://pod-compat.inrupt.com/pod-user/test/` or `https://pod-user.inrupt.net/test`.

There are 2 reasons that the test container is defined like this:
1. Different implementations construct the pod location from the WebID in different ways, as in the 2 examples above.
2. If tests were run in the pod owned by the `alice` user, the test container could be created by the test harness but
   this is not possible if you are using a WebID from a different server's IdP. In this case you need to grant the test 
   user full access to a container in which files can be created. 

### Authentication
The following are mandatory settings for all authentication mechanisms:
```shell
SOLID_IDENTITY_PROVIDER=	# e.g. https://inrupt.net or https://broker.pod-compat.inrupt.com
USERS_ALICE_WEBID=
USERS_BOB_WEBID=
```

There are 3 options for obtaining access tokens when running tests:
* Client credentials
* Refresh tokens
* Session-based login

#### Client credentials
The simplest authentication mechanism is based on the Solid Identity Provider offering the
client_credentials authorization flow and requires the users to be pre-registered.

The configuration that must be saved for each user is:
* WebID (used as the Client Id)
* Client Secret

The additional environment variables required are:
```shell
USERS_ALICE_CLIENTSECRET=
USERS_BOB_CLIENTSECRET=
```

This mechanism works well in CI environments where the credentials can be passed in as secrets.
However, the access tokens provided seem to cause a 500 error on NSS so session based login is the only
option for that server.

#### Refresh tokens
This relies on getting a refresh token from an IdP (e.g. https://broker.pod-compat.inrupt.com/) and
exchanging that for an access token in order to run the tests. The refresh token can be created using a
simple bootstrap process:
```shell
npx @inrupt/generate-oidc-token
```

The configuration that must be saved for each user is:
* Client Id
* Client Secret
* Refresh Token

The additional environment variables required are:
```shell
USERS_ALICE_REFRESHTOKEN=
USERS_ALICE_CLIENTID=
USERS_ALICE_CLIENTSECRET=
USERS_BOB_REFRESHTOKEN=
USERS_BOB_CLIENTID=
USERS_BOB_CLIENTSECRET=
```

Unfortunately, this process requires a user to go the broker's web page, log in and authorize the application. Also, the
refresh tokens expire and would need to be recreated regularly which makes it unsuitable for a CI environment.

This mechanism will not work for NSS until support for refresh tokens is added:
See https://github.com/solid/node-solid-server/issues/1533

#### Session based login 
Some IdPs make is easy to authenticate without a browser by supporting form based login and sessions. The test harness
has the capability to use this mechanism to login and get access tokens but the users must have some additional data 
added to their profiles.  

* Trusted app:
```
:me acl:trustedApp [
  acl:mode acl:Read, acl:Write;
  acl:origin <https://tester>  # or whatever origin is defined in the test subject config file
];
```
* Solid Identity Provider: (where the URL is the IdP of this account) - NSS does not add this to new profiles by default
```
:me solid:oidcIssuer <https://inrupt.net/>;
```

The configuration that must be saved for each user is:
* Username
* Password

A URL for the login form is also required. 

The additional environment variables required are:
```shell
LOGIN_ENDPOINT=		# e.g. https://inrupt.net/login/password
USERS_ALICE_USERNAME=
USERS_ALICE_PASSWORD=
USERS_BOB_USERNAME=
USERS_BOB_PASSWORD=
```

The harness also needs to know the origin that has been registered as the trusted app for the users. This is included
in the test subject configuration file describing the server under test.

This mechanism will work in CI environments where the credentials can be passed in as secrets.

## Command line options

The command line options are:
```
usage: run
    --coverage       produce a coverage report
 -f,--filter <arg>   feature filter(s)
 -h,--help           print this message
 -o,--output <arg>   output directory
 -s,--source <arg>   URL or path to test source(s)
    --subjects <arg> URL or path to test subject config (Turtle)
 -t,--target <arg>   target server
    --tests          produce test and coverage reports
```
If neither `--coverage` nor `--tests` is specified then the default action is to run the tests and produce both reports.

# Execution

## Running in a container
The simplest way to run the test harness is via the docker image published to
https://hub.docker.com/repository/docker/solidconformancetestbeta/conformance-test-harness

For ease of use the image includes the tests, manifest files and test subject configuration files from 
https://github.com/solid/specification-tests. To use this image you just need to provide an environment file for the 
server you are testing and select the test subject using the `target` option on the commandline.

The image works with the following internal structure:
* /app/harness - contains the executable jar file
* /app/config - contains the default application.yaml file
* /data - contains the contents of the https://github.com/solid/specification-tests test repository
* /reports - the directory into which reports are written

If you want to see the reports the `/reports` directory must be mapped to a local volume. Alternatively you could use 
this image in your own image which could send the reports to an external location such as AWS S3.

If you want to run different tests or supply a different config file you can mount local directories in place of other
internal ones. e.g.
```
docker run -i --rm \
  -v "$(pwd)"/tests:/data \
  -v "$(pwd)"/config:/app/config \
  -v "$(pwd)"/reports/local:/reports \
  --env-file=ess.env solidconformancetestbeta/conformance-test-harness \ 
  --output=/reports \
  --target=...
```

The following examples demonstrate how a script can work with this image and run tests against a publicly available
server or one that is also running in a container.

### ESS (ACL compatibility mode)
Create a file for environment variables e.g. `ess-compat.env` with the following contents (based on the 
client_credentials option):
```shell
SOLID_IDENTITY_PROVIDER=
USERS_ALICE_WEBID=
USERS_ALICE_CLIENTSECRET=
USERS_BOB_WEBID=
USERS_BOB_CLIENTSECRET=
RESOURCE_SERVER_ROOT=https://pod-compat.inrupt.com
TEST_CONTAINER=/pod-owner/shared-test/
```
Next create a script based on the following:
```shell
#!/bin/bash

# This uses the test harness docker image with the default tests pulled from a repository.
# Environment variables are defined in an `env` file in the directory from which you run this script.

mkdir reports/ess-compat
docker pull solidconformancetestbeta/conformance-test-harness
docker run -i --rm \
  -v "$(pwd)"/reports/ess-compat:/reports \
  --env-file=ess-compat.env solidconformancetestbeta/conformance-test-harness \
  --output=/reports --target=ess-compat
```

Run `./ess-compat.sh` and the reports will be created in the specified directory.

To run against the ACP version ESS you would just need to supply a different environment file and set the target as
`ess`.

### CSS in a container
Create a file for environment variables e.g. `css.env` with the following contents (based on the client_credentials
option):
```shell
SOLID_IDENTITY_PROVIDER=
USERS_ALICE_WEBID=
USERS_ALICE_CLIENTSECRET=
USERS_BOB_WEBID=
USERS_BOB_CLIENTSECRET=
RESOURCE_SERVER_ROOT=http://server:3000
TEST_CONTAINER=/test/
```
Note that when using a container you can't use http://localhost:3000 as this will not be accessible to the test harness
container.

Next create `Dockerfile.css` to build and run CSS at the base URL defined above:
```dockerfile
FROM node:latest
RUN git clone https://github.com/solid/community-server
WORKDIR community-server
RUN git checkout main
RUN npm ci
EXPOSE 3000
CMD npm start -- --baseUrl=http://server:3000/
```

Finally, create a script based on the following:
```shell
#!/bin/bash

# This uses the test harness docker image with the default tests pulled from a repository.
# Environment variables are defined in the file `env` in the directory from which you run this script.

mkdir -p reports/css

# build and run CSS in a container
docker build -f Dockerfile.css -t css:latest .
docker run -d --name=server --network=testnet -p 3000:3000 -it css:latest

# run the tests in the test harness
docker pull solidconformancetestbeta/conformance-test-harness
docker run -i --rm \
  -v "$(pwd)"/reports/css:/reports \
  --env-file=css.env --network=testnet solidconformancetestbeta/conformance-test-harness \
  --output=/reports --target=css
docker stop server
docker rm server
```

Run `./css.sh` and the reports will be created in the specified directory.

### Providing your own tests and configuration
To use the image to run a set of local tests:
* Create a directory such as `tests`
* This should contain the tests, manifest files and a test subject configuration file e.g.:
  * protocol/test.feature
  * solid-protocol-test-manifest.ttl
  * test-subjects.ttl
* Create a directory called `config`
* Create the file `config/application.yaml` e.g.
  ```
    subjects: /data/test-subjects.ttl
    sources:
      - /data/solid-protocol-test-manifest.ttl
      - /data/solid-protocol-spec.ttl
    target: https://github.com/solid/conformance-test-harness/ess
    mappings:
      - prefix: https://raw.githubusercontent.com/solid/conformance-test-harness/example/main/protocol
        path: /data
  ```
  * Note that the paths are internal paths in the test harness image so you next need to map them in the command line
  * You can set the default target here rather than on the command line - this has to be a URI from the test-subjects.ttl
    file
  * The mapping prefix is whatever you have used within the test manifest file
* The command line needs to map the local directories into the image to replace the internal directories:
  ```
  docker run -i --rm \
    -v "$(pwd)"/tests:/data 
    -v "$(pwd)"/config:/app/config \
    -v "$(pwd)"/reports/local:/reports \
    --env-file=ess.env solidconformancetestbeta/conformance-test-harness \
    --output=/reports --targ
  ```
  
## Local execution
The test harness is packaged into a single, executable jar which is available as an asset in the release within github:
https://github.com/solid/conformance-test-harness/releases. The only dependency is on Java 11. You can run it and see
its usage as follows:
```shell
java -jar solid-conformance-test-harness-runner.jar --help
```

# Reports
|Report|Location|
|------|--------|
|Coverage (HTML+RDFa)|`coverage.html`|
|Results (HTML+RDFa)|`report.html`|
|Results (Turtle)|`report.ttl`|
