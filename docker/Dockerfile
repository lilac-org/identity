# =====================================================================
# Multi-stage build for the Identity auth microservice.
#   Stage 1 builds a fat/distribution jar with the Gradle wrapper.
#   Stage 2 runs it on a slim JRE.
# JDK 25 is used for both build and runtime (project toolchain target).
# =====================================================================

# ---- Build stage ----------------------------------------------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Cache dependencies first: copy only build configuration.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY domain/build.gradle.kts ./domain/
COPY data/build.gradle.kts ./data/
COPY presentation/build.gradle.kts ./presentation/
COPY app/build.gradle.kts ./app/
RUN chmod +x gradlew && ./gradlew --no-daemon :app:dependencies || true

# Copy sources and build the runnable distribution.
COPY . .
RUN ./gradlew --no-daemon :app:installDist -x test

# ---- Runtime stage --------------------------------------------------
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# Run as an unprivileged user.
RUN useradd --system --uid 10001 --create-home appuser

COPY --from=build /workspace/app/build/install/app/ /app/

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENV SERVER_HOST=0.0.0.0
ENV SERVER_PORT=8080
EXPOSE 8080

USER appuser

# The installDist task generates a launcher script named after the app module.
ENTRYPOINT ["/app/bin/app"]
