package tech.techparterns.lead.infrastructure.persistence;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

import tech.techparterns.distribuicao.domain.port.FatosLead;
import tech.techparterns.lead.domain.model.SituacaoLead;
import tech.techparterns.lead.domain.repository.LeadRepository;
import tech.techparterns.shared.domain.CompanyId;

@ApplicationScoped
public class FatosLeadImpl implements FatosLead {

    private final EntityManager em;
    private final LeadRepository leads;
    private final Clock clock;

    public FatosLeadImpl(EntityManager em, LeadRepository leads, Clock clock) {
        this.em = em;
        this.leads = leads;
        this.clock = clock;
    }

    @Override
    public Optional<Fato> buscar(String companyId, UUID leadId) {
        return leads.buscarPorId(leadId, CompanyId.reconstituir(companyId)).map(FatosLeadImpl::fato);
    }

    @Override
    public List<Fato> listarSemDono(String companyId, Filtro filtro) {
        var jpql = new StringBuilder("""
                select l from LeadEntity l
                where l.companyId = :company and l.deletedAt is null and l.situacao = 'ABERTA'
                  and not exists (
                    select o from LeadOwnershipEntity o
                    where o.leadId = l.id and o.ownerUserId is not null)
                """);
        if (filtro.uf() != null)
            jpql.append(" and upper(l.uf) = :uf");
        if (filtro.cidade() != null)
            jpql.append(" and l.cidade = :cidade");
        if (filtro.quente() != null)
            jpql.append(" and l.quente = :quente");
        if (filtro.origem() != null)
            jpql.append(" and l.origem = :origem");
        var q = em.createQuery(jpql.toString(), LeadEntity.class).setParameter("company", companyId);
        if (filtro.uf() != null)
            q.setParameter("uf", filtro.uf().toUpperCase(java.util.Locale.ROOT));
        if (filtro.cidade() != null)
            q.setParameter("cidade", filtro.cidade());
        if (filtro.quente() != null)
            q.setParameter("quente", filtro.quente());
        if (filtro.origem() != null)
            q.setParameter("origem", filtro.origem());
        return q.getResultList().stream().map(FatosLeadImpl::deEntidade).toList();
    }

    @Override
    public List<Fato> listarCarteiraAberta(String companyId, UUID donoId) {
        return em.createQuery("""
                        select l from LeadEntity l
                        where l.companyId = :company and l.deletedAt is null and l.situacao = 'ABERTA'
                          and exists (
                            select o from LeadOwnershipEntity o
                            where o.leadId = l.id and o.ownerUserId = :dono)
                        order by l.createdAt asc, l.id asc
                        """, LeadEntity.class)
                .setParameter("company", companyId)
                .setParameter("dono", donoId)
                .getResultList().stream()
                .map(FatosLeadImpl::deEntidade)
                .toList();
    }

    @Override
    public void atualizar(String companyId, UUID leadId, String situacao, boolean quente, String uf, String cidade) {
        var lead = leads.buscarPorId(leadId, CompanyId.reconstituir(companyId)).orElseThrow();
        var novo = lead.comFatos(SituacaoLead.valueOf(situacao), quente, uf, cidade, clock.instant());
        if (novo.isFailure())
            throw new tech.techparterns.shared.domain.exception.DomainException(novo.getErrors());
        leads.salvar(novo.getValue());
    }

    private static Fato fato(tech.techparterns.lead.domain.model.Lead l) {
        return new Fato(l.getId(), l.companyId().valor(), l.nome().valor(), l.situacao().name(), l.uf(), l.cidade(),
                l.quente(), l.origem().name(), l.getCreatedAt(), l.isDeleted());
    }

    private static Fato deEntidade(LeadEntity e) {
        return new Fato(e.id, e.companyId, e.nomeContato, e.situacao, e.uf, e.cidade, e.quente, e.origem, e.createdAt,
                e.deletedAt != null);
    }
}
