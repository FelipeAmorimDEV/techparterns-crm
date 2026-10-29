package tech.techparterns.funil.infrastructure.persistence.mapper;

import tech.techparterns.funil.domain.model.HistoricoEtapa;
import tech.techparterns.funil.infrastructure.persistence.HistoricoEtapaEntity;

public final class HistoricoEtapaMapper {

    private HistoricoEtapaMapper() {}

    public static HistoricoEtapa toDomain(HistoricoEtapaEntity e) {
        return HistoricoEtapa.reconstituir(
                e.id,
                e.createdAt,
                e.updatedAt,
                e.deletedAt,
                e.origemEtapaId,
                e.destinoEtapaId,
                e.userId,
                e.instante);
    }

    public static HistoricoEtapaEntity toEntity(HistoricoEtapa d) {
        var e = new HistoricoEtapaEntity();
        e.id = d.getId();
        e.createdAt = d.getCreatedAt();
        e.updatedAt = d.getUpdatedAt();
        e.deletedAt = d.getDeletedAt();
        e.origemEtapaId = d.origemEtapaId();
        e.destinoEtapaId = d.destinoEtapaId();
        e.userId = d.userId();
        e.instante = d.quando();
        return e;
    }
}
