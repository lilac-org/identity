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

# Copy the distribution and hand ownership to the unprivileged runtime user.
# Without this, dependency jars stay owned by root and are NOT readable by
# appuser, which fails at runtime with:
#   NoClassDefFoundError: io/ktor/server/application/Application
# (the JVM can see the jars but cannot open them). The chmod is a belt-and-suspenders
# guarantee that every jar is world-readable regardless of cache permissions.
COPY --from=build --chown=appuser:appuser /workspace/app/build/install/app/ /app/
RUN chmod -R a+rX /app

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENV SERVER_HOST=0.0.0.0
ENV SERVER_PORT=8080
EXPOSE 8080

USER appuser

# Run via an explicit wildcard classpath. The JVM expands "/app/lib/*" to every
# jar in the distribution's lib directory (including the application jar). This is
# robust and equivalent to the generated /app/bin/app launcher.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS --enable-native-access=ALL-UNNAMED -cp \"/app/lib/*\" id.andreasmlbngaol.identity.ApplicationKt"]
