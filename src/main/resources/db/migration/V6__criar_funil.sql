CREATE TABLE funil_etapa (
    id                   UUID                     NOT NULL PRIMARY KEY,
    company_id           VARCHAR(255)             NOT NULL,
    nome                 TEXT                     NOT NULL,
    tipo                 VARCHAR(10)              NOT NULL
        CHECK (tipo IN ('ABERTO', 'GANHO', 'PERDIDO')),
    ordem                INT                      NOT NULL,
    ativa                BOOLEAN                  NOT NULL,
    probabilidade        INT,
    cor                  VARCHAR(7),
    sla_dias             INT,
    exige_qualificacao   BOOLEAN                  NOT NULL,
    exige_demo           BOOLEAN                  NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at           TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_funil_etapa_company_ordem UNIQUE (company_id, ordem)
);

CREATE UNIQUE INDEX uq_funil_etapa_ganho_ativa
    ON funil_etapa (company_id)
    WHERE tipo = 'GANHO' AND ativa = TRUE;

CREATE UNIQUE INDEX uq_funil_etapa_perdido_ativa
    ON funil_etapa (company_id)
    WHERE tipo = 'PERDIDO' AND ativa = TRUE;

CREATE TABLE funil_motivo_perda (
    id          UUID                     NOT NULL PRIMARY KEY,
    company_id  VARCHAR(255)             NOT NULL,
    nome        TEXT                     NOT NULL,
    ativo       BOOLEAN                  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_funil_motivo_company_nome UNIQUE (company_id, nome)
);

CREATE TABLE funil_negocio (
    id                    UUID                     NOT NULL PRIMARY KEY,
    lead_id               UUID                     NOT NULL UNIQUE REFERENCES lead (id),
    etapa_id              UUID                     NOT NULL REFERENCES funil_etapa (id),
    entrou_na_etapa       TIMESTAMP WITH TIME ZONE NOT NULL,
    decisor               BOOLEAN,
    dor_principal         BOOLEAN,
    usuarios_previstos    BOOLEAN,
    prazo_decisao         BOOLEAN,
    orcamento             BOOLEAN,
    data_demo             DATE,
    valor_estimado        NUMERIC,
    plano_interesse       VARCHAR(40),
    previsao_fechamento   DATE,
    plano_fechado         VARCHAR(40),
    valor_fechado         NUMERIC,
    motivo_id             UUID                     REFERENCES funil_motivo_perda (id),
    observacao            TEXT,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at            TIMESTAMP WITH TIME ZONE
);

CREATE TABLE funil_historico_etapa (
    id                UUID                     NOT NULL PRIMARY KEY,
    origem_etapa_id   UUID                     REFERENCES funil_etapa (id),
    destino_etapa_id  UUID                     NOT NULL REFERENCES funil_etapa (id),
    user_id           UUID,
    instante          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at        TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_funil_historico_destino ON funil_historico_etapa (destino_etapa_id);
