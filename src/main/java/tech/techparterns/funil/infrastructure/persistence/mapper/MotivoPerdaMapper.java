package tech.techparterns.funil.infrastructure.persistence.mapper;

import tech.techparterns.funil.domain.model.MotivoPerda;
import tech.techparterns.funil.infrastructure.persistence.MotivoPerdaEntity;
import tech.techparterns.shared.domain.CompanyId;

public final class MotivoPerdaMapper {

    private MotivoPerdaMapper() {}

    public static MotivoPerda toDomain(MotivoPerdaEntity e) {
        return MotivoPerda.reconstituir(
                e.id,
                e.createdAt,
                e.updatedAt,
                e.deletedAt,
                CompanyId.reconstituir(e.companyId),
                e.nome,
                e.ativo);
    }

    public static MotivoPerdaEntity toEntity(MotivoPerda d) {
        var e = new MotivoPerdaEntity();
        e.id = d.getId();
        e.createdAt = d.getCreatedAt();
        e.updatedAt = d.getUpdatedAt();
        e.deletedAt = d.getDeletedAt();
        e.companyId = d.companyId().valor();
        e.nome = d.nome();
        e.ativo = d.ativo();
        return e;
    }
}
