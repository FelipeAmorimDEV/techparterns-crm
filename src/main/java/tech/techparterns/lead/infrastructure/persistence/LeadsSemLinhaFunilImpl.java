package tech.techparterns.lead.infrastructure.persistence;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import tech.techparterns.funil.domain.port.LeadsSemLinhaFunil;
import tech.techparterns.funil.domain.port.LeadsSemLinhaFunil.LeadSemLinha;

@ApplicationScoped
public class LeadsSemLinhaFunilImpl implements LeadsSemLinhaFunil {

    private final EntityManager em;

    public LeadsSemLinhaFunilImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public List<LeadSemLinha> listar() {
        return em.createQuery("""
                        select l from LeadEntity l
                        where l.deletedAt is null
                          and not exists (
                            select n.id from NegocioFunilEntity n
                            where n.leadId = l.id)
                        """, LeadEntity.class)
                .getResultStream()
                .map(l -> new LeadSemLinha(l.id, l.companyId, l.situacao))
                .toList();
    }
}
