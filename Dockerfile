# ============================================================
# Runtime image using pre-built artifact from host
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: run as a non-root user
RUN addgroup -S codeguardian && adduser -S codeguardian -G codeguardian

WORKDIR /app

# Copy the pre-built fat JAR from target directory
COPY target/*.jar app.jar

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
