# Used by the Docker build strategy (openshift/buildconfig-docker.yaml).
# The S2I strategy (openshift/buildconfig-s2i.yaml) does NOT use this file —
# it injects the built jar into a Java S2I builder image server-side instead.

# ---- build stage ----
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
# Offline-friendly: relies on the Maven cache already warmed by `mvn -q dependency:go-offline`
# in CI, or a multi-stage cache mount in more advanced pipelines.
RUN --mount=type=cache,target=/root/.m2 \
    apk add --no-cache maven && \
    mvn -q -DskipTests package && \
    cp target/learning-openshift-*.jar /workspace/app.jar

# ---- run stage ----
FROM eclipse-temurin:25-jre-alpine
# OpenShift ignores the numeric USER at runtime — SCC assigns a random UID from the
# namespace's allocated range (see openshift/scc.yaml) — but group 0 (root group) must
# still own writable paths, since the assigned UID is always a member of GID 0.
RUN addgroup -g 0 -S approot && \
    adduser -u 1001 -S appuser -G approot && \
    mkdir -p /app /app/logs && \
    chown -R 1001:0 /app && \
    chmod -R g=u /app
WORKDIR /app
COPY --from=build --chown=1001:0 /workspace/app.jar app.jar
USER 1001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
