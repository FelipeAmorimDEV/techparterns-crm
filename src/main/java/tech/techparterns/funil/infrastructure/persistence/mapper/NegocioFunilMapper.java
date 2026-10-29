package tech.techparterns.funil.infrastructure.persistence.mapper;

import tech.techparterns.funil.domain.model.NegocioFunil;
import tech.techparterns.funil.domain.model.TextoPlano;
import tech.techparterns.funil.domain.model.ValorComercial;
import tech.techparterns.funil.infrastructure.persistence.NegocioFunilEntity;

public final class NegocioFunilMapper {

    private NegocioFunilMapper() {}

    public static NegocioFunil toDomain(NegocioFunilEntity e) {
        return NegocioFunil.reconstituir(
                e.id,
                e.createdAt,
                e.updatedAt,
                e.deletedAt,
                e.leadId,
                e.etapaId,
                e.entrouNaEtapa,
                e.decisor,
                e.dorPrincipal,
                e.usuariosPrevistos,
                e.prazoDecisao,
                e.orcamento,
                e.dataDemo,
                e.valorEstimado == null ? null : ValorComercial.reconstituir(e.valorEstimado),
                e.planoInteresse == null ? null : TextoPlano.reconstituir(e.planoInteresse),
                e.previsaoFechamento,
                e.planoFechado == null ? null : TextoPlano.reconstituir(e.planoFechado),
                e.valorFechado == null ? null : ValorComercial.reconstituir(e.valorFechado),
                e.motivoId,
                e.observacao);
    }

    public static NegocioFunilEntity toEntity(NegocioFunil d) {
        var e = new NegocioFunilEntity();
        e.id = d.getId();
        e.createdAt = d.getCreatedAt();
        e.updatedAt = d.getUpdatedAt();
        e.deletedAt = d.getDeletedAt();
        e.leadId = d.leadId();
        e.etapaId = d.etapaId();
        e.entrouNaEtapa = d.entrouNaEtapa();
        e.decisor = d.decisor();
        e.dorPrincipal = d.dorPrincipal();
        e.usuariosPrevistos = d.usuariosPrevistos();
        e.prazoDecisao = d.prazoDecisao();
        e.orcamento = d.orcamento();
        e.dataDemo = d.dataDemo();
        e.valorEstimado = d.valorEstimado() == null ? null : d.valorEstimado().valor();
        e.planoInteresse = d.planoInteresse() == null ? null : d.planoInteresse().valor();
        e.previsaoFechamento = d.previsaoFechamento();
        e.planoFechado = d.planoFechado() == null ? null : d.planoFechado().valor();
        e.valorFechado = d.valorFechado() == null ? null : d.valorFechado().valor();
        e.motivoId = d.motivoPerdaId();
        e.observacao = d.observacao();
        return e;
    }
}
