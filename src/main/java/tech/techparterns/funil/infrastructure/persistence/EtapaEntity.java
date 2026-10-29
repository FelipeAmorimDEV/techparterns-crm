package tech.techparterns.funil.infrastructure.persistence;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "funil_etapa")
public class EtapaEntity {

    @Id
    @Column(name = "id")
    public UUID id;

    @Column(name = "company_id", nullable = false)
    public String companyId;

    @Column(name = "nome", nullable = false)
    public String nome;

    @Column(name = "tipo", nullable = false, length = 10)
    public String tipo;

    @Column(name = "ordem", nullable = false)
    public int ordem;

    @Column(name = "ativa", nullable = false)
    public boolean ativa;

    @Column(name = "probabilidade")
    public Integer probabilidade;

    @Column(name = "cor", length = 7)
    public String cor;

    @Column(name = "sla_dias")
    public Integer slaDias;

    @Column(name = "exige_qualificacao", nullable = false)
    public boolean exigeQualificacao;

    @Column(name = "exige_demo", nullable = false)
    public boolean exigeDemo;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @Column(name = "deleted_at")
    public OffsetDateTime deletedAt;
}
