#!/bin/bash

# This uses the test harness docker image and pulls tests from a repository.
# Environment variables are defined in the file `env` in the directory from which you run this script.
# The results are saved to the `conformance-tests` subdirectory

mkdir -p conformance-tests/config
cd conformance-tests || exit

# get the example tests from the repository
# in future this would either be replaced by local tests or by using the discovery process to crawl through the specs
wget -q https://github.com/solid/conformance-test-harness/archive/refs/heads/main.zip -O temp.zip
unzip -q temp.zip "conformance-test-harness-main/example/*"
mv conformance-test-harness-main/example ./example
unzip -q temp.zip "conformance-test-harness-main/config/config.ttl"
mv conformance-test-harness-main/config/config.ttl ./servers.ttl
rm -fr ./conformance-test-harness-main/
rm temp.zip

# set up the configuration to reference and map the files above
cat > ./config/application.yaml <<EOF
subjects: ./servers.ttl
sources:
  - ./example/example.ttl
target: https://github.com/solid/conformance-test-harness/ess-compat

feature:
  mappings:
    - prefix: https://github.com/solid/conformance-test-harness/example
      path: ./example
EOF

docker run -i --rm -v "$(pwd)":/data --env-file=../env solid-conformance --coverage
docker run -i --rm -v "$(pwd)":/data --env-file=../env solid-conformance
