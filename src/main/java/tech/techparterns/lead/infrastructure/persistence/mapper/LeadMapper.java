package tech.techparterns.lead.infrastructure.persistence.mapper;

import tech.techparterns.lead.domain.model.EmailContato;
import tech.techparterns.lead.domain.model.Lead;
import tech.techparterns.lead.domain.model.NomeContato;
import tech.techparterns.lead.domain.model.OrigemLead;
import tech.techparterns.lead.domain.model.SituacaoLead;
import tech.techparterns.lead.domain.model.TelefoneBrasil;
import tech.techparterns.lead.infrastructure.persistence.LeadEntity;
import tech.techparterns.shared.domain.CompanyId;

public final class LeadMapper {

    private LeadMapper() {}

    public static Lead toDomain(LeadEntity e) {
        return Lead.reconstituir(
                e.id, e.createdAt, e.updatedAt, e.deletedAt,
                CompanyId.reconstituir(e.companyId),
                NomeContato.reconstituir(e.nomeContato),
                e.telefone == null ? null : TelefoneBrasil.reconstituir(e.telefone),
                e.whatsapp == null ? null : TelefoneBrasil.reconstituir(e.whatsapp),
                e.email == null ? null : EmailContato.reconstituir(e.email),
                OrigemLead.valueOf(e.origem),
                e.situacao == null ? SituacaoLead.ABERTA : SituacaoLead.valueOf(e.situacao),
                e.uf, e.cidade, e.quente);
    }

    public static LeadEntity toEntity(Lead d) {
        var e = new LeadEntity();
        e.id = d.getId();
        e.createdAt = d.getCreatedAt();
        e.updatedAt = d.getUpdatedAt();
        e.deletedAt = d.getDeletedAt();
        e.companyId = d.companyId().valor();
        e.nomeContato = d.nome().valor();
        e.telefone = d.telefone() == null ? null : d.telefone().valor();
        e.whatsapp = d.whatsapp() == null ? null : d.whatsapp().valor();
        e.email = d.email() == null ? null : d.email().valor();
        e.origem = d.origem().name();
        e.situacao = d.situacao().name();
        e.uf = d.uf();
        e.cidade = d.cidade();
        e.quente = d.quente();
        return e;
    }
}
