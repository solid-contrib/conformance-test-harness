# Usage: conformance-test-harness

The test harness used to run conformance tests against the Solid specifications.

# Prerequisites

The tests require 2 user accounts to be set up, referred to as `alice` and `bob`.
These users can be registered on any compatible Solid Identity Provider or on the server under test.

The server under test must also host a POD with a container for running tests. The `alice` user must 
have full control over the container, either because `alice` owns the POD or because `alice` has been
granted full control access.   

# Configuration
The test harness relies on uyp to 4 sources of configuration, depending on how the data is likely to be used
and how dynamic it is. They are as follows (in order of least dynamic to most).

## Test subject description
This is a Turtle file which describes the test subject and it's capabilities, primarily using
[EARL](http://www.w3.org/ns/earl#) and [DOAP](http://usefulinc.com/ns/doap#) vocabularies.
An examnple of this file is provided, containing descriptions of the following Solid implementations:
* CSS - [Community Solid Server](https://github.com/solid/community-server)
* ESS (2 versions) - [Enterprise Solid Server](https://inrupt.com/products/enterprise-solid-server)
    * Access Control Policies (ACP)
    * Compatibility mode for Web Access Controls (ACL)
* NSS - [Node Solid Server] (https://github.com/solid/node-solid-server)
* Other implementations will follow

There are some test subject specific configuration properties in this file:
```
  solid-test:origin <https://tester>   # registered origin for session-based lgin
  solid-test:maxThreads 8              # number of threads for running tests in parallel  
  solid-test:features "authentication", "acl", "wac-allow"  # server capabilities
```

## Test harness configuration
This file can have various formats though the example provided is YAML. It can be used in place of 
command line settings if desired but is only required if you want to override default settings or
map URLs or source files to a local file system. It can also control the level of logging but this
is not normally required.

The file has a specific location: `config/application.yaml` in your current working directory.
```yaml
# The first 3 can be ignored if using the command line settings: subjects, source and target 
subjects: test-subjects.ttl
sources:
  - example/solid-protocol-spec.ttl
  - example/web-access-control-spec.ttl
target: https://github.com/solid/conformance-test-harness/ess-compat

# To map URLs to local files:
feature:
  mappings:
    - prefix: https://github.com/solid/conformance-test-harness/example
      path: ./example

# Other configuration to override defaults
agent: agent-string		# default = Solid-Conformance-Test-Suite
connectTimeout: 1000	# default = 5000
readTimeout: 1000		# default = 5000
```

## Environment variables

### Server
The test harness needs to know the root URL of the server being tested and the path to the container in which test files
will be created.
```
SERVER_ROOT=	# e.g. https://pod-compat.inrupt.com or https://pod-user.inrupt.net
TEST_CONTAINER= # e.g. pod-user/test or test
```
These are used to construct the root location for test files e.g. `https://pod-compat.inrupt.com/pod-user/test/`
or `https://pod-user.inrupt.net/test`

### Authentication
The following are mandatory settings for all authentication mechanisms:
```shell
SOLID_IDENTITY_PROVIDER=	# e.g. https://inrupt.net or https://broker.pod-compat.inrupt.com
ALICE_WEBID=
BOB_WEBID=
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
ALICE_CLIENT_SECRET=
BOB_CLIENT_SECRET=
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
ALICE_REFRESH_TOKEN=
ALICE_CLIENT_ID=
ALICE_CLIENT_SECRET=
BOB_REFRESH_TOKEN=
BOB_CLIENT_ID=
BOB_CLIENT_SECRET=
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
ALICE_USERNAME=
ALICE_PASSWORD=
BOB_USERNAME=
BOB_PASSWORD=
```

The harness also needs to know the origin that has been registered as the trusted app for the users. This is included
in the test subject configuration file describing the server under test.

This mechanism will work in CI environments where the credentials can be passed in as secrets.

### Command line options

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
```

## Execution

### Running in a container
The simplest way to run the test harness is via the docker image published to
https://hub.docker.com/repository/docker/solidconformancetestbeta/conformance-test-harness

The following examples demonstrate how a script can fetch example tests and configuration files from the repository and
run tests against a publicly available server or one that is also running in a container.

#### ESS (ACL compatibility mode)
Create a file for environment variables e.g. `ess-compat.env` with the following contents (based on the 
client_credentials option):
```shell
SOLID_IDENTITY_PROVIDER=
ALICE_WEBID=
ALICE_CLIENT_SECRET=
BOB_WEBID=
BOB_CLIENT_SECRET=
SERVER_ROOT=https://pod-compat.inrupt.com
TEST_CONTAINER=/pod-owner/shared-test/
```
Next create a script based on the following:
```shell
#!/bin/bash

# This uses the test harness docker image and pulls tests from a repository.
# Environment variables are defined in an `env` file in the directory from which you run this script.

mkdir config

# get the example tests and configuration from the repository
wget -q https://github.com/solid/conformance-test-harness/archive/refs/heads/main.zip -O temp.zip
unzip -q temp.zip "conformance-test-harness-main/example/*"
mv conformance-test-harness-main/example ./example
unzip -q temp.zip "conformance-test-harness-main/config/config.ttl"
mv conformance-test-harness-main/config/config.ttl ./test-subjects.ttl
rm -fr ./conformance-test-harness-main/
rm temp.zip

# set up the configuration to reference and map the files above
cat > ./config/application.yaml <<EOF
subjects: ./test-subjects.ttl
sources:
  - https://raw.githubusercontent.com/solid/conformance-test-harness/main/example/solid-protocol-spec.ttl
  - https://raw.githubusercontent.com/solid/conformance-test-harness/main/example/web-access-control-spec.ttl
target: https://github.com/solid/conformance-test-harness/ess-compat

feature:
  mappings:
    - prefix: https://github.com/solid/conformance-test-harness/example
      path: ./example
    - prefix: https://raw.githubusercontent.com/solid/conformance-test-harness/main/example/solid
      path: ./example/solid
    - prefix: https://raw.githubusercontent.com/solid/conformance-test-harness/main/example/web
      path: ./example/web
EOF

# run the tests in the test harness
docker pull solidconformancetestbeta/conformance-test-harness
docker run -i --rm -v "$(pwd)":/data --env-file=ess-compat.env solidconformancetestbeta/conformance-test-harness --coverage
docker run -i --rm -v "$(pwd)":/data --env-file=ess-compat.env solidconformancetestbeta/conformance-test-harness
```

Run `./ess-compat.sh` and the reports will be created in the current directory. 

#### CSS in a container
Create a file for environment variables e.g. `css.env` with the following contents (based on the client_credentials
option):
```shell
SOLID_IDENTITY_PROVIDER=
ALICE_WEBID=
ALICE_CLIENT_SECRET=
BOB_WEBID=
BOB_CLIENT_SECRET=
SERVER_ROOT=http://server:3000
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

# This uses the test harness docker image and pulls tests from a repository.
# Environment variables are defined in the file `env` in the directory from which you run this script.

mkdir config

# get the example tests from the repository
wget -q https://github.com/solid/conformance-test-harness/archive/refs/heads/main.zip -O temp.zip
unzip -q temp.zip "conformance-test-harness-main/example/*"
mv conformance-test-harness-main/example ./example
unzip -q temp.zip "conformance-test-harness-main/config/config.ttl"
mv conformance-test-harness-main/config/config.ttl ./test-subjects.ttl
rm -fr ./conformance-test-harness-main/
rm temp.zip

# set up the configuration to reference and map the files above
cat > ./config/application.yaml <<EOF
subjects: ./test-subjects.ttl
sources:
  - https://raw.githubusercontent.com/solid/conformance-test-harness/main/example/solid-protocol-spec.ttl
  - https://raw.githubusercontent.com/solid/conformance-test-harness/main/example/web-access-control-spec.ttl
target: https://github.com/solid/conformance-test-harness/css

feature:
  mappings:
    - prefix: https://github.com/solid/conformance-test-harness/example
      path: ./example
    - prefix: https://raw.githubusercontent.com/solid/conformance-test-harness/main/example/solid
      path: ./example/solid
    - prefix: https://raw.githubusercontent.com/solid/conformance-test-harness/main/example/web
      path: ./example/web
EOF

# build and run CSS in a container
docker build -f Dockerfile.css -t css:latest .
docker run -d --name=server --network=testnet -p 3000:3000 -it css:latest

# run the tests in the test harness
docker pull solidconformancetestbeta/conformance-test-harness
docker run -i --rm -v "$(pwd)":/data --env-file=css.env solidconformancetestbeta/conformance-test-harness --coverage
docker run -i --rm -v "$(pwd)":/data --env-file=css.env --network=testnet solidconformancetestbeta/conformance-test-harness
docker stop server
docker rm server
```

Run `./css.sh` and the reports will be created in the current directory.

### Local execution
The test harness is packaged into a single, executable jar which is available as an asset in the release within github:
https://github.com/solid/conformance-test-harness/releases. The only dependency is on Java 11. You can run it and see
its usage as follows:
```shell
java -jar solid-conformance-test-harness-runner.jar --help
```

## Reports
|Report|Location|
|------|--------|
|Coverage (HTML+RDFa)|`coverage.html`|
|Results (HTML+RDFa)|`report.html`|
|Results (Turtle)|`report.ttl`|
|Summary report|`target/karate-reports/karate-summary.html`|
|Timeline|`target/karate-reports/karate-timeline.html`|
