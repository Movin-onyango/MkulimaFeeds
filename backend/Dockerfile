# ============================================================
# MkulimaFeeds Backend — Runtime-only image
# ============================================================
# This Dockerfile downloads a pre-built JAR from GitHub Releases
# instead of compiling from source. Benefits:
#   - Build completes in ~30 seconds instead of ~45 minutes
#   - No Gradle, no Kotlin, no JDK, no network for dependencies
#   - Reproducible: the JAR is a versioned, immutable artifact
#
# To update the backend, publish a new release with a bumped
# version tag, then change ARG JAR_VERSION below.
# ============================================================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Version of the JAR to download (matches GitHub release tag without the 'v')
ARG JAR_VERSION=1.0.0

# Non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Install curl (needed for download + health check), fetch the JAR
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && curl -fsSL \
       "https://github.com/Movin-onyango/MkulimaFeeds/releases/download/v${JAR_VERSION}/mkulimafeeds-backend-${JAR_VERSION}-all.jar" \
       -o /app/app.jar \
    && chown appuser:appuser /app/app.jar

# Uploads directory for product images (mounted as a volume in docker-compose)
RUN mkdir -p /app/uploads/products && chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]