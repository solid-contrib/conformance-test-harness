#!/bin/bash

# This uses the test harness docker image with the example tests embedded.
# Environment variables are defined in the file "env" in the directory from which you run this script.
# The results are saved to the working directory

docker run -i --rm -v "$(pwd)":/app/output --env-file=env solid-conformance-examples --coverage --output output
docker run -i --rm -v "$(pwd)":/app/output --env-file=env solid-conformance-examples --output output
