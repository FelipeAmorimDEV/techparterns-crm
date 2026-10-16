CREATE TABLE invite (
    id          UUID                     NOT NULL PRIMARY KEY,
    company_id  UUID                     NOT NULL REFERENCES company (id),
    email       VARCHAR(255),
    token_hash  VARCHAR(64)                 NOT NULL UNIQUE,
    expira_em   TIMESTAMP WITH TIME ZONE NOT NULL,
    max_usos    INT                      NOT NULL DEFAULT 1 CHECK (max_usos >= 1),
    usos        INT                      NOT NULL DEFAULT 0,
    revogado_em TIMESTAMP WITH TIME ZONE,
    criado_por  UUID                     NOT NULL REFERENCES app_user (id),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_invite_usos CHECK (usos >= 0 AND usos <= max_usos)
);

CREATE INDEX idx_invite_company ON invite (company_id, created_at);

CREATE TABLE password_token (
    id            UUID                     NOT NULL PRIMARY KEY,
    user_id       UUID                     NOT NULL REFERENCES app_user (id),
    tipo          VARCHAR(10)              NOT NULL CHECK (tipo IN ('SETUP', 'RESET')),
    token_hash    VARCHAR(64)                 NOT NULL UNIQUE,
    expira_em     TIMESTAMP WITH TIME ZONE NOT NULL,
    usado_em      TIMESTAMP WITH TIME ZONE,
    invalidado_em TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_password_token_user ON password_token (user_id);

CREATE TABLE refresh_token (
    id             UUID                     NOT NULL PRIMARY KEY,
    titular_tipo   VARCHAR(10)              NOT NULL CHECK (titular_tipo IN ('APP', 'PLATFORM')),
    titular_id     UUID                     NOT NULL,
    familia_id     UUID                     NOT NULL,
    token_hash     VARCHAR(64)                 NOT NULL UNIQUE,
    expira_em      TIMESTAMP WITH TIME ZONE NOT NULL,
    rotacionado_em TIMESTAMP WITH TIME ZONE,
    revogado_em    TIMESTAMP WITH TIME ZONE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at     TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_refresh_token_familia ON refresh_token (familia_id);
CREATE INDEX idx_refresh_token_titular ON refresh_token (titular_tipo, titular_id);
