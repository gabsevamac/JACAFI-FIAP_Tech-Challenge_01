# syntax=docker/dockerfile:1.27.1

FROM maven:3.9.16-eclipse-temurin-25-alpine AS build
WORKDIR /build

COPY pom.xml ./
COPY .mvn/maven.config .mvn/settings.xml ./.mvn/
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src/main ./src/main
RUN mvn --batch-mode --no-transfer-progress --offline package \
        -Dmaven.test.skip=true \
        -Dspotless.check.skip=true \
        -Dcheckstyle.skip=true \
        -Denforcer.skip=true \
        -Djacoco.skip=true \
    && java -Djarmode=tools -jar target/*.jar extract \
        --layers \
        --application-filename app.jar \
        --destination extracted

FROM eclipse-temurin:25.0.4.1_1-jdk-alpine-3.24 AS jre

# Modules reported by jdeps --print-module-deps for the application and its dependencies.
RUN jlink \
        --add-modules java.base,java.compiler,java.desktop,java.instrument,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.sql.rowset,jdk.jfr,jdk.management,jdk.unsupported \
        --strip-debug \
        --no-man-pages \
        --no-header-files \
        --compress=zip-6 \
        --output /runtime

FROM alpine:3.24.2

LABEL org.opencontainers.image.title="jacafi-server" \
      org.opencontainers.image.source="https://github.com/gabsevamac/JACAFI-FIAP_Tech-Challenge_01" \
      org.opencontainers.image.licenses="MIT"

RUN addgroup -S -g 10001 app \
    && adduser -S -D -H -u 10001 -G app -h /app -s /sbin/nologin app

COPY --from=jre /runtime /opt/java

WORKDIR /app
COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./

USER 10001:10001
EXPOSE 8082

ENTRYPOINT ["/opt/java/bin/java", "-jar", "app.jar"]
