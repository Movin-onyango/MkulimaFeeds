# ============================================================
# STAGE 1 — Build
# ============================================================
# Match the base image to the version in gradle-wrapper.properties.
# Using gradle:8.7 was wasteful because the wrapper downloads a fresh
# Gradle distribution anyway.
FROM gradle:9.5-jdk17 AS build

WORKDIR /app

# Copy Gradle wrapper and build scripts first (layer caching)
COPY gradle/ ./gradle/
COPY gradlew ./
COPY gradlew.bat ./
COPY settings.gradle.kts ./
COPY build.gradle.kts ./
COPY gradle.properties* ./

# Fix CRLF (Windows line endings) and make gradlew executable
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# The Gradle base image already has the distribution installed.
# Dependencies still need to be downloaded once and cached.
# Note: we do NOT use a Docker cache mount here because of known
#       concurrency issues with Gradle's internal caches under
#       BuildKit cache mounts. Docker's own layer cache is sufficient.
RUN ./gradlew dependencies --no-daemon || true

# Copy source and build the fat JAR
COPY src/ ./src/
RUN ./gradlew buildFatJar --no-daemon -x test

# ============================================================
# STAGE 2 — Runtime
# ============================================================
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

# Non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Copy the fat JAR from the build stage
COPY --from=build /app/build/libs/*-all.jar ./app.jar

# Uploads directory (used for product images)
RUN mkdir -p /app/uploads/products && chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

# Health check — hits the /api/health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]