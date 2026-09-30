# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy dependency specifications first to leverage Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and build production artifact
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal hardened runtime image
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="CarePath Platform Engineering"
LABEL description="CarePath Spring Boot Core Backend Microservice"

# Create non-root application user and group for security hardening
RUN addgroup -S carepath && adduser -S carepath -G carepath

WORKDIR /app

# Copy executable jar from builder stage
COPY --from=builder /build/target/*.jar /app/carepath-backend.jar
RUN chown -R carepath:carepath /app

USER carepath:carepath

EXPOSE 8080

# Production JVM tuning and container memory sensitivity
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Healthcheck probe using the Phase 11 liveness endpoint
HEALTHCHECK --interval=20s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/carepath-backend.jar"]
