FROM openjdk:11
COPY target/solid-conformance-test-harness-runner.jar /app/
WORKDIR /data
ENTRYPOINT ["java", "-jar", "/app/solid-conformance-test-harness-runner.jar"]
