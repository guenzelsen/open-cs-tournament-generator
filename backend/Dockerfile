# 1. Build Stage
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src

# Build the fat JAR (skip tests to speed up the docker build, tests ran in CI)
RUN ./gradlew bootJar -x test

# 2. Run Stage
# Using an Alpine or minimal JRE image that supports ARM64/ARM32 natively
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
