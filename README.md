# Identity — Authentication Microservice

A reusable, production-grade authentication & authorization microservice built
with **Ktor 3.5.1** and **Kotlin 2.4.0**. It provides a single, centralised
identity for every backend you build: it issues **RS256 JWT access tokens** that
any downstream service can verify locally via the **JWKS** endpoint, plus
rotating, reuse-detecting **refresh tokens**.

> Designed to be dropped in front of all your projects that need auth.

---

## Highlights

- **Clean Architecture, multi-module** — `domain` (pure Kotlin), `data`
  (all technology + DI), `presentation` (Ktor HTTP), `app` (bootstrap).
- **Authentication** — register, login (email / username / phone + password),
  logout, token refresh, forgot/reset password, email verification, resend
  verification, change password, profile read/update.
- **OAuth 2.0 social login** — Google & GitHub.
- **JWT** — RS256 signing, 15-minute stateless access tokens, 7-day stateful
  refresh tokens with rotation, reuse detection, and SHA-256-hashed storage.
  Global invalidation via a per-user `tokenVersion`.
- **Authorization** — RBAC with fine-grained `resource:action` permissions.
  Seeded `USER` and `ADMIN` roles.
- **Multi-tenant by audience** — one shared user pool; client/app registration
  with `aud` claims and a client-credentials grant for service-to-service.
- **Security** — Argon2id password hashing (bcrypt fallback), constant-time
  login, CORS, rate-limiting scaffolding, request validation.
- **Operations** — Flyway migrations, monthly-partitioned async audit log,
  soft-delete users, structured (JSON) logging, `/health`, OpenAPI/Swagger,
  server-rendered admin dashboard (kotlinx.html + HTMX).
- **Testing** — unit tests (MockK) + integration tests (Testcontainers).

---

## Module layout

```
identity/
├── domain/         Pure Kotlin: models, ports (interfaces), use cases, policy.
│                   No framework dependencies whatsoever.
├── data/           Implements domain ports: Exposed/Postgres, JWT (Nimbus),
│                   Argon2/bcrypt, email, OAuth clients, Koin data module,
│                   Flyway migrations (src/main/resources/db/migration).
├── presentation/   Ktor routing, DTOs, mappers, plugins, security, status
│                   pages, admin dashboard. Depends on domain only.
└── app/            Composition root: configuration, Koin wiring, bootstrap,
                    logback + OpenAPI resources.
```

**Dependency rule:** `data` and `presentation` never see each other. Only
`domain` types cross every boundary; `app` is the single place that wires
everything together.

---

## Requirements

- **JDK 25** (the Gradle toolchain targets 25).
- **PostgreSQL 14+** (17 recommended).
- **Docker** (for the dev stack and integration tests).
- An RSA key pair for JWT signing (see below).

---

## Quick start (development)

```bash
# 1. Start Postgres (and Redis, reserved for the future denylist).
docker compose -f docker-compose.dev.yml up -d

# 2. Generate a JWT key pair.
mkdir -p keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out keys/private.pem
openssl rsa -in keys/private.pem -pubout -out keys/public.pem

# 3. Configure.
cp .env.example .env      # adjust values as needed

# 4. Run.
./gradlew :app:run
```

The service starts on `http://localhost:8080`.
Migrations run automatically on startup (`DB_RUN_MIGRATIONS=true`).

### Useful endpoints

| Endpoint | Purpose |
| --- | --- |
| `GET /health` | Liveness/readiness probe |
| `GET /.well-known/jwks.json` | Public keys for token verification |
| `GET /.well-known/openid-configuration` | Discovery metadata |
| `POST /api/v1/auth/register` | Create an account |
| `POST /api/v1/auth/login` | Authenticate |
| `POST /api/v1/auth/refresh` | Rotate tokens |
| `GET /api/v1/users/me` | Current profile (bearer auth) |
| `GET /api/v1/admin/users` | Admin user list (permissioned) |
| `GET /swagger` | Swagger UI (when `SWAGGER_ENABLED=true`) |
| `GET /admin/dashboard` | Server-rendered admin dashboard |

---

## Configuration

All configuration is via environment variables (12-factor), transparently
sourced from a local `.env` in development (dotenv-kotlin). See
[`.env.example`](.env.example) for the full list with defaults. Highlights:

- **Database:** `DB_JDBC_URL`, `DB_USERNAME`, `DB_PASSWORD`.
- **JWT keys:** `JWT_PRIVATE_KEY_PATH` / `JWT_PUBLIC_KEY_PATH` (file) **or**
  `JWT_PRIVATE_KEY_PEM` / `JWT_PUBLIC_KEY_PEM` (inline, `\n`-escaped).
- **Email:** `EMAIL_PROVIDER` = `LOG` (dev) | `RESEND` | `SMTP`.
- **OAuth:** `OAUTH_GOOGLE_*`, `OAUTH_GITHUB_*` (a provider is enabled only
  when both its client id and secret are present).
- **Rate limiting:** `RATE_LIMIT_ENABLED` (off by default — implemented but not
  enforced until you opt in).

---

## Tokens & how downstream services verify them

1. The service signs access tokens with RS256 using its private key.
2. It publishes the corresponding public key at `/.well-known/jwks.json`.
3. Any backend verifies an incoming access token **locally** against the JWKS
   (no network call to this service per request), checking signature, `iss`,
   `aud`, and expiry. Authorization is carried in the token's role/permission
   and audience claims.

Refresh tokens are opaque, stored only as SHA-256 hashes, rotated on every use,
and grouped into families so that **reuse of a rotated token revokes the whole
family** (theft detection).

---

## Running tests

```bash
./gradlew test            # all modules
./gradlew :domain:test    # fast unit tests (MockK)
./gradlew :data:test      # integration tests (Testcontainers; needs Docker)
```

---

## Production with Docker

```bash
cp .env.example .env      # set real secrets (DB, JWT keys, email, OAuth)
docker compose -f docker-compose.prod.yml up -d --build
```

The image is a multi-stage build on Temurin JDK/JRE 25 and runs as a non-root
user. Structured JSON logging is enabled via `LOG_FORMAT=json`.

---

## Important build notes & caveats

This project was assembled as a complete source tree. Please read the following
before your first build:

1. **Network access is required for the first Gradle build** to resolve all the
   pinned dependency versions from Maven Central. It was **not** possible to run
   `gradle build` in the authoring environment (no network), so the tree has not
   been compiler-verified end to end. Build it once in IntelliJ IDEA / via the
   Gradle wrapper to surface any environment-specific issues.
2. **JDK 25 toolchain.** The toolchain is set to 25. Make sure your Gradle
   version and the Kotlin 2.4.0 plugin you resolve fully support a JDK 25
   toolchain in your environment; if not, lower `jvmToolchain(25)` to your
   installed LTS (e.g. 21).
3. **Exposed 1.3.0 packages.** The data layer uses the Exposed v1 package layout
   (`org.jetbrains.exposed.v1.core.*`, `org.jetbrains.exposed.v1.jdbc.*`,
   `org.jetbrains.exposed.v1.datetime.*`). If you resolve a different Exposed
   build whose package layout differs, adjust the imports in `data/.../db`.
4. **Validation.** `konform` is declared as a dependency; the current request
   validators are hand-written for precise error codes. You can migrate them to
   konform schemas if you prefer.
5. **Admin dashboard auth.** The server-rendered dashboard is intended to sit
   behind your reverse-proxy / SSO. Disable it with `ADMIN_DASHBOARD_ENABLED=false`
   if you don't want it exposed.
6. **First admin user.** Registration assigns the `USER` role. Grant `ADMIN` to
   your first administrator directly in the database, then manage the rest
   through the admin API.

---

## License

Proprietary / internal. Adapt as needed for your own projects.
