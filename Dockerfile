# Build stage
FROM gradle:8-jdk17-alpine AS build
WORKDIR /app

# Copy Gradle files
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build application
RUN gradle build --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy built JAR from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Create storage directory
RUN mkdir -p /tmp/clause/uploads && chown -R spring:spring /tmp/clause

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
