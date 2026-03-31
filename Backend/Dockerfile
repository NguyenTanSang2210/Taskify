FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar
RUN mkdir -p /app/uploads

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
