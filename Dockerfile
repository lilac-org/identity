# =====================================================================
# Hugging Face Spaces (Docker SDK) image for the Identity auth service.
#
# Differences vs docker/Dockerfile:
#   - Listens on port 7860 (Hugging Face Spaces default app port).
#   - Runs as uid 1000 (Hugging Face convention; user "user").
#   - No key file mount: provide JWT keys inline via the
#     JWT_PRIVATE_KEY_PEM / JWT_PUBLIC_KEY_PEM secrets, and use an
#     EXTERNAL PostgreSQL (set DB_JDBC_URL) because the Space filesystem
#     is ephemeral.
#
# Hugging Face builds THIS file (it must live at the repo root).
# JDK 25 for both build and runtime (project toolchain target).
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
# NOTE: "COPY . ." overwrites the gradlew copied above with the repo copy,
# which may have lost its executable bit in git (mode 100644). Re-apply
# chmod +x here, otherwise this step fails with "./gradlew: Permission denied".
COPY . .
RUN chmod +x gradlew && ./gradlew --no-daemon :app:installDist -x test

# ---- Runtime stage --------------------------------------------------
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# Hugging Face runs Spaces as uid 1000. The base image ALREADY ships a user
# with UID 1000, so we must NOT create one (useradd fails with
# "UID 1000 is not unique"). We simply run as that numeric UID instead.

# Copy the distribution and hand ownership to the runtime user (uid 1000).
# Without --chown the dependency jars stay owned by root and are NOT
# readable by the unprivileged user, which fails at runtime with:
#   NoClassDefFoundError: io/ktor/server/application/Application
# (the JVM can see the jars but cannot open them). The chmod is a
# belt-and-suspenders guarantee that every jar is world-readable.
COPY --from=build --chown=1000:1000 /workspace/app/build/install/app/ /app/
RUN chmod -R a+rX /app

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENV HOME=/tmp
ENV SERVER_HOST=0.0.0.0
# Hugging Face Spaces routes external HTTPS traffic to this port.
ENV SERVER_PORT=7860
# Behind the Hugging Face proxy: honor X-Forwarded-* and secure cookies.
ENV BEHIND_PROXY=true
ENV COOKIE_SECURE=true
ENV LOG_FORMAT=json
EXPOSE 7860

USER 1000

# Run via an explicit wildcard classpath. The JVM expands "/app/lib/*" to
# every jar in the distribution's lib directory (including the application
# jar). Robust and equivalent to the generated /app/bin/app launcher.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS --enable-native-access=ALL-UNNAMED -cp \"/app/lib/*\" id.andreasmlbngaol.identity.ApplicationKt"]
