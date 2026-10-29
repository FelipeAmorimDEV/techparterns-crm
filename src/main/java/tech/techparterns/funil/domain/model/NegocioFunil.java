package tech.techparterns.funil.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import tech.techparterns.shared.domain.Entity;
import tech.techparterns.shared.domain.Result;

public class NegocioFunil extends Entity {

    public static final String LEAD_OBRIGATORIO = "Lead é obrigatório";
    public static final String ETAPA_OBRIGATORIA = "Etapa é obrigatória";
    public static final String ENTRADA_OBRIGATORIA = "Instante de entrada na etapa é obrigatório";

    private final UUID leadId;
    private final UUID etapaId;
    private final Instant entrouNaEtapa;
    private final Boolean decisor;
    private final Boolean dorPrincipal;
    private final Boolean usuariosPrevistos;
    private final Boolean prazoDecisao;
    private final Boolean orcamento;
    private final LocalDate dataDemo;
    private final ValorComercial valorEstimado;
    private final TextoPlano planoInteresse;
    private final LocalDate previsaoFechamento;
    private final TextoPlano planoFechado;
    private final ValorComercial valorFechado;
    private final UUID motivoPerdaId;
    private final String observacao;

    private NegocioFunil(UUID id, UUID leadId, UUID etapaId, Instant entrouNaEtapa) {
        super(id);
        this.leadId = leadId;
        this.etapaId = etapaId;
        this.entrouNaEtapa = entrouNaEtapa;
        this.decisor = null;
        this.dorPrincipal = null;
        this.usuariosPrevistos = null;
        this.prazoDecisao = null;
        this.orcamento = null;
        this.dataDemo = null;
        this.valorEstimado = null;
        this.planoInteresse = null;
        this.previsaoFechamento = null;
        this.planoFechado = null;
        this.valorFechado = null;
        this.motivoPerdaId = null;
        this.observacao = null;
    }

    private NegocioFunil(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                         UUID leadId, UUID etapaId, Instant entrouNaEtapa,
                         Boolean decisor, Boolean dorPrincipal, Boolean usuariosPrevistos,
                         Boolean prazoDecisao, Boolean orcamento, LocalDate dataDemo,
                         ValorComercial valorEstimado, TextoPlano planoInteresse, LocalDate previsaoFechamento,
                         TextoPlano planoFechado, ValorComercial valorFechado, UUID motivoPerdaId,
                         String observacao) {
        super(id, createdAt, updatedAt, deletedAt);
        this.leadId = leadId;
        this.etapaId = etapaId;
        this.entrouNaEtapa = entrouNaEtapa;
        this.decisor = decisor;
        this.dorPrincipal = dorPrincipal;
        this.usuariosPrevistos = usuariosPrevistos;
        this.prazoDecisao = prazoDecisao;
        this.orcamento = orcamento;
        this.dataDemo = dataDemo;
        this.valorEstimado = valorEstimado;
        this.planoInteresse = planoInteresse;
        this.previsaoFechamento = previsaoFechamento;
        this.planoFechado = planoFechado;
        this.valorFechado = valorFechado;
        this.motivoPerdaId = motivoPerdaId;
        this.observacao = observacao;
    }

    public static Result<NegocioFunil> tryCreate(UUID leadId, UUID etapaId, Instant entrouNaEtapa) {
        var leadResult = leadId == null ? Result.<UUID>fail(LEAD_OBRIGATORIO) : Result.ok(leadId);
        var etapaResult = etapaId == null ? Result.<UUID>fail(ETAPA_OBRIGATORIA) : Result.ok(etapaId);
        var entradaResult = entrouNaEtapa == null
                ? Result.<Instant>fail(ENTRADA_OBRIGATORIA)
                : Result.ok(entrouNaEtapa);
        var validacao = Result.combineAll(leadResult, etapaResult, entradaResult);
        if (validacao.isFailure())
            return Result.propagate(validacao);
        return Result.ok(new NegocioFunil(
                UUID.randomUUID(),
                leadResult.getValue(),
                etapaResult.getValue(),
                entradaResult.getValue()));
    }

    public static NegocioFunil reconstituir(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                                            UUID leadId, UUID etapaId, Instant entrouNaEtapa,
                                            Boolean decisor, Boolean dorPrincipal, Boolean usuariosPrevistos,
                                            Boolean prazoDecisao, Boolean orcamento, LocalDate dataDemo,
                                            ValorComercial valorEstimado, TextoPlano planoInteresse,
                                            LocalDate previsaoFechamento, TextoPlano planoFechado,
                                            ValorComercial valorFechado, UUID motivoPerdaId, String observacao) {
        return new NegocioFunil(id, createdAt, updatedAt, deletedAt, leadId, etapaId, entrouNaEtapa,
                decisor, dorPrincipal, usuariosPrevistos, prazoDecisao, orcamento, dataDemo,
                valorEstimado, planoInteresse, previsaoFechamento, planoFechado, valorFechado,
                motivoPerdaId, observacao);
    }

    public Result<NegocioFunil> comDadosAbertos(Boolean decisor, Boolean dorPrincipal,
                                                Boolean usuariosPrevistos, Boolean prazoDecisao,
                                                Boolean orcamento, LocalDate dataDemo,
                                                BigDecimal valorEstimado, String planoInteresse,
                                                LocalDate previsaoFechamento) {
        ValorComercial valor = null;
        if (valorEstimado != null) {
            var valorResult = ValorComercial.tryCreate(valorEstimado);
            if (valorResult.isFailure())
                return Result.propagate(valorResult);
            valor = valorResult.getValue();
        }
        TextoPlano plano = null;
        if (planoInteresse != null && !planoInteresse.trim().isEmpty()) {
            var planoResult = TextoPlano.tryCreate(planoInteresse);
            if (planoResult.isFailure())
                return Result.propagate(planoResult);
            plano = planoResult.getValue();
        }
        return Result.ok(reconstituir(
                getId(), getCreatedAt(), Instant.now(), getDeletedAt(),
                leadId, etapaId, entrouNaEtapa,
                decisor, dorPrincipal, usuariosPrevistos, prazoDecisao, orcamento,
                dataDemo, valor, plano, previsaoFechamento,
                planoFechado, valorFechado, motivoPerdaId, observacao));
    }

    public NegocioFunil moverPara(UUID etapaDestinoId, Instant quando) {
        return reconstituir(
                getId(), getCreatedAt(), Instant.now(), getDeletedAt(),
                leadId, etapaDestinoId, quando,
                decisor, dorPrincipal, usuariosPrevistos, prazoDecisao, orcamento,
                dataDemo, valorEstimado, planoInteresse, previsaoFechamento,
                planoFechado, valorFechado, motivoPerdaId, observacao);
    }

    public NegocioFunil comGanho(UUID etapaDestinoId, Instant quando,
                                 TextoPlano planoFechadoNovo, ValorComercial valorFechadoNovo) {
        return reconstituir(
                getId(), getCreatedAt(), Instant.now(), getDeletedAt(),
                leadId, etapaDestinoId, quando,
                decisor, dorPrincipal, usuariosPrevistos, prazoDecisao, orcamento,
                dataDemo, valorEstimado, planoInteresse, previsaoFechamento,
                planoFechadoNovo, valorFechadoNovo, motivoPerdaId, observacao);
    }

    public NegocioFunil comPerdido(UUID etapaDestinoId, Instant quando,
                                   UUID motivoPerdaIdNovo, String observacaoNova) {
        return reconstituir(
                getId(), getCreatedAt(), Instant.now(), getDeletedAt(),
                leadId, etapaDestinoId, quando,
                decisor, dorPrincipal, usuariosPrevistos, prazoDecisao, orcamento,
                dataDemo, valorEstimado, planoInteresse, previsaoFechamento,
                planoFechado, valorFechado, motivoPerdaIdNovo, observacaoNova);
    }

    public UUID leadId() {
        return leadId;
    }

    public UUID etapaId() {
        return etapaId;
    }

    public Instant entrouNaEtapa() {
        return entrouNaEtapa;
    }

    public Boolean decisor() {
        return decisor;
    }

    public Boolean dorPrincipal() {
        return dorPrincipal;
    }

    public Boolean usuariosPrevistos() {
        return usuariosPrevistos;
    }

    public Boolean prazoDecisao() {
        return prazoDecisao;
    }

    public Boolean orcamento() {
        return orcamento;
    }

    public LocalDate dataDemo() {
        return dataDemo;
    }

    public ValorComercial valorEstimado() {
        return valorEstimado;
    }

    public TextoPlano planoInteresse() {
        return planoInteresse;
    }

    public LocalDate previsaoFechamento() {
        return previsaoFechamento;
    }

    public TextoPlano planoFechado() {
        return planoFechado;
    }

    public ValorComercial valorFechado() {
        return valorFechado;
    }

    public UUID motivoPerdaId() {
        return motivoPerdaId;
    }

    public String observacao() {
        return observacao;
    }
}
