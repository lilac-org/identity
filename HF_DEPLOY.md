# Deploying to Hugging Face Spaces (Docker)

This guide deploys the Identity auth service as a **Docker Space**. Because a
Space runs a single ephemeral container, you must use an **external PostgreSQL**
and provide the **JWT keys inline** (no file mounts, no docker-compose).

---

## 1. Prerequisites

- A free Hugging Face account.
- An **external PostgreSQL** database. Free options: **Neon**, **Supabase**,
  **Railway**. Copy its connection details (host, port, db, user, password).
  Make sure SSL is allowed (Neon/Supabase require `sslmode=require`).
- An RSA key pair for JWT signing (generate locally):
  ```bash
  mkdir -p keys
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out keys/private.pem
  openssl rsa -in keys/private.pem -pubout -out keys/public.pem
  ```

---

## 2. Create the Space

1. Go to https://huggingface.co/new-space
2. **Owner**: andreasmlbngaol · **Space name**: e.g. `identity`
3. **SDK**: choose **Docker** → **Blank**
4. **Visibility**: **Public** (so your API and OAuth callbacks are reachable)

Your Space URL will be:
```
https://andreasmlbngaol-identity.hf.space
```
(The pattern is `https://<owner>-<space>.hf.space`.) All API routes are under
`/api/v1/`, e.g. `https://andreasmlbngaol-identity.hf.space/api/v1/...`.

---

## 3. Push the code

The Space is a git repo. Push the **whole project** (it builds the root
`Dockerfile`). Hugging Face reads the Space config from the YAML front-matter
at the top of `README.md` (already added).

```bash
# Authenticate once (use a HF access token with write scope).
pip install -U "huggingface_hub[cli]"
huggingface-cli login

# From the project root, add the Space as a remote and push.
git remote add space https://huggingface.co/spaces/andreasmlbngaol/identity
git push space main
```

Hugging Face will start building the Docker image automatically. The first
build downloads Gradle dependencies and can take several minutes.

> Tip: large/secret files must NOT be committed. `keys/` and `.env*` are in
> `.gitignore` — keep it that way. Keys go in **Secrets** (next step), not git.

---

## 4. Configure Secrets & Variables

In the Space: **Settings → Variables and secrets**. Put sensitive values as
**Secrets**, the rest as **Variables**. All of them are injected as environment
variables at runtime.

### Required

| Key | Value | Type |
| --- | --- | --- |
| `DB_JDBC_URL` | `jdbc:postgresql://<host>:<port>/<db>?sslmode=require` | Secret |
| `DB_USERNAME` | your DB user | Secret |
| `DB_PASSWORD` | your DB password | Secret |
| `DB_NAME` | your DB name | Variable |
| `JWT_PRIVATE_KEY_PEM` | private key as a single line (see below) | Secret |
| `JWT_PUBLIC_KEY_PEM` | public key as a single line (see below) | Secret |
| `FRONTEND_BASE_URL` | `https://andreasmlbngaol-identity.hf.space` | Variable |
| `CORS_ALLOWED_HOSTS` | `https://andreasmlbngaol-identity.hf.space` | Variable |

### Already set by the Dockerfile (no need to add, but you may override)

`BEHIND_PROXY=true`, `COOKIE_SECURE=true`, `LOG_FORMAT=json`,
`SERVER_PORT=7860`, `SERVER_HOST=0.0.0.0`.

### Optional

| Key | Value |
| --- | --- |
| `EMAIL_PROVIDER` | `RESEND` (or `SMTP`) |
| `RESEND_API_KEY` | your Resend key (Secret) |
| `EMAIL_FROM_ADDRESS` | e.g. `no-reply@yourdomain` |
| `EMAIL_FROM_NAME` | e.g. `Identity` |
| `OAUTH_GOOGLE_CLIENT_ID` / `OAUTH_GOOGLE_CLIENT_SECRET` | from Google Cloud (Secret) |
| `OAUTH_GITHUB_CLIENT_ID` / `OAUTH_GITHUB_CLIENT_SECRET` | from GitHub (Secret) |
| `SWAGGER_ENABLED` | `true` / `false` |
| `ADMIN_DASHBOARD_ENABLED` | `true` / `false` |

### Converting a PEM key to a single line

Hugging Face secret values are easiest to handle as one line. Convert the
newlines to literal `\n` (the app understands this form):

```bash
awk '{printf "%s\\n", $0}' keys/private.pem   # paste output into JWT_PRIVATE_KEY_PEM
awk '{printf "%s\\n", $0}' keys/public.pem    # paste output into JWT_PUBLIC_KEY_PEM
```

---

## 5. OAuth redirect URIs

If you use social login, register the callback URLs with the **new HF domain**
(exact match, `https`, no port):

```
https://andreasmlbngaol-identity.hf.space/api/v1/oauth/google/callback
https://andreasmlbngaol-identity.hf.space/api/v1/oauth/github/callback
```

- Google: APIs & Services → Credentials → OAuth client → Authorized redirect URIs.
- GitHub: Developer settings → OAuth Apps → Authorization callback URL.

`BEHIND_PROXY=true` (already set) makes the service build these `https` URLs
correctly behind the HF proxy.

---

## 6. Verify

After the build goes green:

```bash
# Health probe (outside /api/v1, used by the platform).
curl -s https://andreasmlbngaol-identity.hf.space/health

# Confirm OAuth redirect uses https (if configured).
curl -s -i "https://andreasmlbngaol-identity.hf.space/api/v1/oauth/google" \
  | grep -io 'redirect_uri=[^&\"]*'
```

Migrations run automatically on the external DB (`DB_RUN_MIGRATIONS=true`).
To promote an admin, run the SQL from the README against your external DB.

---

## Caveats (read me)

- **Ephemeral filesystem.** Anything written inside the container is lost on
  rebuild/restart. That is why the DB is external and the keys are inline.
- **Restarts / pauses.** Free Spaces can be restarted; not ideal for an
  always-on auth backend. For real production prefer Railway / Render / Fly.io /
  a VPS (your existing Cloudflare Tunnel + Docker setup is already a better fit).
- **Single public port.** Only the app port (7860) is exposed; API + admin
  dashboard share it, which is fine here.
- **Keep the same RSA keys** across restarts (they live in Secrets), otherwise
  previously issued tokens stop verifying.
