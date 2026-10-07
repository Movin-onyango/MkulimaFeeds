# ============================================================
# STAGE 1 — Build
# ============================================================
FROM gradle:8.7-jdk17 AS build

WORKDIR /app

# Copy Gradle wrapper and build scripts first (layer caching)
COPY gradle/ ./gradle/
COPY gradlew ./
COPY gradlew.bat ./
COPY settings.gradle.kts ./
COPY build.gradle.kts ./
COPY gradle.properties* ./

# Convert gradlew to Unix line endings (in case it was saved on Windows)
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Pre-fetch dependencies (cached unless build files change)
RUN ./gradlew dependencies --no-daemon || true

# Copy source and build a fat JAR
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
