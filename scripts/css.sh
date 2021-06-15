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
docker run -i --rm -v "$(pwd)":/data --env-file=css.env --network=testnet solidconformancetestbeta/conformance-test-harness
docker stop server
docker rm server
