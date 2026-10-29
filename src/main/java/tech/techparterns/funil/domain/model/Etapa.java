package tech.techparterns.funil.domain.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import tech.techparterns.shared.domain.CompanyId;
import tech.techparterns.shared.domain.Entity;
import tech.techparterns.shared.domain.Result;

public class Etapa extends Entity {

    public static final String NOME_OBRIGATORIO = "Nome da etapa é obrigatório";
    public static final String TIPO_OBRIGATORIO = "Tipo da etapa é obrigatório";

    private final CompanyId companyId;
    private String nome;
    private final TipoEtapa tipo;
    private OrdemEtapa ordem;
    private boolean ativa;
    private Probabilidade probabilidade;
    private CorEtapa cor;
    private SlaDias sla;
    private final boolean exigeQualificacao;
    private final boolean exigeDemo;

    private Etapa(UUID id, CompanyId companyId, String nome, TipoEtapa tipo, OrdemEtapa ordem, boolean ativa,
                  Probabilidade probabilidade, CorEtapa cor, SlaDias sla,
                  boolean exigeQualificacao, boolean exigeDemo) {
        super(id);
        this.companyId = companyId;
        this.nome = nome;
        this.tipo = tipo;
        this.ordem = ordem;
        this.ativa = ativa;
        this.probabilidade = probabilidade;
        this.cor = cor;
        this.sla = sla;
        this.exigeQualificacao = exigeQualificacao;
        this.exigeDemo = exigeDemo;
    }

    private Etapa(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                  CompanyId companyId, String nome, TipoEtapa tipo, OrdemEtapa ordem, boolean ativa,
                  Probabilidade probabilidade, CorEtapa cor, SlaDias sla,
                  boolean exigeQualificacao, boolean exigeDemo) {
        super(id, createdAt, updatedAt, deletedAt);
        this.companyId = companyId;
        this.nome = nome;
        this.tipo = tipo;
        this.ordem = ordem;
        this.ativa = ativa;
        this.probabilidade = probabilidade;
        this.cor = cor;
        this.sla = sla;
        this.exigeQualificacao = exigeQualificacao;
        this.exigeDemo = exigeDemo;
    }

    public static Result<Etapa> tryCreate(String companyId, String nome, TipoEtapa tipo, Integer ordem,
                                          boolean ativa, Integer probabilidade, String cor, Integer slaDias,
                                          boolean exigeQualificacao, boolean exigeDemo) {
        var companyIdResult = CompanyId.tryCreate(companyId);
        var nomeResult = nome(nome);
        var tipoResult = tipo == null ? Result.<TipoEtapa>fail(TIPO_OBRIGATORIO) : Result.ok(tipo);
        var ordemResult = OrdemEtapa.tryCreate(ordem);
        var probabilidadeResult = probabilidade == null
                ? Result.<Probabilidade>ok(null)
                : Probabilidade.tryCreate(probabilidade);
        var corResult = cor == null ? Result.<CorEtapa>ok(null) : CorEtapa.tryCreate(cor);
        var slaResult = SlaDias.tryCreate(slaDias);
        var validacao = Result.combineAll(companyIdResult, nomeResult, tipoResult, ordemResult,
                probabilidadeResult, corResult, slaResult);
        if (validacao.isFailure())
            return Result.propagate(validacao);
        return Result.ok(new Etapa(
                UUID.randomUUID(),
                companyIdResult.getValue(),
                nomeResult.getValue(),
                tipoResult.getValue(),
                ordemResult.getValue(),
                ativa,
                probabilidadeResult.getValue(),
                corResult.getValue(),
                slaResult.getValue(),
                exigeQualificacao,
                exigeDemo));
    }

    public static Etapa reconstituir(UUID id, Instant createdAt, Instant updatedAt, OffsetDateTime deletedAt,
                                     CompanyId companyId, String nome, TipoEtapa tipo, OrdemEtapa ordem,
                                     boolean ativa, Probabilidade probabilidade, CorEtapa cor, SlaDias sla,
                                     boolean exigeQualificacao, boolean exigeDemo) {
        return new Etapa(id, createdAt, updatedAt, deletedAt, companyId, nome, tipo, ordem, ativa,
                probabilidade, cor, sla, exigeQualificacao, exigeDemo);
    }

    /** Atributos já fixados pelo domínio. Não revalida nome, tipo, ordem, probabilidade, cor nem SLA. */
    public static Etapa nascida(CompanyId companyId, TipoEtapa tipo, OrdemEtapa ordem) {
        SlaDias slaVazio = SlaDias.reconstituir(null);
        return switch (tipo) {
            case ABERTO -> new Etapa(UUID.randomUUID(), companyId, "Aberto", TipoEtapa.ABERTO, ordem,
                    true, Probabilidade.reconstituir(0), null, slaVazio, false, false);
            case GANHO -> new Etapa(UUID.randomUUID(), companyId, "Ganho", TipoEtapa.GANHO, ordem,
                    true, null, null, slaVazio, false, false);
            case PERDIDO -> new Etapa(UUID.randomUUID(), companyId, "Perdido", TipoEtapa.PERDIDO, ordem,
                    true, null, null, slaVazio, false, false);
        };
    }

    /** Etapa enviada pelo admin. Os objetos já foram aceitos; não revalida. */
    public static Etapa definida(CompanyId companyId, String nome, TipoEtapa tipo, OrdemEtapa ordem,
                                 boolean ativa, Probabilidade probabilidade, CorEtapa cor, SlaDias sla,
                                 boolean exigeQualificacao, boolean exigeDemo) {
        return new Etapa(UUID.randomUUID(), companyId, nome, tipo, ordem, ativa, probabilidade, cor, sla,
                exigeQualificacao, exigeDemo);
    }

    private static Result<String> nome(String nome) {
        if (nome == null || nome.isBlank())
            return Result.fail(NOME_OBRIGATORIO);
        return Result.ok(nome.trim());
    }

    /** Nome, ordem, probabilidade, cor e SLA. Tipo, ativo e as flags permanecem. */
    public Result<Void> alterar(String nome, Integer ordem, Integer probabilidade, String cor, Integer slaDias) {
        var nomeResult = nome(nome);
        var ordemResult = OrdemEtapa.tryCreate(ordem);
        var probabilidadeResult = probabilidade == null
                ? Result.<Probabilidade>ok(null)
                : Probabilidade.tryCreate(probabilidade);
        var corResult = cor == null ? Result.<CorEtapa>ok(null) : CorEtapa.tryCreate(cor);
        var slaResult = SlaDias.tryCreate(slaDias);
        var validacao = Result.combineAll(nomeResult, ordemResult, probabilidadeResult, corResult, slaResult);
        if (validacao.isFailure())
            return Result.propagate(validacao);
        this.nome = nomeResult.getValue();
        this.ordem = ordemResult.getValue();
        this.probabilidade = probabilidadeResult.getValue();
        this.cor = corResult.getValue();
        this.sla = slaResult.getValue();
        return Result.ok();
    }

    public void desativar() {
        this.ativa = false;
    }

    public CompanyId companyId() {
        return companyId;
    }

    public String nome() {
        return nome;
    }

    public TipoEtapa tipo() {
        return tipo;
    }

    public OrdemEtapa ordem() {
        return ordem;
    }

    public boolean ativa() {
        return ativa;
    }

    public Probabilidade probabilidade() {
        return probabilidade;
    }

    public CorEtapa cor() {
        return cor;
    }

    public SlaDias sla() {
        return sla;
    }

    public boolean exigeQualificacao() {
        return exigeQualificacao;
    }

    public boolean exigeDemo() {
        return exigeDemo;
    }
}
