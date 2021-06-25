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
  - https://raw.githubusercontent.com/solid/conformance-test-harness/example/main/protocol/solid-protocol-test-manifest.ttl
  - https://raw.githubusercontent.com/solid/conformance-test-harness/example/main/web-access-control/web-access-control-test-manifest.ttl
  - https://raw.githubusercontent.com/solid/conformance-test-harness/example/main/protocol/solid-protocol-spec.ttl
  - https://raw.githubusercontent.com/solid/conformance-test-harness/example/main/web-access-control/web-access-control-spec.ttl
target: https://github.com/solid/conformance-test-harness/ess

mappings:
  - prefix: https://raw.githubusercontent.com/solid/conformance-test-harness/example/main
    path: ./example
EOF

# run the tests in the test harness
docker pull solidconformancetestbeta/conformance-test-harness
docker run -i --rm -v "$(pwd)":/data --env-file=ess.env solidconformancetestbeta/conformance-test-harness
