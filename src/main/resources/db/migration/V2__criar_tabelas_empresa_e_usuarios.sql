CREATE TABLE company (
    id               UUID                     NOT NULL PRIMARY KEY,
    nome             VARCHAR(120)             NOT NULL,
    status           VARCHAR(20)              NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    suspensa_em      TIMESTAMP WITH TIME ZONE,
    motivo_suspensao VARCHAR(255),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at       TIMESTAMP WITH TIME ZONE
);

CREATE TABLE platform_user (
    id              UUID                     NOT NULL PRIMARY KEY,
    nome            VARCHAR(120)             NOT NULL,
    email           VARCHAR(255)             NOT NULL UNIQUE CHECK (email = lower(email)),
    password_hash   VARCHAR(100),
    ultimo_login_em TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE TABLE app_user (
    id              UUID                     NOT NULL PRIMARY KEY,
    company_id      UUID                     NOT NULL REFERENCES company (id),
    nome            VARCHAR(120)             NOT NULL,
    email           VARCHAR(255)             NOT NULL UNIQUE CHECK (email = lower(email)),
    telefone        VARCHAR(20),
    password_hash   VARCHAR(100),
    role            VARCHAR(20)              NOT NULL CHECK (role IN ('ADMIN', 'SELLER')),
    status          VARCHAR(20)              NOT NULL CHECK (status IN ('PENDING', 'ACTIVE', 'INACTIVE')),
    ultimo_login_em TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_app_user_company_status_role ON app_user (company_id, status, role);
CREATE INDEX idx_app_user_company_nome ON app_user (company_id, nome, id);
