package tech.techparterns.lead.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import tech.techparterns.lead.domain.model.Lead;
import tech.techparterns.lead.domain.repository.LeadRepository;
import tech.techparterns.lead.infrastructure.persistence.mapper.LeadMapper;
import tech.techparterns.shared.domain.CompanyId;

@ApplicationScoped
public class LeadRepositoryImpl implements LeadRepository {

    private final EntityManager em;

    public LeadRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<Lead> buscarPorId(UUID id, CompanyId companyId) {
        return em.createQuery(
                        "from LeadEntity e where e.id = :id and e.companyId = :companyId", LeadEntity.class)
                .setParameter("id", id)
                .setParameter("companyId", companyId.valor())
                .getResultStream()
                .findFirst()
                .map(LeadMapper::toDomain);
    }

    @Override
    public List<Lead> listarNaoExcluidos(CompanyId companyId) {
        return em.createQuery(
                        "from LeadEntity e where e.companyId = :companyId and e.deletedAt is null", LeadEntity.class)
                .setParameter("companyId", companyId.valor())
                .getResultList()
                .stream()
                .map(LeadMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void salvar(Lead lead) {
        em.merge(LeadMapper.toEntity(lead));
    }
}
