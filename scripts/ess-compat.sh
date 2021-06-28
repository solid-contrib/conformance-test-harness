#!/bin/bash

# This uses the test harness docker image which includes tests pulled from a repository.
# Environment variables are defined in an `env` file in the directory from which you run this script.

mkdir reports/ess-compat
docker pull solidconformancetestbeta/conformance-test-harness
docker run -i --rm -v "$(pwd)"/reports/ess-compat:/reports --env-file=ess-compat.env solidconformancetestbeta/conformance-test-harness --output=/reports --target=ess-compat
