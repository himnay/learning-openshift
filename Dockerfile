# Used by the Docker build strategy (openshift/buildconfig-docker.yaml).
# The S2I strategy (openshift/buildconfig-s2i.yaml) does NOT use this file —
# it injects the built jar into a Java S2I builder image server-side instead.
#
# The parent POMs (com.org.llm:super-pom -> com.org.learning:learning-bom) are not on
# Maven Central. Locally, hand them to the build as a named context:
#   docker build --build-context m2=$HOME/.m2/repository/com/org -t learning-openshift .
# In-cluster, deploy both to an internal repository (Nexus/Artifactory) and mount a
# settings.xml via a build secret — otherwise the build stage cannot resolve the parent.

# ---- optional local Maven repo (empty unless --build-context m2=... is passed) ----
FROM scratch AS m2

# ---- build stage ----
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace
RUN apk add --no-cache maven
COPY --from=m2 / /tmp/parents/com/org/
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2/repository \
    cp -r /tmp/parents/. /root/.m2/repository/ && \
    mvn -B -ntp -DskipTests -Dmaven.gitcommitid.skip=true package && \
    cp target/learning-openshift-*.jar /workspace/app.jar

# ---- run stage ----
FROM eclipse-temurin:25-jre-alpine
# OpenShift ignores the numeric USER at runtime — SCC assigns a random UID from the
# namespace's allocated range (see openshift/scc.yaml) — but that UID is always a member
# of GID 0, so group 0 (the existing `root` group) must own writable paths.
RUN adduser -u 1001 -S -G root appuser && \
    mkdir -p /app/logs && \
    chown -R 1001:0 /app && \
    chmod -R g=u /app
WORKDIR /app
COPY --from=build --chown=1001:0 /workspace/app.jar app.jar
USER 1001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
