# =============================================================================
# Backend Runtime Dockerfile - Service Virtualization Platform
# =============================================================================
#
# This Dockerfile contains ONLY runtime instructions for the backend service.
# It assumes the JAR file has already been built by the CI/CD pipeline.
#
# Purpose: Define HOW TO RUN the backend application
# Responsibility: Runtime environment and application packaging only
# Does NOT contain: Build logic, compilation, dependency management
#
# =============================================================================

# Use production-ready JRE (smaller footprint than JDK)
FROM eclipse-temurin:21-jre-alpine

# Install runtime dependencies
RUN apk add --no-cache \
    curl \
    tzdata \
    && rm -rf /var/cache/apk/*

# Create application user for security (non-root)
RUN addgroup -g 1000 appuser && \
    adduser -u 1000 -G appuser -s /bin/sh -D appuser

# Set working directory
WORKDIR /app

# Copy pre-built JAR file (built by CI/CD pipeline)
# Expects: target/*.jar (created by Maven build in CI)
COPY target/*.jar app.jar

# Set ownership to application user
RUN chown appuser:appuser app.jar

# Switch to non-root user for security
USER appuser

# Application runtime configuration
EXPOSE 8080

# Environment variables for containerized runtime
ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:+UseG1GC \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom"

# Health check for container orchestration
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Runtime labels for container metadata
LABEL maintainer="Service Virtualization Team"
LABEL description="Service Virtualization Backend - Runtime Container"
LABEL version="1.0"

# Application startup command
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# =============================================================================
# Runtime Specification Summary:
#
# Base Image: Eclipse Temurin 21 JRE Alpine (production-ready)
# User: Non-root (appuser:1000)
# Port: 8080 (Spring Boot default)
# Health: /actuator/health endpoint
# Profile: docker (container-specific configuration)
# JVM: G1GC with container support and memory limits
#
# Prerequisites:
# - JAR file must exist in target/ directory (built by CI/CD)
# - Spring Boot actuator enabled for health checks
# - Application configured for docker profile
#
# ============================================================================= 