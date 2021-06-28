FROM openjdk:11
COPY target/solid-conformance-test-harness-runner.jar /app/harness/
COPY src/main/docker/application.yaml /app/config/
RUN git clone https://github.com/solid/specification-tests /data
WORKDIR /app
ENTRYPOINT ["java", "-jar", "/app/harness/solid-conformance-test-harness-runner.jar"]
