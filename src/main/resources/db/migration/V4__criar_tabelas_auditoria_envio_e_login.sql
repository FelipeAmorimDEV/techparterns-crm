-- Nenhuma coluna guarda senha, token, link ou corpo de e-mail.
CREATE TABLE evento_acesso (
    id           UUID                     NOT NULL PRIMARY KEY,
    tipo         VARCHAR(40)              NOT NULL,
    resultado    VARCHAR(20)              NOT NULL,
    titular_tipo VARCHAR(10),
    titular_id   UUID,
    company_id   UUID,
    ip           VARCHAR(45),
    ocorrido_em  TIMESTAMP WITH TIME ZONE NOT NULL,
    detalhe      VARCHAR(255),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at   TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_evento_acesso_ocorrido ON evento_acesso (ocorrido_em);
CREATE INDEX idx_evento_acesso_company ON evento_acesso (company_id, ocorrido_em);

CREATE TABLE envio_email (
    id         UUID                     NOT NULL PRIMARY KEY,
    user_id    UUID                     NOT NULL REFERENCES app_user (id),
    tipo       VARCHAR(10)              NOT NULL CHECK (tipo IN ('SETUP', 'RESET')),
    status     VARCHAR(10)              NOT NULL CHECK (status IN ('PENDENTE', 'ENVIADO', 'FALHA')),
    tentativas INT                      NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_envio_email_user ON envio_email (user_id, created_at);

CREATE TABLE login_falha (
    id          UUID                     NOT NULL PRIMARY KEY,
    email_hash  VARCHAR(64)                 NOT NULL,
    ip          VARCHAR(45)              NOT NULL,
    ocorrido_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_login_falha_email ON login_falha (email_hash, ocorrido_em);
CREATE INDEX idx_login_falha_ip ON login_falha (ip, ocorrido_em);

CREATE TABLE login_bloqueio (
    tipo          VARCHAR(10)  NOT NULL CHECK (tipo IN ('PAR', 'EMAIL', 'IP')),
    chave         VARCHAR(140) NOT NULL,
    falhas        INT          NOT NULL DEFAULT 0,
    bloqueado_ate TIMESTAMP WITH TIME ZONE,
    zerado_em     TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (tipo, chave)
);
