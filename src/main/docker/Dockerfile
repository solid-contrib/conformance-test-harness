FROM openjdk:11
COPY target/solid-conformance-test-harness-runner.jar /app/harness/
RUN git clone https://github.com/solid-contrib/specification-tests /data
WORKDIR /data
RUN git checkout $(git describe --abbrev=0 --tags)
RUN mkdir -p /app/config/
RUN cp /data/application.yaml /app/config/
WORKDIR /app
ENTRYPOINT ["java", "-jar", "/app/harness/solid-conformance-test-harness-runner.jar"]
