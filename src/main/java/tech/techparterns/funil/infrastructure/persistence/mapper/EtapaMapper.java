package tech.techparterns.funil.infrastructure.persistence.mapper;

import tech.techparterns.funil.domain.model.CorEtapa;
import tech.techparterns.funil.domain.model.Etapa;
import tech.techparterns.funil.domain.model.OrdemEtapa;
import tech.techparterns.funil.domain.model.Probabilidade;
import tech.techparterns.funil.domain.model.SlaDias;
import tech.techparterns.funil.domain.model.TipoEtapa;
import tech.techparterns.funil.infrastructure.persistence.EtapaEntity;
import tech.techparterns.shared.domain.CompanyId;

public final class EtapaMapper {

    private EtapaMapper() {}

    public static Etapa toDomain(EtapaEntity e) {
        return Etapa.reconstituir(
                e.id,
                e.createdAt,
                e.updatedAt,
                e.deletedAt,
                CompanyId.reconstituir(e.companyId),
                e.nome,
                TipoEtapa.valueOf(e.tipo),
                OrdemEtapa.reconstituir(e.ordem),
                e.ativa,
                e.probabilidade == null ? null : Probabilidade.reconstituir(e.probabilidade),
                e.cor == null ? null : CorEtapa.reconstituir(e.cor),
                SlaDias.reconstituir(e.slaDias),
                e.exigeQualificacao,
                e.exigeDemo);
    }

    public static EtapaEntity toEntity(Etapa d) {
        var e = new EtapaEntity();
        e.id = d.getId();
        e.createdAt = d.getCreatedAt();
        e.updatedAt = d.getUpdatedAt();
        e.deletedAt = d.getDeletedAt();
        e.companyId = d.companyId().valor();
        e.nome = d.nome();
        e.tipo = d.tipo().name();
        e.ordem = d.ordem().valor();
        e.ativa = d.ativa();
        e.probabilidade = d.probabilidade() == null ? null : d.probabilidade().valor();
        e.cor = d.cor() == null ? null : d.cor().valor();
        e.slaDias = d.sla() == null ? null : d.sla().valor();
        e.exigeQualificacao = d.exigeQualificacao();
        e.exigeDemo = d.exigeDemo();
        return e;
    }
}
