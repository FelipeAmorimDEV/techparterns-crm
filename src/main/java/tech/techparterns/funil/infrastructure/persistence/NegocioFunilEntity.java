package tech.techparterns.funil.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "funil_negocio")
public class NegocioFunilEntity {

    @Id
    @Column(name = "id")
    public UUID id;

    @Column(name = "lead_id", nullable = false, unique = true)
    public UUID leadId;

    @Column(name = "etapa_id", nullable = false)
    public UUID etapaId;

    @Column(name = "entrou_na_etapa", nullable = false)
    public Instant entrouNaEtapa;

    @Column(name = "decisor")
    public Boolean decisor;

    @Column(name = "dor_principal")
    public Boolean dorPrincipal;

    @Column(name = "usuarios_previstos")
    public Boolean usuariosPrevistos;

    @Column(name = "prazo_decisao")
    public Boolean prazoDecisao;

    @Column(name = "orcamento")
    public Boolean orcamento;

    @Column(name = "data_demo")
    public LocalDate dataDemo;

    @Column(name = "valor_estimado")
    public BigDecimal valorEstimado;

    @Column(name = "plano_interesse", length = 40)
    public String planoInteresse;

    @Column(name = "previsao_fechamento")
    public LocalDate previsaoFechamento;

    @Column(name = "plano_fechado", length = 40)
    public String planoFechado;

    @Column(name = "valor_fechado")
    public BigDecimal valorFechado;

    @Column(name = "motivo_id")
    public UUID motivoId;

    @Column(name = "observacao")
    public String observacao;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @Column(name = "deleted_at")
    public OffsetDateTime deletedAt;
}
