CREATE TABLE lead (
    id           UUID                     NOT NULL PRIMARY KEY,
    company_id   VARCHAR(255)             NOT NULL,
    nome_contato VARCHAR(120)             NOT NULL,
    telefone     VARCHAR(20),
    whatsapp     VARCHAR(20),
    email        VARCHAR(255),
    origem       VARCHAR(30)              NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at   TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_lead_company_telefone ON lead (company_id, telefone);
CREATE INDEX idx_lead_company_email ON lead (company_id, email);
