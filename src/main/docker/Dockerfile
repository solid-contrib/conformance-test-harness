FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24

ENV LANGUAGE='en_US:en'

USER root
RUN microdnf install -y git

RUN mkdir /app
RUN mkdir /data

RUN chown 185:185 /data
RUN chown 185:185 /app

COPY --chown=185:185 target/solid-conformance-test-harness-runner.jar /app/harness/

# Run as the default user
USER 185

RUN git clone https://github.com/solid-contrib/specification-tests /data

WORKDIR /data
RUN git checkout $(git describe --abbrev=0 --tags)

RUN mkdir -p /app/config/
RUN cp /data/application.yaml /app/config/

WORKDIR /app

ENTRYPOINT ["java", "-jar", "/app/harness/solid-conformance-test-harness-runner.jar"]
