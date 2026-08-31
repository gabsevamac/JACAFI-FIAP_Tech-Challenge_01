FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

COPY config ./config

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre

RUN groupadd --system app && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app \
    && apt-get update \
    && apt-get install --no-install-recommends --yes curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build --chown=app:app /app/target/*-SNAPSHOT.jar app.jar

ENV TZ=UTC \
    SPRINGDOC_API_DOCS_ENABLED=false \
    SPRINGDOC_SWAGGER_UI_ENABLED=false
EXPOSE 8082
USER app

ENTRYPOINT ["java", "-jar", "app.jar"]
