-- =====================================================================
-- V1: Core identity schema
-- Mirrors the Exposed table definitions in data/db/Tables.kt exactly.
-- All timestamps are stored without time zone (UTC by convention).
-- =====================================================================

-- ---------------------------------------------------------------------
-- Users
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id              UUID         NOT NULL PRIMARY KEY,
    email           VARCHAR(320) NOT NULL,
    username        VARCHAR(32)  NOT NULL,
    password_hash   VARCHAR(255),
    status          VARCHAR(32)  NOT NULL,
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    full_name       VARCHAR(255),
    photo_url       VARCHAR(1024),
    phone_number    VARCHAR(32),
    phone_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    date_of_birth   DATE,
    token_version   INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    deleted_at      TIMESTAMP
);
CREATE UNIQUE INDEX uq_users_email        ON users (email);
CREATE UNIQUE INDEX uq_users_username      ON users (username);
CREATE UNIQUE INDEX uq_users_phone_number  ON users (phone_number);
CREATE INDEX        ix_users_status        ON users (status);
CREATE INDEX        ix_users_deleted_at    ON users (deleted_at);

-- ---------------------------------------------------------------------
-- Roles & permissions (RBAC)
-- ---------------------------------------------------------------------
CREATE TABLE roles (
    id          UUID        NOT NULL PRIMARY KEY,
    name        VARCHAR(64) NOT NULL,
    description VARCHAR(255),
    is_system   BOOLEAN     NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX uq_roles_name ON roles (name);

CREATE TABLE permissions (
    id          UUID         NOT NULL PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(255)
);
CREATE UNIQUE INDEX uq_permissions_name ON permissions (name);

CREATE TABLE role_permissions (
    role_id       UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);
CREATE INDEX ix_user_roles_role_id ON user_roles (role_id);

-- ---------------------------------------------------------------------
-- Refresh tokens (stateful, rotation + reuse detection)
-- ---------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id                   UUID        NOT NULL PRIMARY KEY,
    user_id              UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    family_id            UUID        NOT NULL,
    token_hash           VARCHAR(64) NOT NULL,
    client_id            VARCHAR(128),
    issued_at            TIMESTAMP   NOT NULL,
    expires_at           TIMESTAMP   NOT NULL,
    used_at              TIMESTAMP,
    revoked_at           TIMESTAMP,
    replaced_by_token_id UUID,
    user_agent           VARCHAR(512),
    ip_address           VARCHAR(64)
);
CREATE UNIQUE INDEX uq_refresh_tokens_token_hash ON refresh_tokens (token_hash);
CREATE INDEX        ix_refresh_tokens_family_id  ON refresh_tokens (family_id);
CREATE INDEX        ix_refresh_tokens_user_id    ON refresh_tokens (user_id);
CREATE INDEX        ix_refresh_tokens_expires_at ON refresh_tokens (expires_at);

-- ---------------------------------------------------------------------
-- OAuth linked accounts
-- ---------------------------------------------------------------------
CREATE TABLE oauth_accounts (
    id               UUID         NOT NULL PRIMARY KEY,
    user_id          UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider         VARCHAR(32)  NOT NULL,
    provider_user_id VARCHAR(191) NOT NULL,
    email            VARCHAR(320),
    created_at       TIMESTAMP    NOT NULL
);
CREATE UNIQUE INDEX uq_oauth_provider_user ON oauth_accounts (provider, provider_user_id);
CREATE INDEX        ix_oauth_user_id       ON oauth_accounts (user_id);

-- ---------------------------------------------------------------------
-- OAuth / service clients (audiences, client_credentials)
-- ---------------------------------------------------------------------
CREATE TABLE clients (
    id                  UUID         NOT NULL PRIMARY KEY,
    client_id           VARCHAR(128) NOT NULL,
    client_secret_hash  VARCHAR(255),
    name                VARCHAR(191) NOT NULL,
    allowed_audiences   TEXT         NOT NULL DEFAULT '',
    allowed_scopes      TEXT         NOT NULL DEFAULT '',
    redirect_uris       TEXT         NOT NULL DEFAULT '',
    is_confidential     BOOLEAN      NOT NULL DEFAULT TRUE,
    enabled             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP    NOT NULL,
    updated_at          TIMESTAMP    NOT NULL
);
CREATE UNIQUE INDEX uq_clients_client_id ON clients (client_id);

-- ---------------------------------------------------------------------
-- Email verification tokens (stored as SHA-256 hashes)
-- ---------------------------------------------------------------------
CREATE TABLE email_verification_tokens (
    id         UUID        NOT NULL PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP   NOT NULL,
    used_at    TIMESTAMP,
    created_at TIMESTAMP   NOT NULL
);
CREATE UNIQUE INDEX uq_email_verif_token_hash ON email_verification_tokens (token_hash);
CREATE INDEX        ix_email_verif_user_id    ON email_verification_tokens (user_id);

-- ---------------------------------------------------------------------
-- Password reset tokens (stored as SHA-256 hashes)
-- ---------------------------------------------------------------------
CREATE TABLE password_reset_tokens (
    id         UUID        NOT NULL PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP   NOT NULL,
    used_at    TIMESTAMP,
    created_at TIMESTAMP   NOT NULL
);
CREATE UNIQUE INDEX uq_pwd_reset_token_hash ON password_reset_tokens (token_hash);
CREATE INDEX        ix_pwd_reset_user_id    ON password_reset_tokens (user_id);

-- ---------------------------------------------------------------------
-- Audit log (append-only, RANGE-partitioned by month on created_at).
-- The partition key must be part of the primary key in PostgreSQL, so the
-- PK is composite (id, created_at). A DEFAULT partition guarantees writes
-- never fail; monthly partitions are pre-created for operational windows
-- and should be extended by a scheduled maintenance job.
-- ---------------------------------------------------------------------
CREATE TABLE audit_logs (
    id         UUID        NOT NULL,
    action     VARCHAR(64) NOT NULL,
    user_id    UUID,
    client_id  VARCHAR(128),
    ip_address VARCHAR(64),
    user_agent VARCHAR(512),
    metadata   TEXT        NOT NULL DEFAULT '{}',
    success    BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP   NOT NULL,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

CREATE INDEX ix_audit_logs_action     ON audit_logs (action);
CREATE INDEX ix_audit_logs_user_id    ON audit_logs (user_id);
CREATE INDEX ix_audit_logs_created_at ON audit_logs (created_at);

-- Fallback partition: catches any row outside the explicit monthly ranges.
CREATE TABLE audit_logs_default PARTITION OF audit_logs DEFAULT;

-- Pre-created monthly partitions (2026). Extend via scheduled maintenance.
CREATE TABLE audit_logs_2026_01 PARTITION OF audit_logs FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE audit_logs_2026_02 PARTITION OF audit_logs FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
CREATE TABLE audit_logs_2026_03 PARTITION OF audit_logs FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE audit_logs_2026_04 PARTITION OF audit_logs FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE audit_logs_2026_05 PARTITION OF audit_logs FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
CREATE TABLE audit_logs_2026_06 PARTITION OF audit_logs FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
CREATE TABLE audit_logs_2026_07 PARTITION OF audit_logs FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
CREATE TABLE audit_logs_2026_08 PARTITION OF audit_logs FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');
CREATE TABLE audit_logs_2026_09 PARTITION OF audit_logs FOR VALUES FROM ('2026-09-01') TO ('2026-10-01');
CREATE TABLE audit_logs_2026_10 PARTITION OF audit_logs FOR VALUES FROM ('2026-10-01') TO ('2026-11-01');
CREATE TABLE audit_logs_2026_11 PARTITION OF audit_logs FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');
CREATE TABLE audit_logs_2026_12 PARTITION OF audit_logs FOR VALUES FROM ('2026-12-01') TO ('2027-01-01');
