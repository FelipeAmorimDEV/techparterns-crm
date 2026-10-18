ALTER TABLE lead
    ADD COLUMN situacao VARCHAR(10) NOT NULL DEFAULT 'ABERTA'
        CHECK (situacao IN ('ABERTA', 'GANHO', 'PERDIDO')),
    ADD COLUMN uf VARCHAR(2),
    ADD COLUMN cidade VARCHAR(80),
    ADD COLUMN quente BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE lead_ownership (
    lead_id         UUID                     NOT NULL PRIMARY KEY REFERENCES lead (id),
    company_id      VARCHAR(255)             NOT NULL,
    owner_user_id   UUID                     REFERENCES app_user (id),
    atribuido_em    TIMESTAMP WITH TIME ZONE,
    parado          BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_lead_ownership_company_owner ON lead_ownership (company_id, owner_user_id);

CREATE TABLE assignment (
    id                UUID                     NOT NULL PRIMARY KEY,
    company_id        VARCHAR(255)             NOT NULL,
    lead_id           UUID                     NOT NULL REFERENCES lead (id),
    previous_owner_id UUID                     REFERENCES app_user (id),
    new_owner_id      UUID                     REFERENCES app_user (id),
    regra             VARCHAR(20)              NOT NULL
        CHECK (regra IN ('MANUAL', 'RODIZIO', 'PESO', 'CARGA', 'TERRITORIO', 'FILA', 'DEVOLUCAO', 'PARADO')),
    motivo            VARCHAR(200),
    por_user_id       UUID                     REFERENCES app_user (id),
    ocorrido_em       TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_assignment_lead ON assignment (company_id, lead_id, ocorrido_em, id);

CREATE TABLE seller_distribution_config (
    company_id              VARCHAR(255) NOT NULL,
    user_id                 UUID         NOT NULL REFERENCES app_user (id),
    peso                    INT          NOT NULL CHECK (peso BETWEEN 0 AND 10),
    max_leads_abertos       INT          CHECK (max_leads_abertos IS NULL OR max_leads_abertos >= 0),
    participa_distribuicao  BOOLEAN      NOT NULL,
    PRIMARY KEY (company_id, user_id)
);

CREATE TABLE territory (
    id         UUID         NOT NULL PRIMARY KEY,
    company_id VARCHAR(255) NOT NULL,
    uf         VARCHAR(2)   NOT NULL,
    cidade     VARCHAR(80),
    user_id    UUID         NOT NULL REFERENCES app_user (id),
    CONSTRAINT uq_territory_company_uf_cidade UNIQUE NULLS NOT DISTINCT (company_id, uf, cidade)
);

CREATE TABLE distribution_settings (
    company_id               VARCHAR(255) NOT NULL PRIMARY KEY,
    auto_distribuicao        BOOLEAN      NOT NULL,
    regra_padrao             VARCHAR(20)
        CHECK (regra_padrao IS NULL OR regra_padrao IN ('RODIZIO', 'PESO', 'CARGA', 'TERRITORIO', 'FILA')),
    dias_para_parado         INT          NOT NULL CHECK (dias_para_parado >= 1),
    devolver_parado_a_fila   BOOLEAN      NOT NULL,
    ponteiro                 INT
);

ALTER TABLE envio_email DROP CONSTRAINT IF EXISTS envio_email_tipo_check;
ALTER TABLE envio_email
    ADD CONSTRAINT envio_email_tipo_check CHECK (tipo IN ('SETUP', 'RESET', 'PARADO'));
