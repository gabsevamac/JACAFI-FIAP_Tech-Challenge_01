FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

COPY config ./config

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /app/target/*-SNAPSHOT.jar app.jar

ENV TZ=UTC
EXPOSE 8082
CMD ["java", "-jar", "app.jar"]
