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
