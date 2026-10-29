package tech.techparterns.funil.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import tech.techparterns.funil.domain.model.NegocioFunil;
import tech.techparterns.funil.domain.repository.NegocioFunilRepository;
import tech.techparterns.funil.infrastructure.persistence.mapper.NegocioFunilMapper;
import tech.techparterns.shared.domain.CompanyId;

@ApplicationScoped
public class NegocioFunilRepositoryImpl implements NegocioFunilRepository {

    private final EntityManager em;

    public NegocioFunilRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public Optional<NegocioFunil> buscar(UUID leadId, CompanyId companyId) {
        return em.createQuery("""
                        select n from NegocioFunilEntity n
                        where n.leadId = :leadId
                          and exists (
                            select e.id from EtapaEntity e
                            where e.id = n.etapaId and e.companyId = :companyId)
                          and exists (
                            select l.id from LeadEntity l
                            where l.id = n.leadId and l.companyId = :companyId)
                        """, NegocioFunilEntity.class)
                .setParameter("leadId", leadId)
                .setParameter("companyId", companyId.valor())
                .getResultStream()
                .findFirst()
                .map(NegocioFunilMapper::toDomain);
    }

    @Override
    @Transactional
    public List<NegocioFunil> listarPorEmpresa(CompanyId companyId) {
        return em.createQuery("""
                        select n from NegocioFunilEntity n
                        where exists (
                            select e.id from EtapaEntity e
                            where e.id = n.etapaId and e.companyId = :companyId)
                          and exists (
                            select l.id from LeadEntity l
                            where l.id = n.leadId and l.companyId = :companyId)
                        """, NegocioFunilEntity.class)
                .setParameter("companyId", companyId.valor())
                .getResultStream()
                .map(NegocioFunilMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public boolean existeNaEtapa(UUID etapaId) {
        return !em.createQuery(
                        "select n.id from NegocioFunilEntity n where n.etapaId = :etapaId", UUID.class)
                .setParameter("etapaId", etapaId)
                .setMaxResults(1)
                .getResultList()
                .isEmpty();
    }

    @Override
    @Transactional
    public void salvar(NegocioFunil negocio) {
        em.merge(NegocioFunilMapper.toEntity(negocio));
    }
}
