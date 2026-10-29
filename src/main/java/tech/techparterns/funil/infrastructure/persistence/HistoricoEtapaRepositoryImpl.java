package tech.techparterns.funil.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import tech.techparterns.funil.domain.model.HistoricoEtapa;
import tech.techparterns.funil.domain.repository.HistoricoEtapaRepository;
import tech.techparterns.funil.infrastructure.persistence.mapper.HistoricoEtapaMapper;
import tech.techparterns.shared.domain.CompanyId;

@ApplicationScoped
public class HistoricoEtapaRepositoryImpl implements HistoricoEtapaRepository {

    private final EntityManager em;

    public HistoricoEtapaRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public Optional<HistoricoEtapa> buscarPorId(UUID id, CompanyId companyId) {
        return em.createQuery("""
                        select h from HistoricoEtapaEntity h
                        where h.id = :id
                          and exists (
                            select e.id from EtapaEntity e
                            where e.id = h.destinoEtapaId and e.companyId = :companyId)
                        """, HistoricoEtapaEntity.class)
                .setParameter("id", id)
                .setParameter("companyId", companyId.valor())
                .getResultStream()
                .findFirst()
                .map(HistoricoEtapaMapper::toDomain);
    }

    @Override
    @Transactional
    public void salvar(HistoricoEtapa historico) {
        em.merge(HistoricoEtapaMapper.toEntity(historico));
    }
}
