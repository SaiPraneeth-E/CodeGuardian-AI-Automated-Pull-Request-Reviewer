# ============================================================
# Multi-stage build: Build JAR then run
# ============================================================

# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and pom first for dependency caching
COPY pom.xml .
COPY maven-dist/ maven-dist/

# Copy source code
COPY src/ src/

# Build the JAR (skip tests for faster build)
RUN apk add --no-cache bash && \
    chmod +x maven-dist/apache-maven-3.9.7/bin/mvn && \
    maven-dist/apache-maven-3.9.7/bin/mvn package -DskipTests -q \
    -Dmaven.repo.local=/build/.m2/repository

# ============================================================
# Stage 2: Runtime image
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: run as a non-root user
RUN addgroup -S codeguardian && adduser -S codeguardian -G codeguardian

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Set ownership
RUN chown -R codeguardian:codeguardian /app

USER codeguardian

# Expose the Spring Boot default port
EXPOSE 8080

# Health check using Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run with the docker profile
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
