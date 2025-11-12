FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.23

ENV LANGUAGE='en_US:en'

USER root
RUN microdnf install git
COPY target/solid-conformance-test-harness-runner.jar /app/harness/
RUN git clone https://github.com/solid-contrib/specification-tests /data
WORKDIR /data
RUN git checkout $(git describe --abbrev=0 --tags)
RUN mkdir -p /app/config/
RUN cp /data/application.yaml /app/config/
WORKDIR /app

# Run as the default user
USER 185

ENTRYPOINT ["java", "-jar", "/app/harness/solid-conformance-test-harness-runner.jar"]
