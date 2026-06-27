# Identity — Authentication & Authorization Microservice

A reusable, production-grade authentication and authorization microservice built
with **Ktor 3.5.1** and **Kotlin 2.4.0**. It acts as a single, centralized
identity provider for every backend you build: it issues **RS256 JWT access
tokens** that any downstream service can verify locally through the **JWKS**
endpoint, together with rotating, reuse-detecting **refresh tokens**.

> Designed to be dropped in front of all of your projects that need auth.

---

## Base URL

The running API is currently available at:

```
https://8080.tomavue.online
```

**Every application endpoint lives under `/api/v1/`.** For example, the public
API surface is reached via:

```
https://8080.tomavue.online/api/v1/...
```

A few operational endpoints sit outside the versioned prefix (for example the
health probe and the JWKS/OpenID discovery documents used for token
verification), but everything you call as a client of this service is under
`/api/v1/`.

Interactive API documentation (when enabled) is served at
`https://8080.tomavue.online/swagger`.

---

## Highlights

- **Clean Architecture, multi-module** — `domain` (pure Kotlin), `data`
  (all technology + DI), `presentation` (Ktor HTTP), `app` (bootstrap).
- **Authentication** — register, login (email / username / phone + password),
  logout, token refresh, forgot/reset password, email verification, resend
  verification, change password, and profile read/update.
- **OAuth 2.0 social login** — Google and GitHub.
- **JWT** — RS256 signing, 15-minute stateless access tokens, 7-day stateful
  refresh tokens with rotation, reuse detection, and SHA-256-hashed storage.
  Global invalidation via a per-user `tokenVersion`.
- **Authorization** — RBAC with fine-grained `resource:action` permissions.
  Seeded `USER` and `ADMIN` roles.
- **Multi-tenant by audience** — one shared user pool; client/app registration
  with `aud` claims and a client-credentials grant for service-to-service auth.
- **Security** — Argon2id password hashing (bcrypt fallback), constant-time
  login, CORS, rate-limiting scaffolding, request validation, and forwarded-
  header awareness for correct scheme/host behind a proxy.
- **Operations** — Flyway migrations, monthly-partitioned async audit log,
  soft-delete users, structured (JSON) logging, a `/health` probe,
  OpenAPI/Swagger, and a server-rendered admin dashboard
  (kotlinx.html + HTMX).
- **Testing** — unit tests (MockK) plus integration tests (Testcontainers).

---

## Technology stack

| Concern | Choice |
| --- | --- |
| Language / runtime | Kotlin 2.4.0 on JDK 25 |
| HTTP framework | Ktor 3.5.1 (Netty engine) |
| Dependency injection | Koin 4.2.2 |
| Persistence | Exposed 1.3.0 + PostgreSQL (HikariCP pool) |
| Migrations | Flyway 12.9.0 |
| JWT / crypto | Nimbus JOSE + JWT, Argon2id, bcrypt |
| Email | Resend / SMTP (Simple Java Mail), or console (LOG) |
| Logging | Logback + Logstash encoder (text or JSON) |
| Validation | Konform |
| Testing | JUnit 5, MockK, Kotest assertions, Testcontainers |

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
- **Docker** (for the dev stack, the production stack, and integration tests).
- An **RSA key pair** for JWT signing (see below).

---

## Quick start (development)

```bash
# 1. Start PostgreSQL.
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

The service starts on `http://localhost:8080`, and all application routes are
served under `/api/v1/`. Database migrations run automatically on startup
(`DB_RUN_MIGRATIONS=true`).

---

## Production deployment

The production stack (`docker-compose.prod.yml`) runs the service image
alongside PostgreSQL. The service is built from `docker/Dockerfile` (multi-stage:
JDK 25 build → slim JRE 25 runtime, running as an unprivileged user).

### 1. Prepare secrets and keys

```bash
# RSA signing keys (mounted read-only into the container at /app/keys).
mkdir -p keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out keys/private.pem
openssl rsa -in keys/private.pem -pubout -out keys/public.pem

# Production environment file.
cp .env.example .env.prod
```

### 2. Set the important production values in `.env.prod`

| Variable | Production value | Why |
| --- | --- | --- |
| `DB_PASSWORD` | a strong secret | never ship the default |
| `FRONTEND_BASE_URL` | your public URL | used in email links / redirects |
| `CORS_ALLOWED_HOSTS` | your real origins | avoid `*` in production |
| `BEHIND_PROXY` | `true` | honor `X-Forwarded-*` (correct HTTPS scheme/host) |
| `COOKIE_SECURE` | `true` | admin-dashboard cookie over HTTPS only |
| `LOG_FORMAT` | `json` | structured logs (already forced in compose) |
| `EMAIL_PROVIDER` | `RESEND` or `SMTP` | real email delivery |

> **`BEHIND_PROXY=true` is required** when the service runs behind a TLS
> terminator (reverse proxy or a Cloudflare Tunnel). Without it the service
> derives URLs from the raw `http` connection it receives locally, which breaks
> OAuth redirect URIs (they would be sent as `http://` and rejected with
> `redirect_uri_mismatch`).

### 3. Build and run

```bash
docker compose -f docker-compose.prod.yml build --no-cache
docker compose -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml logs -f identity
```

The container exposes port `8080`. Put your TLS terminator (Cloudflare Tunnel,
Caddy, nginx, Traefik, …) in front of it and point it at `localhost:8080`. The
public API is then reachable at `https://<your-domain>/api/v1/...` — for the
current deployment, `https://8080.tomavue.online/api/v1/...`.

---

## OAuth configuration

Providers activate automatically only when **both** the client id and secret are
present in the environment. Set them in `.env.prod`:

```
OAUTH_GOOGLE_CLIENT_ID=...
OAUTH_GOOGLE_CLIENT_SECRET=...
OAUTH_GITHUB_CLIENT_ID=...
OAUTH_GITHUB_CLIENT_SECRET=...
```

Register the **exact** callback URLs in each provider console (scheme, host, and
path must match character-for-character):

```
https://8080.tomavue.online/api/v1/oauth/google/callback
https://8080.tomavue.online/api/v1/oauth/github/callback
```

- **Google** — APIs & Services → Credentials → OAuth 2.0 Client ID →
  *Authorized redirect URIs*.
- **GitHub** — Settings → Developer settings → OAuth Apps →
  *Authorization callback URL*.

With `BEHIND_PROXY=true`, the service builds redirect URIs from the forwarded
scheme/host, so they correctly resolve to `https://8080.tomavue.online/...`.

---

## Roles & administration

The `USER` and `ADMIN` roles are seeded by migrations. New accounts get `USER`.
To promote an existing account to admin, grant it the `ADMIN` role directly in
the database:

```bash
docker compose -f docker-compose.prod.yml exec postgres \
  psql -U identity -d identity -c "INSERT INTO user_roles (user_id, role_id) \
SELECT u.id, r.id FROM users u, roles r \
WHERE u.email = 'someone@example.com' AND r.name = 'ADMIN' \
ON CONFLICT DO NOTHING;"
```

Roles are encoded into the JWT at login, so the user must **log in again** (or
refresh their token) for the new role to take effect.

The server-rendered admin dashboard is available when
`ADMIN_DASHBOARD_ENABLED=true`.

---

## Configuration reference

All configuration is read from environment variables (a `.env` file is loaded
automatically in development). See `.env.example` for the complete, commented
list. The most relevant groups:

- **HTTP server** — `SERVER_HOST`, `SERVER_PORT`, `APP_NAME`,
  `FRONTEND_BASE_URL`, `CORS_ALLOWED_HOSTS`, `SWAGGER_ENABLED`,
  `ADMIN_DASHBOARD_ENABLED`, `BEHIND_PROXY`, `COOKIE_SECURE`.
- **Logging** — `LOG_FORMAT` (`text` | `json`), `LOG_LEVEL`.
- **Database** — `DB_JDBC_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_NAME`,
  `DB_MAX_POOL_SIZE`, `DB_MIN_IDLE`, `DB_RUN_MIGRATIONS`.
- **JWT** — key material (inline `*_PEM` or file `*_PATH`), `JWT_KEY_ID`,
  `JWT_ISSUER`, `JWT_DEFAULT_AUDIENCE`.
- **Token lifetimes** — `ACCESS_TOKEN_TTL_MINUTES`, `REFRESH_TOKEN_TTL_DAYS`,
  `SERVICE_TOKEN_TTL_MINUTES`, `EMAIL_VERIFICATION_TTL_HOURS`,
  `PASSWORD_RESET_TTL_HOURS`.
- **Email** — `EMAIL_PROVIDER` (`LOG` | `RESEND` | `SMTP`),
  `EMAIL_FROM_ADDRESS`, `EMAIL_FROM_NAME`, and provider credentials.
- **OAuth** — `OAUTH_GOOGLE_*`, `OAUTH_GITHUB_*`.
- **Rate limiting** — `RATE_LIMIT_ENABLED`, `RATE_LIMIT_MAX`,
  `RATE_LIMIT_WINDOW_SECONDS`.

---

## Token verification (for downstream services)

Downstream services do **not** call this API to validate every request. Instead
they verify access tokens locally:

1. Fetch the public keys once from the JWKS document and cache them.
2. Verify the JWT signature (RS256), then check `iss`, `aud`, and `exp`.
3. Read the user id, roles, and permissions from the token claims.

This keeps the identity service off the hot path while still allowing instant,
global revocation through the per-user `tokenVersion`.

---

## Build & test

```bash
./gradlew build              # compile + unit tests
./gradlew :app:run           # run locally
./gradlew :app:installDist   # produce a runnable distribution under app/build/install/app
```

Integration tests use Testcontainers and require a running Docker daemon.

---

## License

Licensed under the **Apache License, Version 2.0**. See the [`LICENSE`](LICENSE)
file for the full text and [`NOTICE`](NOTICE) for attribution.

```
Copyright 2026 Andreas Lumban Ngaol

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
