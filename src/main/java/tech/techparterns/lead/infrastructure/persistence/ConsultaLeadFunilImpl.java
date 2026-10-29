package tech.techparterns.lead.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import tech.techparterns.distribuicao.infrastructure.persistence.LeadOwnershipEntity;
import tech.techparterns.funil.domain.port.ConsultaLeadFunil;
import tech.techparterns.lead.domain.repository.LeadRepository;
import tech.techparterns.shared.domain.CompanyId;

@ApplicationScoped
public class ConsultaLeadFunilImpl implements ConsultaLeadFunil {

    private final EntityManager em;
    private final LeadRepository leads;

    public ConsultaLeadFunilImpl(EntityManager em, LeadRepository leads) {
        this.em = em;
        this.leads = leads;
    }

    @Override
    @Transactional
    public Optional<Fato> buscar(CompanyId companyId, UUID leadId) {
        return leads.buscarPorId(leadId, companyId)
                .map(lead -> new Fato(
                        lead.situacao().name(),
                        lead.uf(),
                        lead.cidade(),
                        lead.quente(),
                        lead.isDeleted(),
                        dono(companyId, leadId)));
    }

    private UUID dono(CompanyId companyId, UUID leadId) {
        return em.createQuery("""
                        from LeadOwnershipEntity o
                        where o.leadId = :leadId and o.companyId = :companyId
                        """, LeadOwnershipEntity.class)
                .setParameter("leadId", leadId)
                .setParameter("companyId", companyId.valor())
                .getResultStream()
                .findFirst()
                .map(o -> o.ownerUserId)
                .orElse(null);
    }
}
